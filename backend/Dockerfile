FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY backend/build.gradle.kts ./backend/build.gradle.kts
COPY frontend/build.gradle.kts ./frontend/build.gradle.kts
COPY backend/src ./backend/src

RUN --mount=type=cache,target=/root/.gradle \
    sed -i 's/\r$//' ./gradlew \
    && chmod +x ./gradlew \
    && ./gradlew :backend:installDist --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /workspace/backend/build/install/backend/ /app/

RUN chmod +x /app/bin/backend \
    && addgroup --system backend \
    && adduser --system --ingroup backend --no-create-home backend \
    && chown -R backend:backend /app

USER backend

EXPOSE 8080

CMD ["/app/bin/backend"]
