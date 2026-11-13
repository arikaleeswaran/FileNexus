# FileNexus Phase 23 — GitHub Actions CI

This patch assumes the repository root contains:

- `backend/filenexus/pom.xml`
- `frontend/package.json` and `frontend/package-lock.json`
- `scanner/requirements.txt` and `scanner/app/`

It adds:
- `.gitignore` at repository root
- `.github/workflows/ci.yml`

## What the workflow checks

1. Java 21: compiles/packages the Spring Boot application.
2. Node 22: installs exact locked dependencies with `npm ci` and builds React.
3. Python 3.12: installs scanner requirements and syntax-compiles `scanner/app`.

Backend tests are deliberately skipped in this first CI pass because the existing test suite includes Spring context / PostgreSQL / real SQS integration tests. Those need a test profile and mocked AWS clients before they can safely run on GitHub-hosted runners.

The scanner's existing `test_scanner.py` starts real scanning and expects scanner/ClamAV configuration, so it is not invoked by this CI workflow.

## Before the first push

Do not push the current local configuration until these are checked:

- Remove actual credentials and tokens from every `.env` file.
- Keep `.env.example` files with placeholder values only.
- Replace the hard-coded JWT secret in `application.properties` with `${JWT_SECRET_BASE64}`.
- Replace any hard-coded scanner token in Java/Python source with environment-based configuration.
- Use an EC2 IAM instance profile for AWS access after deployment; do not commit AWS access keys.
- Check `git status` and `git diff` before pushing.

The root `.gitignore` ignores `.env`, virtual environments, `node_modules`, Maven `target`, IDE files, and private key files. `.gitignore` does not remove files that Git is already tracking; use `git rm --cached` for any sensitive file already staged/tracked, then rotate the exposed secret.

## Run locally before pushing

From the repository root in PowerShell:

```powershell
cd "C:\MyFolder\EDGE\Infra Capstone\FileNexus"

# Backend compile/package (does not run integration tests)
cd backend\filenexus
mvn -DskipTests package
cd ..\..

# Frontend production build
cd frontend
npm ci
npm run build
cd ..

# Scanner syntax check
cd scanner
python -m compileall -q app
cd ..
```

## GitHub setup

Create an empty GitHub repository named `FileNexus` (do not initialize it with a README, license, or .gitignore if your local project already has those files).

Then:

```powershell
git init
git branch -M main
git add .
git status
git diff --cached
git commit -m "chore: prepare FileNexus for CI"
git remote add origin https://github.com/YOUR_GITHUB_USERNAME/FileNexus.git
git push -u origin main
```

Open the repository's Actions tab and confirm all three jobs pass.

Do not add AWS deploy credentials to GitHub Actions yet. This phase is CI only; EC2 deployment is the next phase after the repository is clean and CI is green.
