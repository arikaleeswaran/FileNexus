
import os

from dotenv import load_dotenv

load_dotenv()


class Settings:

    AWS_REGION = os.getenv("AWS_REGION", "ap-south-1")

    SQS_SCAN_QUEUE_URL = os.getenv("SQS_SCAN_QUEUE_URL")

    S3_QUARANTINE_BUCKET = os.getenv("S3_QUARANTINE_BUCKET")

    SPRING_BOOT_BASE_URL = os.getenv(
        "SPRING_BOOT_BASE_URL",
        "http://localhost:8080"
    )

    SCANNER_VERSION = "filenexus-clamav-scanner-2.0.0"

    SQS_WAIT_TIME_SECONDS = 20

    SQS_VISIBILITY_TIMEOUT = 120

    CLAMD_HOST = os.getenv("CLAMD_HOST", "127.0.0.1")

    CLAMD_PORT = int(os.getenv("CLAMD_PORT", "3310"))

    CLAMD_TIMEOUT_SECONDS = int(
        os.getenv("CLAMD_TIMEOUT_SECONDS", "120")
    )

    CLAMD_CHUNK_SIZE = int(
        os.getenv("CLAMD_CHUNK_SIZE", "65536")
    )


settings = Settings()
