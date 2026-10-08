#!/usr/bin/env bash
set -euo pipefail

db_container="juridico-postgres"
deploy_dir="/home/ajdbprod"
backup_dir="${deploy_dir}/siaj-backups"
migration_file="${deploy_dir}/07-dynamic-smtp-credentials.sql"
audit_file="${deploy_dir}/08-verify-schema.sql"

test -r "${migration_file}"
test -r "${audit_file}"
docker inspect "${db_container}" >/dev/null

mkdir -p "${backup_dir}"
chmod 700 "${backup_dir}"

echo "=== Auditoria previa ==="
docker exec -i "${db_container}" sh -c \
  'exec psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  < "${audit_file}"

backup_file="${backup_dir}/pre-dynamic-smtp-$(date +%Y%m%d-%H%M%S).dump"
docker exec "${db_container}" sh -c \
  'exec pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' \
  > "${backup_file}"
chmod 600 "${backup_file}"
test -s "${backup_file}"
echo "BACKUP=${backup_file}"
stat -c 'BACKUP_SIZE=%s' "${backup_file}"

echo "=== Aplicando migracion 07 en una transaccion ==="
docker exec -i "${db_container}" sh -c \
  'exec psql -X -1 -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  < "${migration_file}"

echo "=== Asignando objetos SMTP al usuario de la aplicacion ==="
docker exec -i "${db_container}" sh -c \
  'exec psql -X -1 -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v app_user="$SIAJ_DB_USER"' <<'SQL'
ALTER TABLE public.configuraciones_correo_smtp OWNER TO :"app_user";
ALTER SEQUENCE public.configuraciones_correo_smtp_id_seq OWNER TO :"app_user";
SQL

echo "=== Auditoria posterior ==="
docker exec -i "${db_container}" sh -c \
  'exec psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  < "${audit_file}"

sha256sum "${migration_file}" "${audit_file}" "${backup_file}"
