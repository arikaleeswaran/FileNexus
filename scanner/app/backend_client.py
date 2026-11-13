import os
import logging

import httpx

from app.config import settings


logger = logging.getLogger(__name__)

SCANNER_TOKEN = os.getenv("SCANNER_TOKEN", "")

SCANNER_HEADERS = {
    "X-Scanner-Token": SCANNER_TOKEN
}


class BackendClient:

    

    def __init__(self):
        self.base_url = settings.SPRING_BOOT_BASE_URL.rstrip("/")
        self.timeout = 15.0

    def _extract_data(self, response):
        response.raise_for_status()

        payload = response.json()

        if not isinstance(payload, dict):
            raise ValueError("Unexpected response from Spring Boot")

        if payload.get("success") is False:
            raise RuntimeError(
                payload.get("message", "Spring Boot request failed")
            )

        return payload.get("data", payload)

    def get_scan_job(self, scan_id: int):
        response = httpx.get(
            f"{self.base_url}/api/scan-jobs/{scan_id}",
            headers=SCANNER_HEADERS,
            timeout=self.timeout
        )

        return self._extract_data(response)

    def start_scan(self, scan_id: int):
        response = httpx.patch(
            f"{self.base_url}/api/scan-jobs/{scan_id}/start",
            headers=SCANNER_HEADERS,
            timeout=self.timeout
        )

        return self._extract_data(response)

    def complete_scan(self, scan_id: int, scan_result: str):
        response = httpx.patch(
            f"{self.base_url}/api/scan-jobs/{scan_id}/complete",
            headers=SCANNER_HEADERS,
            json={
                "scanResult": scan_result,
                "scannerVersion": settings.SCANNER_VERSION
            },
            timeout=self.timeout
        )

        return self._extract_data(response)

    def fail_scan(self, scan_id: int, reason: str):
        response = httpx.patch(
            f"{self.base_url}/api/scan-jobs/{scan_id}/fail",
            headers=SCANNER_HEADERS,
            json={
                "failureReason": reason[:2000]
            },
            timeout=self.timeout
        )

        return self._extract_data(response)

    def timeout_scan(self, scan_id: int, reason: str):
        response = httpx.patch(
            f"{self.base_url}/api/scan-jobs/{scan_id}/timeout",
            headers=SCANNER_HEADERS,
            json={
                "failureReason": reason[:2000]
            },
            timeout=self.timeout
        )

        return self._extract_data(response)

    def release_clean_file(self, scan_id: int):
        response = httpx.post(
            f"{self.base_url}/api/internal/file-release/{scan_id}",
            headers=SCANNER_HEADERS,
            timeout=self.timeout
        )

        return self._extract_data(response)


backend_client = BackendClient()
