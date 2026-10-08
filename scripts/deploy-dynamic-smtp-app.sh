#!/usr/bin/env bash
set -euo pipefail

app_dir="/home/ajappprod/siaj/app"
active_jar="${app_dir}/siaj.jar"
new_jar="${app_dir}/siaj.jar.new"
expected_sha256="${1:?Indique la huella SHA-256 esperada del JAR}"

actual_sha256="$(sha256sum "${new_jar}" | awk '{print $1}')"
if [[ "${actual_sha256}" != "${expected_sha256}" ]]; then
  echo "ERROR: La huella del JAR nuevo no coincide." >&2
  exit 1
fi

old_pid="$(systemctl show siaj -p MainPID --value)"
if [[ -z "${old_pid}" || "${old_pid}" == "0" ]]; then
  echo "ERROR: No se encontro el proceso activo de SIAJ." >&2
  exit 1
fi

backup_jar="${app_dir}/siaj.jar.bak-$(date +%Y%m%d-%H%M%S)"
cp -p "${active_jar}" "${backup_jar}"
mv "${new_jar}" "${active_jar}"
echo "BACKUP=${backup_jar}"
echo "OLD_PID=${old_pid}"

# SIGINT permite al JVM ejecutar su apagado normal. systemd lo considera fallo
# y aplica la politica Restart=on-failure configurada para el servicio.
kill -INT "${old_pid}"

for attempt in $(seq 1 60); do
  new_pid="$(systemctl show siaj -p MainPID --value)"
  active_state="$(systemctl show siaj -p ActiveState --value)"
  if [[ "${active_state}" == "active" && "${new_pid}" != "0" && "${new_pid}" != "${old_pid}" ]]; then
    echo "NEW_PID=${new_pid}"
    echo "ACTIVE_STATE=${active_state}"
    sha256sum "${active_jar}"
    exit 0
  fi
  sleep 1
done

echo "ERROR: SIAJ no volvio a estado activo dentro de 60 segundos." >&2
systemctl show siaj -p ActiveState -p SubState -p MainPID --no-pager >&2
exit 1
