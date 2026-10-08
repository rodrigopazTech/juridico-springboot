#!/usr/bin/env bash
set -euo pipefail

docker exec -i juridico-postgres sh -c \
  'exec psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  < /home/ajdbprod/inspect-smtp-runtime.sql
