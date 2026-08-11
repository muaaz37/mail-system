# Mail Support System

Quick-start file for evaluating the project from a fresh checkout. The complete project documentation, architecture description, feature overview and development notes are in [README.adoc](./README.adoc).

## Requirements

- Docker Desktop with Docker Compose
- Java 21 for the included Gradle Wrapper
- Bash, for example Git Bash on Windows
- Free host ports `80` and `443`, unless overridden in `.env`

Docker Desktop must be running before the startup command is executed. A separate Gradle, Node.js or npm installation is not required for the standard container startup.

## Required inputs

Create a local environment file in the project root:

```bash
cp .env.example .env
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Local infrastructure values such as database, Keycloak and S3 credentials can use the evaluation defaults from `.env.example`. Replace the SMTP/IMAP mailbox values and the support sender address before startup. Real SMTP and IMAP mail communication requires valid mailbox settings.

Do not commit `.env`.
The `startLocal` task validates these values and stops before Docker Compose starts if required mailbox placeholders are still present.

## Start the complete system

Build and start the complete Docker Compose stack with one Gradle command:

```bash
./gradlew startLocal
```

On Windows PowerShell, use:

```powershell
./gradlew.bat startLocal
```

The task creates a local self-signed TLS certificate when necessary, builds the frontend and backend containers, starts all Docker services in the background and waits until the HTTPS health endpoint is reachable.

Open the application at:

- Frontend: <https://localhost/app/>
- API health: <https://localhost/api/health>
- Swagger UI: <https://localhost/api/swagger-ui>

The browser can show a warning because the local setup uses a self-signed TLS certificate.

## Evaluation users

Keycloak imports the following users during initial startup. The initial password is `123456` for every listed user.

| Username | Initial password |
| --- | --- |
| `aallanson@example.com` | `123456` |
| `svardey1@example.com` | `123456` |
| `jpoe@example.uk` | `123456` |
| `tianno3@example.com` | `123456` |
| `araisbeck4@example.com` | `123456` |

The first login can require setting a new password because the imported Keycloak credentials are temporary.

## Useful commands

```bash
./gradlew logsLocal   # Follow container logs
./gradlew stopLocal   # Stop services and keep Docker volumes
./gradlew resetLocal  # Stop services and remove Docker volumes
```

Use `resetLocal` only when local application, Keycloak and attachment data should be deleted.
