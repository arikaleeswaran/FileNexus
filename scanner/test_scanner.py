
from app.scanner_engine import scan_file, TEST_MALWARE_MARKER
import hashlib


def create_test_file(content, filename):
    with open(filename, "wb") as file:
        file.write(content)

    return hashlib.sha256(content).hexdigest()


# Test 1: Clean file
clean_content = b"This is a safe FileNexus test document."

clean_checksum = create_test_file(
    clean_content,
    "clean_test.txt"
)

clean_result = scan_file(
    "clean_test.txt",
    clean_checksum
)

print("Clean file result:", clean_result)


# Test 2: Simulated malware marker
malicious_content = (
    b"This is a test document. "
    + TEST_MALWARE_MARKER
)

malicious_checksum = create_test_file(
    malicious_content,
    "malware_test.txt"
)

malicious_result = scan_file(
    "malware_test.txt",
    malicious_checksum
)

print("Malware simulation result:", malicious_result)
