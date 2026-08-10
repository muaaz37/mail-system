#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/.env"
CERT_SCRIPT="${ROOT_DIR}/proxy/cert/generate-certificate.sh"
CERT_FILE="${ROOT_DIR}/proxy/cert/proxy.crt"
KEY_FILE="${ROOT_DIR}/proxy/cert/proxy.key"

if [[ ! -f "${ENV_FILE}" ]]; then
  cat >&2 <<'MESSAGE'
Missing .env file.
Create it from .env.example, replace all placeholder credentials, then run this command again:

  cp .env.example .env
MESSAGE
  exit 1
fi

if [[ ! -f "${CERT_FILE}" || ! -f "${KEY_FILE}" ]]; then
  bash "${CERT_SCRIPT}"
fi

cd "${ROOT_DIR}"
if ! bash "${ROOT_DIR}/scripts/docker-compose-local.sh" up -d --build --remove-orphans; then
  cat >&2 <<'MESSAGE'
Docker Compose could not start the complete stack.

Inspect the startup logs with:
  ./gradlew logsLocal
MESSAGE

  KEYCLOAK_LOGS="$(bash "${ROOT_DIR}/scripts/docker-compose-local.sh" logs --no-color --tail=240 keycloak 2>/dev/null || true)"
  if [[ "${KEYCLOAK_LOGS}" == *'password authentication failed for user "keycloak"'* ]]; then
    cat >&2 <<'MESSAGE'

Detected Keycloak database authentication failure.
This usually means that the persisted keycloak_db_data Docker volume was initialized
with different Keycloak database credentials than the current .env file.

Choose one:
  - Keep local data: restore the original Keycloak database credentials in .env.
  - Reset local development data: run ./gradlew resetLocal and then ./gradlew startLocal.
MESSAGE
  fi

  MAIL_SERVER_LOGS="$(bash "${ROOT_DIR}/scripts/docker-compose-local.sh" logs --no-color --tail=240 mail-server 2>/dev/null || true)"
  if [[ "${MAIL_SERVER_LOGS}" == *'password authentication failed'* ]]; then
    cat >&2 <<'MESSAGE'

Detected backend database authentication failure.
This usually means that the persisted db_data Docker volume was initialized
with different application database credentials than the current .env file.

Choose one:
  - Keep local data: restore the original application database credentials in .env.
  - Reset local development data: run ./gradlew resetLocal and then ./gradlew startLocal.
MESSAGE
  fi

  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  cat <<'MESSAGE'
Docker Compose stack was started in the background.

curl is not available, so readiness could not be checked from this script.
Use ./gradlew logsLocal to inspect startup logs.
MESSAGE
  exit 0
fi

HTTPS_PORT="$(
  bash "${ROOT_DIR}/scripts/docker-compose-local.sh" port ips 8443 2>/dev/null \
    | tail -n 1 \
    | awk -F: '{print $NF}'
)"
HTTPS_PORT="${HTTPS_PORT:-443}"
HEALTH_URL="https://localhost:${HTTPS_PORT}/api/health"

printf 'Waiting for %s' "${HEALTH_URL}"
for _ in {1..90}; do
  if curl --fail --insecure --silent --output /dev/null "${HEALTH_URL}"; then
    printf '\n'
    cat <<MESSAGE
Docker Compose stack is ready.

Frontend: https://localhost:${HTTPS_PORT}/app/
API health: ${HEALTH_URL}
Logs: ./gradlew logsLocal
Stop: ./gradlew stopLocal
MESSAGE
    exit 0
  fi
  printf '.'
  sleep 2
done

printf '\n' >&2
cat >&2 <<MESSAGE
Docker Compose stack was started, but the HTTPS health endpoint did not become ready in time.

Inspect the containers with:
  ./gradlew logsLocal
  bash scripts/docker-compose-local.sh ps
MESSAGE
exit 1
