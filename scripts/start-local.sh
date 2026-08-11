#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${MAIL_SYSTEM_ENV_FILE:-${ROOT_DIR}/.env}"
CERT_SCRIPT="${ROOT_DIR}/proxy/cert/generate-certificate.sh"
CERT_FILE="${ROOT_DIR}/proxy/cert/proxy.crt"
KEY_FILE="${ROOT_DIR}/proxy/cert/proxy.key"

if [[ ! -f "${ENV_FILE}" ]]; then
  cat >&2 <<'MESSAGE'
Missing .env file.
Create it from .env.example, configure the required mailbox values, then run this command again:

  cp .env.example .env
MESSAGE
  exit 1
fi

CONFIG_ERRORS=()

# Read simple KEY=value lines without sourcing .env, so local files cannot execute shell code.
env_value() {
  local key="$1"
  local line=""
  local value=""

  line="$(grep -E "^[[:space:]]*${key}=" "${ENV_FILE}" | tail -n 1 || true)"
  if [[ -z "${line}" ]]; then
    printf ''
    return
  fi

  value="${line#*=}"
  value="${value%$'\r'}"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  local first_char="${value:0:1}"
  local last_char="${value: -1}"
  if [[ "${#value}" -ge 2 && "${first_char}" == '"' && "${last_char}" == '"' ]]; then
    value="${value:1:${#value}-2}"
  elif [[ "${#value}" -ge 2 && "${first_char}" == "'" && "${last_char}" == "'" ]]; then
    value="${value:1:${#value}-2}"
  fi

  printf '%s' "${value}"
}

add_config_error() {
  CONFIG_ERRORS+=("  - $1")
}

require_env_value() {
  local key="$1"
  local value=""
  value="$(env_value "${key}")"
  if [[ -z "${value}" ]]; then
    add_config_error "${key} is missing or empty."
  fi
}

reject_placeholder_value() {
  local key="$1"
  local value=""
  value="$(env_value "${key}")"
  if [[ "${value}" == replace_with_* ]]; then
    add_config_error "${key} still contains the .env.example placeholder value."
  fi
}

reject_exact_value() {
  local key="$1"
  local forbidden="$2"
  local reason="$3"
  local value=""
  value="$(env_value "${key}")"
  if [[ "${value}" == "${forbidden}" ]]; then
    add_config_error "${key} must be configured (${reason})."
  fi
}

validate_env_file() {
  local required_values=(
    DB_PASSWORD
    STORAGE_S3_ACCESS_KEY
    STORAGE_S3_SECRET_KEY
    KEYCLOAK_ADMIN_PASSWORD
    KEYCLOAK_DB_PASSWORD
    KEYCLOAK_ISSUER_URI
    KEYCLOAK_JWK_SET_URI
    MAIL_SMTP_HOST
    MAIL_SMTP_USERNAME
    MAIL_SMTP_PASSWORD
    MAIL_IMAP_HOST
    MAIL_IMAP_USERNAME
    MAIL_IMAP_PASSWORD
    SUPPORT_MAIL_ADDRESS
  )

  for key in "${required_values[@]}"; do
    require_env_value "${key}"
    reject_placeholder_value "${key}"
  done

  reject_exact_value MAIL_SMTP_HOST mail.example.org "use the real SMTP host of the support mailbox"
  reject_exact_value MAIL_IMAP_HOST mail.example.org "use the real IMAP host of the support mailbox"
  reject_exact_value MAIL_SMTP_USERNAME mailbox-user "use the real SMTP username"
  reject_exact_value MAIL_IMAP_USERNAME mailbox-user "use the real IMAP username"
  reject_exact_value SUPPORT_MAIL_ADDRESS support@example.org "use the real shared support email address"

  if (( ${#CONFIG_ERRORS[@]} > 0 )); then
    cat >&2 <<'MESSAGE'
.env is not ready for the complete mail-support workflow.

Local infrastructure defaults for PostgreSQL, SeaweedFS and Keycloak can stay as provided by .env.example.
The external SMTP/IMAP mailbox values and the support sender address must be configured before startup.

Fix these values:
MESSAGE
    printf '%s\n' "${CONFIG_ERRORS[@]}" >&2
    cat >&2 <<'MESSAGE'

Then run:
  ./gradlew startLocal
MESSAGE
    exit 1
  fi
}

validate_env_file

if [[ "${MAIL_SYSTEM_VALIDATE_ONLY:-false}" == "true" ]]; then
  echo "Environment configuration is valid."
  exit 0
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
