FROM node:24-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build -- --base-href /app/

FROM caddy:2.6.4-alpine
COPY Caddyfile /etc/caddy/Caddyfile
COPY --from=builder /app/build/angular/browser/ /srv/

# Remove Caddy's permission for privileged ports
RUN apk add --no-cache --virtual .cap-utils libcap \
    && setcap -r /usr/bin/caddy \
    && apk del .cap-utils

# Create a non-root user for Caddy
RUN addgroup --system caddy \
    && adduser --system --disabled-password --no-create-home --ingroup caddy caddy

# Set ownership of the Caddy directories to the caddy user
RUN chown -R caddy:caddy /srv /etc/caddy

USER caddy

EXPOSE 8080
