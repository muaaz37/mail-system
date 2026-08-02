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

# Set the entrypoint to the Keycloak startup script
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
