FROM eclipse-temurin:21-alpine-3.23

WORKDIR /app

# Copy the Server distribution in app
COPY build/install/backend/ /app/

# Make the start script (gradle script) executable (ownership stays root, which is fine)
RUN chmod +x /app/bin/backend \
&& addgroup --system backend \
&& adduser --system --ingroup backend --no-create-home backend \
&& chown -R backend:backend /app

# Create Linux user backend
USER backend

EXPOSE 8080

# Execute the backend script
ENTRYPOINT ["/app/bin/backend"]
