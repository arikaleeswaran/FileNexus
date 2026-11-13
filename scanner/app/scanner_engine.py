
import hashlib
import logging
import socket
import struct

from pathlib import Path

from app.config import settings


logger = logging.getLogger(__name__)


def calculate_sha256(file_path: str) -> str:

    sha256 = hashlib.sha256()

    with open(file_path, "rb") as file:

        for chunk in iter(lambda: file.read(8192), b""):
            sha256.update(chunk)

    return sha256.hexdigest()


def check_clamav_connection() -> bool:

    try:
        with socket.create_connection(
            (settings.CLAMD_HOST, settings.CLAMD_PORT),
            timeout=5
        ) as connection:

            connection.sendall(b"zPING\0")

            response = connection.recv(1024)

            return response.strip(b"\0") == b"PONG"

    except OSError:
        return False


def scan_with_clamav(file_path: str) -> str:

    path = Path(file_path)

    if not path.is_file():
        raise FileNotFoundError(
            "File does not exist for antivirus scanning"
        )

    logger.info(
        "Connecting to ClamAV at %s:%s",
        settings.CLAMD_HOST,
        settings.CLAMD_PORT
    )

    try:

        with socket.create_connection(
            (settings.CLAMD_HOST, settings.CLAMD_PORT),
            timeout=settings.CLAMD_TIMEOUT_SECONDS
        ) as connection:

            connection.settimeout(settings.CLAMD_TIMEOUT_SECONDS)

            # Start ClamAV INSTREAM protocol.
            connection.sendall(b"zINSTREAM\0")

            # Stream the file in chunks.
            with open(path, "rb") as file:

                while True:

                    chunk = file.read(settings.CLAMD_CHUNK_SIZE)

                    if not chunk:
                        break

                    # ClamAV expects:
                    # 4-byte big-endian chunk length + chunk data.
                    connection.sendall(
                        struct.pack(">I", len(chunk))
                    )

                    connection.sendall(chunk)

            # A zero-length chunk marks the end of the stream.
            connection.sendall(struct.pack(">I", 0))

            # Read the complete response.
            response = bytearray()

            while True:

                data = connection.recv(4096)

                if not data:
                    break

                response.extend(data)

                if b"\0" in data:
                    break

            result = response.decode(
                "utf-8",
                errors="replace"
            ).strip("\0").strip()

            if not result:
                raise RuntimeError(
                    "ClamAV returned an empty response"
                )

            logger.info("ClamAV response: %s", result)

            if result.endswith("OK"):
                return "CLEAN"

            if result.endswith("FOUND"):
                return "REJECTED"

            if result.endswith("ERROR"):
                raise RuntimeError(
                    f"ClamAV scanning error: {result}"
                )

            raise RuntimeError(
                f"Unexpected ClamAV response: {result}"
            )

    except socket.timeout as exception:

        raise RuntimeError(
            "ClamAV scanning timed out"
        ) from exception

    except OSError as exception:

        raise RuntimeError(
            f"Unable to communicate with ClamAV: {exception}"
        ) from exception


def scan_file(file_path: str, expected_checksum: str):

    path = Path(file_path)

    if not path.is_file():
        raise FileNotFoundError(
            "Downloaded file does not exist"
        )

    if not expected_checksum:
        raise ValueError(
            "Expected SHA-256 checksum is missing"
        )

    logger.info(
        "Starting file integrity verification: %s",
        path.name
    )

    # STEP 1: Calculate actual checksum.
    actual_checksum = calculate_sha256(file_path)

    # STEP 2: Verify file integrity.
    if actual_checksum.lower() != expected_checksum.lower():

        raise ValueError(
            "SHA-256 checksum mismatch. "
            "File integrity verification failed."
        )

    logger.info(
        "SHA-256 verification successful: %s",
        actual_checksum
    )

    # STEP 3: Run actual antivirus scanning.
    scan_result = scan_with_clamav(file_path)

    if scan_result == "REJECTED":

        return {
            "scan_result": "REJECTED",
            "reason": "ClamAV detected a malware signature",
            "sha256": actual_checksum
        }

    if scan_result == "CLEAN":

        return {
            "scan_result": "CLEAN",
            "reason": "ClamAV did not detect a known threat",
            "sha256": actual_checksum
        }

    # Fail closed: never assume CLEAN.
    raise RuntimeError(
        "Antivirus scan did not produce a valid result"
    )
