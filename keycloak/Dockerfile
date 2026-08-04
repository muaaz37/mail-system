FROM quay.io/keycloak/keycloak:26.6.3 AS builder

# Health support is a Keycloak build-time option. Baking it into the image

# allows us to avoid having to build Keycloak at runtime
ENV KC_HEALTH_ENABLED=true \
    KC_DB=postgres

# Build Keycloak with the health support enabled
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:26.6.3

# Copy the built Keycloak from the builder stage
COPY --from=builder /opt/keycloak/ /opt/keycloak/

# Set the health support environment variable for runtime
ENV KC_HEALTH_ENABLED=true \
    KC_DB=postgres

# Verify that Keycloak is ready to process requests.
HEALTHCHECK --interval=5s --timeout=5s --start-period=90s --retries=30 \
    CMD { printf 'HEAD /health/ready HTTP/1.0\r\n\r\n' >&0; grep 'HTTP/1.0 200'; } 0<>/dev/tcp/127.0.0.1/9000

# Set the entrypoint to the Keycloak startup script
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
