#!/usr/bin/env bash
set -euo pipefail

if command -v docker >/dev/null 2>&1; then
  DOCKER_BIN="$(command -v docker)"
elif [[ -x /usr/local/bin/docker ]]; then
  DOCKER_BIN="/usr/local/bin/docker"
elif [[ -x /opt/homebrew/bin/docker ]]; then
  DOCKER_BIN="/opt/homebrew/bin/docker"
elif [[ -x /Applications/Docker.app/Contents/Resources/bin/docker ]]; then
  DOCKER_BIN="/Applications/Docker.app/Contents/Resources/bin/docker"
else
  cat >&2 <<'MESSAGE'
Docker CLI was not found.

Start Docker Desktop and make sure the Docker command line tools are available.
On macOS this usually means one of these paths exists:
  /usr/local/bin/docker
  /opt/homebrew/bin/docker
  /Applications/Docker.app/Contents/Resources/bin/docker
MESSAGE
  exit 127
fi

exec "${DOCKER_BIN}" compose "$@"
