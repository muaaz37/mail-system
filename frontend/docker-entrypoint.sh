#!/bin/sh
set -eu

CONFIG_DIR="${RUNTIME_CONFIG_DIR:-/config}"
CONFIG_FILE="${CONFIG_DIR}/runtime-config.js"
MAILBOX_REFRESH_INTERVAL_MS="${MAILBOX_REFRESH_INTERVAL_MS:-15000}"
MAILBOX_REFRESH_TIMEOUT_MS="${MAILBOX_REFRESH_TIMEOUT_MS:-10000}"

case "${MAILBOX_REFRESH_INTERVAL_MS}" in
  ''|*[!0-9]*) MAILBOX_REFRESH_INTERVAL_MS=15000 ;;
esac

case "${MAILBOX_REFRESH_TIMEOUT_MS}" in
  ''|*[!0-9]*) MAILBOX_REFRESH_TIMEOUT_MS=10000 ;;
esac

mkdir -p "${CONFIG_DIR}"
cat > "${CONFIG_FILE}" <<CONFIG
window.mailSystemConfig = {
  mailboxRefreshIntervalMs: ${MAILBOX_REFRESH_INTERVAL_MS},
  mailboxRefreshTimeoutMs: ${MAILBOX_REFRESH_TIMEOUT_MS}
};
CONFIG

exec caddy run --config /etc/caddy/Caddyfile --adapter caddyfile
