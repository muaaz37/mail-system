#!/usr/bin/env bash

set -e
set -o pipefail

echo "Starting Snort in inline IPS mode..."

# Start Snort using the NFQUEUE configuration from snort.lua
COMMAND=(
  "snort"

  # Enable inline packet processing.
  "-Q"

  # Bind Snort explicitly to the NFQUEUE used by the iptables FORWARD rule.
  "--daq" "nfq"
  "--daq-var" "queue=1"

  # Load the Snort configuration.
  "-c" "/etc/snort/snort.lua"

  # Disable checksum validation because checksum offloading in container
  # networks can expose packets before their checksums are finalized.
  "-k" "none"

  # Enable full alert output.
  "-A" "alert_full"
)

echo "Executing: ${COMMAND[*]}"
exec "${COMMAND[@]}"
