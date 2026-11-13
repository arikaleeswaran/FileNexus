
import boto3
from botocore.exceptions import ClientError

from app.config import settings


class S3StorageService:

    def __init__(self):
        self.s3_client = boto3.client(
            "s3",
            region_name=settings.AWS_REGION
        )

    def download_file(self, object_key: str, destination: str):
        try:
            self.s3_client.download_file(
                settings.S3_QUARANTINE_BUCKET,
                object_key,
                destination
            )

            return destination

        except ClientError as exception:
            raise RuntimeError(
                f"Unable to download file from S3: {exception}"
            ) from exception


s3_storage_service = S3StorageService()
