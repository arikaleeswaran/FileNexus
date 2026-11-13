# FileNexus Frontend

React + Vite + Tailwind CSS frontend for the FileNexus Spring Boot backend.

## Requirements
- Node.js 20.19+ or 22.12+
- npm
- Spring Boot backend running at http://localhost:8080

## Run
1. Extract the ZIP and open a terminal in `filenexus-frontend`.
2. Run `npm install`.
3. Start the Spring Boot backend on port 8080.
4. Run `npm run dev`.
5. Open http://localhost:5173.

Vite proxies `/api` requests to `http://localhost:8080`; no local CORS change is normally needed.

## Build
`npm run build`
`npm run preview`

## Backend endpoints
- POST `/api/files/metadata`
- POST `/api/files/{fileId}/upload` (multipart field `file`)
- GET `/api/files`
- GET `/api/files/{fileId}`
- GET `/api/files/{fileId}/download`
- GET `/api/files/{fileId}/validation-history`
- GET `/api/scan-jobs/files/{fileId}`
- GET `/api/audit-logs/files/{fileId}`

If your actual response wrapper or paths differ, update `src/services/fileApi.js`.

## Upload flow
The user selects a file. The frontend reads its name, size and MIME type, creates metadata automatically, takes `fileId` from the response, then uploads the file as multipart field `file`. The user does not type metadata manually.

Authentication/JWT is not included because it is deferred in the current backend milestone. Keep the application private until authentication and authorization are implemented. Frontend validation is for usability; backend validation and scanning remain authoritative. Dashboard counts are based on the records loaded from the list endpoint (up to 100 by default).
