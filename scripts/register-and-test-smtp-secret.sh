#!/usr/bin/env bash
set -euo pipefail

main_pid="$(systemctl show siaj -p MainPID --value)"
if [[ -z "${main_pid}" || "${main_pid}" == "0" ]]; then
  echo "ERROR: SIAJ no esta activo." >&2
  exit 1
fi

migration_api_key="$(tr '\0' '\n' < "/proc/${main_pid}/environ" | sed -n 's/^MIGRATION_API_KEY=//p' | head -n 1)"
if [[ -z "${migration_api_key}" ]]; then
  echo "ERROR: No se encontro la llave interna de administracion." >&2
  exit 1
fi

read -r -s smtp_password
echo
if [[ -z "${smtp_password}" ]]; then
  echo "ERROR: La contrasena SMTP esta vacia." >&2
  exit 1
fi

response_file="$(mktemp)"
trap 'rm -f "${response_file}"; unset smtp_password migration_api_key' EXIT

next_change_date="$(date -d '+30 days' +%F)"
create_status="$(printf '%s' "${smtp_password}" | curl -sS \
  -o "${response_file}" -w '%{http_code}' \
  -H "X-API-KEY: ${migration_api_key}" \
  --data-urlencode password@- \
  --data-urlencode 'testEmail=rodrigo.paz@finabien.gob.mx' \
  --data-urlencode "nextChangeDate=${next_change_date}" \
  http://127.0.0.1:8080/admin/configuracion-correo/candidatas)"
unset smtp_password

if [[ "${create_status}" != "302" ]]; then
  echo "ERROR: El registro de la candidata respondio HTTP ${create_status}." >&2
  exit 1
fi

page_file="$(mktemp)"
trap 'rm -f "${response_file}" "${page_file}"; unset migration_api_key' EXIT
curl -sS -H "X-API-KEY: ${migration_api_key}" \
  http://127.0.0.1:8080/admin/configuracion-correo > "${page_file}"

test_path="$(grep -oE '/admin/configuracion-correo/[0-9]+/probar' "${page_file}" | head -n 1)"
if [[ -z "${test_path}" ]]; then
  echo "ERROR: No se encontro la candidata pendiente recien creada." >&2
  exit 1
fi

candidate_id="$(printf '%s' "${test_path}" | sed -E 's#^.*/([0-9]+)/probar$#\1#')"
echo "CANDIDATE_ID=${candidate_id}"
echo "CREATE_HTTP=${create_status}"
echo "TESTING=started"

test_status="$(curl -sS -o "${response_file}" -w '%{http_code}' \
  -H "X-API-KEY: ${migration_api_key}" \
  -X POST "http://127.0.0.1:8080${test_path}")"

echo "TEST_HTTP=${test_status}"
if [[ "${test_status}" != "302" ]]; then
  echo "ERROR: La prueba SMTP respondio HTTP ${test_status}." >&2
  exit 1
fi
