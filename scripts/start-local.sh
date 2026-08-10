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
bash "${ROOT_DIR}/scripts/docker-compose-local.sh" up --build
