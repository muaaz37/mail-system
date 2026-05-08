FROM node:24-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM caddy:2.6.4-alpine
COPY Caddyfile /etc/caddy/Caddyfile
COPY --from=builder /app/build/angular/browser/ /usr/share/caddy/
