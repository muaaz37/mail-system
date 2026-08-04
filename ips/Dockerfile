FROM haproxy:3.2-alpine

# Use an unprivileged account because HAProxy listens only on ports above 1024.
COPY --chown=65532:65532 \
  haproxy.cfg \
  /usr/local/etc/haproxy/haproxy.cfg

# Run HAProxy as an unprivileged user.
USER 65532:65532

# Internal container ports:
#   8080 -> forwards HTTP to the WAF's redirect listener
#   8443 -> forwards encrypted TLS traffic to the WAF
EXPOSE 8080 8443

# Run a health check to ensure that HAProxy is running and the configuration is valid.
HEALTHCHECK --interval=10s --timeout=5s --start-period=5s --retries=3 \
  CMD haproxy -c -f /usr/local/etc/haproxy/haproxy.cfg || exit 1