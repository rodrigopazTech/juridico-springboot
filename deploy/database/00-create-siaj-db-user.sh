#!/usr/bin/env bash
set -euo pipefail

: "${SIAJ_DB_USER:?SIAJ_DB_USER es requerido}"
: "${SIAJ_DB_PASSWORD:?SIAJ_DB_PASSWORD es requerido}"

psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  --set=target_db="$POSTGRES_DB" \
  --set=siaj_db_user="$SIAJ_DB_USER" \
  --set=siaj_db_password="$SIAJ_DB_PASSWORD" <<'SQL'
CREATE ROLE :"siaj_db_user" LOGIN PASSWORD :'siaj_db_password';
GRANT CONNECT, TEMPORARY ON DATABASE :"target_db" TO :"siaj_db_user";
GRANT USAGE, CREATE ON SCHEMA public TO :"siaj_db_user";
SQL
