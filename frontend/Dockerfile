FROM node:24.12.0-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build -- --base-href /app/

FROM caddy:2.6.4-alpine
COPY Caddyfile /etc/caddy/Caddyfile
COPY docker-entrypoint.sh /usr/local/bin/mail-system-frontend-entrypoint
COPY --from=builder /app/build/angular/browser/ /srv/

# Remove Caddy's permission for privileged ports
RUN apk add --no-cache --virtual .cap-utils libcap \
    && setcap -r /usr/bin/caddy \
    && apk del .cap-utils

# Create a non-root user for Caddy
RUN addgroup --system caddy \
    && adduser --system --disabled-password --no-create-home --ingroup caddy caddy

# Set ownership of the Caddy directories to the caddy user
RUN chmod +x /usr/local/bin/mail-system-frontend-entrypoint \
    && mkdir -p /config \
    && chown -R caddy:caddy /srv /etc/caddy /config

USER caddy

# Verify that the frontend can serve the Angular application.
HEALTHCHECK --interval=5s --timeout=5s --start-period=30s --retries=30 \
    CMD wget --quiet --spider http://localhost:8080/app/ || exit 1

EXPOSE 8080

ENTRYPOINT ["/usr/local/bin/mail-system-frontend-entrypoint"]
