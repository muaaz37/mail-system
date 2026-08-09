FROM alpine:3.23

# Install Snort 3, the NFQUEUE DAQ and required networking tools.
RUN apk add --no-cache bash daq iproute2 iptables snort

# Ensure that Snort supports NFQUEUE inline processing.
RUN snort --daq-list | grep -q "nfq"

# Download and install the official Snort 3 community rules.
RUN wget -qO /tmp/snort3-community-rules.tar.gz \
      https://www.snort.org/downloads/community/snort3-community-rules.tar.gz \
    && tar -xzf /tmp/snort3-community-rules.tar.gz -C /etc/snort/ \
    && rm /tmp/snort3-community-rules.tar.gz

# Copy the Snort configuration file into the container.
COPY snort.lua /etc/snort/snort.lua
COPY local.rules /etc/snort/local.rules

# Copy the container startup scripts.
COPY --chmod=0755 entrypoint.sh /entrypoint.sh
COPY --chmod=0755 snort.sh /snort.sh

# Configure the network before starting Snort.
ENTRYPOINT ["/entrypoint.sh"]

# Start Snort in inline IPS mode.
CMD ["/snort.sh"]