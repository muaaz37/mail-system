#!/usr/bin/env bash

set -e
set -o pipefail

# Container network configuration.
NETWORK_INTERFACE_NAME="${NETWORK_INTERFACE_NAME:-eth0}"
FORWARD_PORTS="${FORWARD_PORTS:-8080 8443}"

# Ensure that the target service is configured.
if [[ -z "${FORWARD_HOST:-}" ]]; then
  echo "Error: FORWARD_HOST is not configured."
  exit 1
fi

# Determine the IPv4 address of the IPS container.
NETWORK_ADDRESS_CIDR=$(
  ip -4 addr show "${NETWORK_INTERFACE_NAME}" |
    grep "inet\b" |
    awk '{print $2}'
)

NETWORK_ADDRESS=$(
  echo "${NETWORK_ADDRESS_CIDR}" |
    cut -d'/' -f1
)

# Ensure that the network address was resolved.
if [[ -z "${NETWORK_ADDRESS}" ]]; then
  echo "Could not resolve the IPS container address."
  exit 1
fi

# Resolve the IPv4 address of the target container.
TARGET_ADDRESS=$(
  getent ahostsv4 "${FORWARD_HOST}" |
    awk 'NR == 1 {print $1}'
)

# Ensure that the target address was resolved.
if [[ -z "${TARGET_ADDRESS}" ]]; then
  echo "Could not resolve forward host: ${FORWARD_HOST}"
  exit 1
fi

echo "Network address: ${NETWORK_ADDRESS}"
echo "Forward host: ${FORWARD_HOST}"
echo "Forward ports: ${FORWARD_PORTS}"
echo "Target address: ${TARGET_ADDRESS}"

# Forward each configured port to the target container.
for port in ${FORWARD_PORTS}; do
  iptables -t nat -A PREROUTING -p tcp --dport "${port}" -j DNAT --to-destination "${TARGET_ADDRESS}:${port}"

  iptables -t nat -A POSTROUTING -p tcp -d "${TARGET_ADDRESS}" --dport "${port}" -j SNAT --to-source "${NETWORK_ADDRESS}"
done

# redirect all forwarded packets to queue watched by snort
# (queue-num identifies the queue and has to match the snort "--daq-var queue=X" option)
iptables -I FORWARD -j NFQUEUE --queue-num=1

# Insert the runtime network configuration into the Snort configuration.
sed -i "s|__HOME_NET__|${TARGET_ADDRESS}/32|g" /etc/snort/snort.lua
sed -i "s|__HTTP_PORTS__|${FORWARD_PORTS}|g" /etc/snort/snort.lua

# Validate the Snort configuration.
echo "Validating Snort configuration..."
snort -T -c /etc/snort/snort.lua

# Print the effective configuration for diagnostic purposes.
cat /etc/snort/snort.lua

exec "$@"