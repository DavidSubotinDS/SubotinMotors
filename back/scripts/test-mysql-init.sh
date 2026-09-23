#!/usr/bin/env bash
# Run inside the pinned MySQL image with this directory mounted at /checks.
# No MySQL server, network, credentials or database volume is required.
set -eo pipefail
source /usr/local/bin/docker-entrypoint.sh

mkdir -p /tmp/init-test-bin
cat > /tmp/init-test-bin/mysql <<'STUB'
#!/bin/sh
cat > /tmp/init-test-sql
STUB
chmod +x /tmp/init-test-bin/mysql
export PATH="/tmp/init-test-bin:$PATH"
export MYSQL_ROOT_PASSWORD=test-only-root MYSQL_DATABASE=test_business
export DB_RUNTIME_USERNAME=test_runtime DB_RUNTIME_PASSWORD=test-only-runtime
export DB_MIGRATION_USERNAME=test_migration DB_MIGRATION_PASSWORD=test-only-migration
export IDENTITY_DB_NAME=test_identity IDENTITY_DB_USERNAME=test_identity
export IDENTITY_DB_PASSWORD=test-only-identity
export NOTIFICATION_DB_NAME=test_notification NOTIFICATION_DB_USERNAME=test_notification NOTIFICATION_DB_PASSWORD=test-only-notification
unset MYSQL_ONETIME_PASSWORD MYSQL_RANDOM_ROOT_PASSWORD AUTOSTRADA_E2E

# Match a Linux Git checkout: Git mode 100644 means the entrypoint sources it.
cp "${MYSQL_INIT_SCRIPT:-/checks/mysql-init-users.sh}" /tmp/20-autostrada-users.sh
chmod 644 /tmp/20-autostrada-users.sh
before_options=$-
docker_process_init_files /tmp/20-autostrada-users.sh
# This exact official-entrypoint call used to crash with an unbound variable.
mysql_expire_root_user
test "$-" = "$before_options"
grep -q 'CREATE DATABASE IF NOT EXISTS' /tmp/init-test-sql
grep -q 'WITH GRANT OPTION' /tmp/init-test-sql
echo 'PASS: sourced production init preserves parent options and password handling'

rm /tmp/init-test-sql
export AUTOSTRADA_E2E=true
docker_process_init_files /tmp/20-autostrada-users.sh
test "$-" = "$before_options"
test ! -e /tmp/init-test-sql
mysql_expire_root_user
echo 'PASS: sourced E2E skip returns to the parent without executing SQL'

# Both executable and sourced files are supported, regardless of host mount mode.
chmod +x /tmp/20-autostrada-users.sh
docker_process_init_files /tmp/20-autostrada-users.sh
test ! -e /tmp/init-test-sql
unset AUTOSTRADA_E2E
docker_process_init_files /tmp/20-autostrada-users.sh
test -s /tmp/init-test-sql
echo 'PASS: executable normal and E2E paths'

# Strict validation must still fail in the child and propagate to the caller.
unset DB_RUNTIME_PASSWORD
if (source /tmp/20-autostrada-users.sh) >/dev/null 2>&1; then
  echo 'FAIL: missing required configuration was silently accepted' >&2
  exit 1
fi
echo 'PASS: missing required configuration remains a failure'
