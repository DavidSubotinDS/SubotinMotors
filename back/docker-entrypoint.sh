#!/bin/sh
set -eu
if [ "${APP_DEMO_DATA_ACK:-}" != "I_ACCEPT_EXISTING_DEMO_DATA" ]; then
  echo 'Existing Flyway migrations seed demo accounts/data. Set APP_DEMO_DATA_ACK=I_ACCEPT_EXISTING_DEMO_DATA deliberately. See docs/docker-compose.md.' >&2
  exit 1
fi
exec java -jar /app/app.jar "$@"
