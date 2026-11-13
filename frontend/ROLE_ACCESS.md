# FileNexus role access

## USER
- Own documents only (backend ownership check required).
- Upload files.
- View own file metadata and validation history.
- See own scan status at a safe, user-facing level.
- Download only own APPROVED files.
- No audit log, cross-user file, scanner diagnostics, or administration access.

## SECURITY_ANALYST
- Read documents across users for investigation.
- View scan history, validation results, audit and access events.
- Monitor rejected files, timeouts, and scan failures.
- No user-role assignment or system configuration by default.

## ADMIN
- All document and security visibility.
- User administration, role assignment, account activation, system and retention settings.
- All administrative APIs must enforce ADMIN on the backend.

## Backend endpoint note
This frontend package uses the existing routes already present in the project for files, validation history, scan history, and per-file audit history. It does not invent user-management endpoints. The Administration page clearly indicates that user management needs secured backend APIs before those controls can be enabled.

Frontend route protection is for navigation/UX only. Every sensitive operation must also be authorized by Spring Security and service-level ownership checks.
