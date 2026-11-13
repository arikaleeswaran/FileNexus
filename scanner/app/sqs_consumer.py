
import json
import logging
import os
import tempfile

import boto3
from botocore.exceptions import ClientError

from app.config import settings
from app.s3_service import s3_storage_service
from app.scanner_engine import scan_file
from app.backend_client import backend_client


logger = logging.getLogger(__name__)


class SQSScanConsumer:

    def __init__(self):
        self.sqs_client = boto3.client(
            "sqs",
            region_name=settings.AWS_REGION
        )

    def _get_scan_state(self, scan_id: int):
        scan_job = backend_client.get_scan_job(scan_id)

        scan_status = scan_job.get("scanStatus")
        scan_result = scan_job.get("scanResult")

        if not scan_status:
            raise ValueError(
                f"Missing scanStatus for scan ID {scan_id}"
            )

        return scan_status, scan_result

    def _release_completed_clean_file(self, file_id: int, scan_id: int):
        logger.info(
            "Retrying clean-bucket release: fileId=%s scanId=%s",
            file_id,
            scan_id
        )

        # This operation is idempotent in the Spring Boot service.
        backend_client.release_clean_file(scan_id)

        logger.info(
            "File released successfully: fileId=%s scanId=%s",
            file_id,
            scan_id
        )

    def process_message(self, message):
        body = json.loads(message["Body"])

        file_id = int(body["fileId"])
        scan_id = int(body["scanId"])
        object_key = body["quarantineKey"]
        expected_checksum = body["sha256Checksum"]
        bucket = body["quarantineBucket"]

        if bucket != settings.S3_QUARANTINE_BUCKET:
            raise ValueError(
                "Unexpected quarantine bucket in SQS message"
            )

        if not object_key or not expected_checksum:
            raise ValueError(
                "Missing quarantine key or SHA-256 checksum"
            )

        logger.info(
            "Processing scan message: fileId=%s scanId=%s",
            file_id,
            scan_id
        )

        # STEP 1: Inspect the existing backend state first.
        scan_status, scan_result = self._get_scan_state(scan_id)

        logger.info(
            "Existing scan state: scanId=%s status=%s result=%s",
            scan_id,
            scan_status,
            scan_result
        )

        # STEP 2: The scan already completed successfully.
        # Retry only the release. Never scan the file again.
        if scan_status == "COMPLETED":

            if scan_result == "CLEAN":
                self._release_completed_clean_file(file_id, scan_id)
                return True

            if scan_result == "REJECTED":
                logger.info(
                    "File was already rejected. No release required: "
                    "fileId=%s scanId=%s",
                    file_id,
                    scan_id
                )
                return True

            raise ValueError(
                f"Unexpected result for completed scan {scan_id}: "
                f"{scan_result}"
            )

        # STEP 3: Terminal operational failure.
        # A new scan requires a new scan job; this job cannot be restarted.
        if scan_status in ("FAILED", "TIMEOUT"):
            logger.error(
                "Scan job is already in terminal state %s: scanId=%s",
                scan_status,
                scan_id
            )
            return True

        # STEP 4: Another worker may already be processing this job.
        # Do not start a second scan.
        if scan_status == "RUNNING":
            logger.warning(
                "Scan is already running. Deferring this message: "
                "fileId=%s scanId=%s",
                file_id,
                scan_id
            )
            return False

        if scan_status != "QUEUED":
            raise ValueError(
                f"Unsupported scan status for scan {scan_id}: "
                f"{scan_status}"
            )

        temporary_path = None
        scan_started = False
        scan_completed = False

        try:
            # STEP 5: Start the queued scan.
            backend_client.start_scan(scan_id)
            scan_started = True

            # STEP 6: Download the quarantined file.
            with tempfile.NamedTemporaryFile(delete=False) as temp_file:
                temporary_path = temp_file.name

            s3_storage_service.download_file(
                object_key,
                temporary_path
            )

            # STEP 7: Verify checksum and run the simulated scanner.
            result = scan_file(
                temporary_path,
                expected_checksum
            )

            scan_result = result["scan_result"]

            if scan_result not in ("CLEAN", "REJECTED"):
                raise ValueError(
                    f"Invalid scanner result: {scan_result}"
                )

            # STEP 8: Report the result to Spring Boot.
            backend_client.complete_scan(
                scan_id,
                scan_result
            )

            scan_completed = True

            logger.info(
                "Scan completed: fileId=%s scanId=%s result=%s",
                file_id,
                scan_id,
                scan_result
            )

            # STEP 9: Release only CLEAN files.
            if scan_result == "CLEAN":
                self._release_completed_clean_file(
                    file_id,
                    scan_id
                )

            # REJECTED files are not copied to clean storage.
            return True

        except Exception as exception:

            logger.exception(
                "Scan processing failed: fileId=%s scanId=%s",
                file_id,
                scan_id
            )

            # A completed CLEAN scan must not be changed to FAILED
            # just because its S3 release operation failed.
            if scan_completed:
                raise

            # Report operational scan failure only if scanning started.
            if scan_started:
                backend_client.fail_scan(
                    scan_id,
                    str(exception)
                )

                logger.error(
                    "Operational failure recorded in backend: "
                    "fileId=%s scanId=%s",
                    file_id,
                    scan_id
                )

                # The backend has recorded this job as FAILED.
                # A new attempt requires a new scan job.
                return True

            # If starting the scan or contacting the backend failed,
            # keep the SQS message for retry.
            raise

        finally:
            if temporary_path and os.path.exists(temporary_path):
                try:
                    os.remove(temporary_path)
                except OSError:
                    logger.warning(
                        "Could not remove temporary file: %s",
                        temporary_path
                    )

    def poll_forever(self, stop_event):

        logger.info("SQS scanner consumer started")

        while not stop_event.is_set():

            try:
                response = self.sqs_client.receive_message(
                    QueueUrl=settings.SQS_SCAN_QUEUE_URL,
                    MaxNumberOfMessages=1,
                    WaitTimeSeconds=settings.SQS_WAIT_TIME_SECONDS,
                    VisibilityTimeout=settings.SQS_VISIBILITY_TIMEOUT
                )

                messages = response.get("Messages", [])

                for message in messages:

                    try:
                        should_acknowledge = self.process_message(message)

                        if should_acknowledge:
                            self.sqs_client.delete_message(
                                QueueUrl=settings.SQS_SCAN_QUEUE_URL,
                                ReceiptHandle=message["ReceiptHandle"]
                            )

                            logger.info("SQS message acknowledged")

                        else:
                            # Do not delete a message for a scan that
                            # is already RUNNING.
                            self.sqs_client.change_message_visibility(
                                QueueUrl=settings.SQS_SCAN_QUEUE_URL,
                                ReceiptHandle=message["ReceiptHandle"],
                                VisibilityTimeout=30
                            )

                            logger.info(
                                "SQS message deferred because scan is "
                                "already running"
                            )

                    except Exception:
                        # Keep the message for retry.
                        # SQS redrive policy handles repeated failures.
                        logger.exception(
                            "Message was not acknowledged; it will be retried"
                        )

            except ClientError:
                logger.exception("AWS SQS operation failed")
                stop_event.wait(5)

            except Exception:
                logger.exception("Unexpected consumer error")
                stop_event.wait(5)


sqs_scan_consumer = SQSScanConsumer()
