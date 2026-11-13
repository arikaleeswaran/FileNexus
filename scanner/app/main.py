
import logging
import threading

from contextlib import asynccontextmanager
from fastapi import FastAPI

from app.config import settings
from app.sqs_consumer import sqs_scan_consumer
from app.scanner_engine import check_clamav_connection


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s"
)

logger = logging.getLogger(__name__)

stop_event = threading.Event()


@asynccontextmanager
async def lifespan(app: FastAPI):

    if not settings.SQS_SCAN_QUEUE_URL:
        raise RuntimeError(
            "SQS_SCAN_QUEUE_URL is not configured"
        )

    if not settings.S3_QUARANTINE_BUCKET:
        raise RuntimeError(
            "S3_QUARANTINE_BUCKET is not configured"
        )

    if not check_clamav_connection():
        raise RuntimeError(
            "ClamAV is unavailable. Start the ClamAV container first."
        )

    logger.info("ClamAV connection successful")

    stop_event.clear()

    consumer_thread = threading.Thread(
        target=sqs_scan_consumer.poll_forever,
        args=(stop_event,),
        daemon=True
    )

    consumer_thread.start()

    logger.info("FileNexus scanner consumer started")

    yield

    logger.info("Stopping scanner consumer")

    stop_event.set()

    consumer_thread.join(timeout=25)


app = FastAPI(
    title="FileNexus Malware Scanner",
    description="S3, SQS and ClamAV-based file scanning service",
    version="2.0.0",
    lifespan=lifespan
)


@app.get("/health")
def health_check():

    clamav_available = check_clamav_connection()

    return {
        "success": True,
        "service": "FileNexus Malware Scanner",
        "status": "RUNNING" if clamav_available else "DEGRADED",
        "antivirus": {
            "engine": "ClamAV",
            "status": (
                "CONNECTED"
                if clamav_available
                else "UNAVAILABLE"
            )
        }
    }
