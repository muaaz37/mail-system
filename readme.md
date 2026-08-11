# Mail Support System

This guide contains the information required to start and evaluate the application from a fresh checkout. Existing build artifacts or local Gradle and Node.js installations are not required.

## Requirements

- Docker Desktop with Docker Compose
- Java 21 for the included Gradle Wrapper
- Bash (for example Git Bash on Windows)
- Available host ports `80` and `443`

Docker Desktop must be running before the startup command is executed. A separate Gradle, Node.js or npm installation is not required: the Gradle Wrapper and Docker handle the build.

## Start the application

Create the local environment file in the project root.

PowerShell:

```powershell
Copy-Item .env.example .env
```

Bash:

```bash
cp .env.example .env
```

The `.env` file contains the local database, storage, Keycloak and mail configuration. The provided values are sufficient for evaluating the application with local services. Sending and importing real external emails requires valid SMTP and IMAP account settings.

Build and start the complete system with one Gradle command:

```bash
./gradlew startLocal
```

On Windows PowerShell, use `./gradlew.bat startLocal`. The task creates the local TLS certificate when necessary, builds and starts all Docker services in the background, and waits for the health endpoint.

Alternatively, if the local TLS certificate has already been generated, the services can be built and started directly with Docker Compose:

```bash
docker compose up --build
```

For a fresh checkout, `./gradlew startLocal` is recommended because it also generates the required certificate and verifies that the application becomes healthy.

The first startup can take several minutes because Docker downloads the required base images and builds the frontend, backend, Keycloak, proxy and IPS images. Keep the terminal open and wait until the databases, Keycloak, backend, frontend and proxy report that they are ready or healthy.

Then open [https://localhost/app/](https://localhost/app/). The browser may display a warning because the local environment uses a self-signed TLS certificate.

The backend health status can be checked at [https://localhost/api/health](https://localhost/api/health).

## Optional ports

Docker Compose uses HTTP port `80` and HTTPS port `443` when no port variables are defined. To use different host ports, add them to `.env`:

```env
HTTP_HOST_PORT=8080
HTTPS_HOST_PORT=8443
```

With this example, the application is available at `https://localhost:8443/app/`.

## Evaluation account

Keycloak imports this account during the initial startup:

- Username: `aallanson@example.com`
- Initial password: `123456`

Additional evaluation accounts are documented in [README.adoc](./README.adoc).

## Useful commands

The Gradle tasks provide shortcuts for logs, shutdown and a complete reset:

```bash
./gradlew logsLocal   # Follow logs
./gradlew stopLocal   # Stop services and keep data
./gradlew resetLocal  # Stop services and delete local data
```

The running containers can also be inspected or stopped directly with Docker Compose:

```bash
docker compose ps       # Show service status
docker compose logs -f  # Follow logs
docker compose down     # Stop services and keep data
docker compose down -v  # Stop services and delete local data
```

Use `./gradlew resetLocal` or `docker compose down -v` only when a complete reset is required. Both remove the application database, Keycloak data and stored attachments. The data is created again during the next startup.

For architecture, configuration and development information, see [README.adoc](./README.adoc).
