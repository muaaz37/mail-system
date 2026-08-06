FROM owasp/modsecurity-crs:4.25-nginx-lts

# The base image renders templates at startup so Compose environment variables
# can configure the unprivileged HTTP/TLS listener and mounted TLS secrets.
COPY --chown=nginx:nginx nginx.conf /etc/nginx/templates/conf.d/default.conf.template

EXPOSE 8080 8443
