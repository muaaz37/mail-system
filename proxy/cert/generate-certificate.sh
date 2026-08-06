#!/usr/bin/env bash
# This script generates a self-signed certificate for the proxy server

set -euo pipefail

# Resolve the directory containing this script.
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"


openssl req -x509 -days 365 -nodes -batch -newkey rsa:4096 \
  -out "${SCRIPT_DIR}/proxy.crt" \
  -keyout "${SCRIPT_DIR}/proxy.key" \
  -config "${SCRIPT_DIR}/proxy.conf"

echo "Self-signed certificate created:"
echo "  proxy/cert/proxy.crt"
echo "  proxy/cert/proxy.key"