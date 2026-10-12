#!/bin/bash
# ==============================================================================
# SIAJ - Script de Despliegue y Ciclo de Vida (Ambiente de Desarrollo)
# Ingenieria DevOps / SRE - Financiera del Bienestar
# ==============================================================================
# Rutas:
#   JAR Activo: /opt/siaj/app/sistema-juridico.jar
#   PID:        /opt/siaj/app/app.pid
#   Logs:       /opt/siaj/logs/app.log
#   Config:     /opt/siaj/config/.env
# ==============================================================================
set -euo pipefail

APP_DIR="/opt/siaj/app"
JAR_NAME="sistema-juridico.jar"
ACTIVE_JAR="${APP_DIR}/${JAR_NAME}"
PID_FILE="${APP_DIR}/app.pid"
LOG_FILE="/opt/siaj/logs/app.log"
INCOMING_JAR="/home/agendajuridicoappdev/sistema-juridico-0.0.1-SNAPSHOT.jar"

echo "=== [SIAJ DEV] Iniciando ciclo de despliegue ==="

# 1. Respaldar versión anterior
if [ -f "${ACTIVE_JAR}" ]; then
    BACKUP_NAME="${ACTIVE_JAR}.bak-$(date +%Y%m%d-%H%M%S)"
    echo "[1/5] Creando respaldo: ${BACKUP_NAME}..."
    cp -p "${ACTIVE_JAR}" "${BACKUP_NAME}"
    
    # Política de retención: Mantener solo los 3 backups más recientes
    echo "       Aplicando política de retención (max 3 backups)..."
    ls -t "${APP_DIR}/${JAR_NAME}".bak-* 2>/dev/null | tail -n +4 | xargs -r rm -f
fi

# 2. Desplazar artefacto entrante
if [ -f "${INCOMING_JAR}" ]; then
    echo "[2/5] Actualizando artefacto activo..."
    mv "${INCOMING_JAR}" "${ACTIVE_JAR}"
else
    echo "[2/5] No se detectó nuevo artefacto entrante. Reiniciando versión actual..."
fi

# 3. Detener proceso anterior de forma limpia
echo "[3/5] Deteniendo instancia anterior..."
if [ -f "${PID_FILE}" ]; then
    PID=$(cat "${PID_FILE}" || true)
    if [ -n "${PID}" ] && kill -0 "${PID}" 2>/dev/null; then
        echo "       Enviando SIGTERM a PID ${PID}..."
        kill "${PID}" || true
        for i in {1..10}; do
            if ! kill -0 "${PID}" 2>/dev/null; then
                break
            fi
            sleep 1
        done
        if kill -0 "${PID}" 2>/dev/null; then
            echo "       El proceso no cedió a SIGTERM. Forzando SIGKILL..."
            kill -9 "${PID}" || true
        fi
    fi
    rm -f "${PID_FILE}"
fi

# Detener cualquier proceso huérfano remanente
REMANENTES=$(pgrep -f "${JAR_NAME}" || true)
if [ -n "${REMANENTES}" ]; then
    echo "       Limpiando PIDs huérfanos: ${REMANENTES}..."
    echo "${REMANENTES}" | xargs -r kill -9 2>/dev/null || true
fi

# 4. Rotación preventiva de log si excede 100MB
if [ -f "${LOG_FILE}" ]; then
    LOG_SIZE_KB=$(du -k "${LOG_FILE}" | cut -f1)
    if [ "${LOG_SIZE_KB}" -gt 102400 ]; then
        echo "[4/5] Rotando app.log preventivamente (>100MB)..."
        tail -n 10000 "${LOG_FILE}" > "${LOG_FILE}.prev"
        gzip -f -9 "${LOG_FILE}.prev"
        : > "${LOG_FILE}"
    fi
fi

# 5. Iniciar la aplicación
echo "[5/5] Iniciando SIAJ en segundo plano..."
cd "${APP_DIR}"
nohup ./start.sh >/dev/null 2>&1 &

# Esperar inicialización y capturar PID real
sleep 4
NEW_PID=$(pgrep -f "${JAR_NAME}" | head -n 1 || true)

if [ -n "${NEW_PID}" ]; then
    echo "${NEW_PID}" > "${PID_FILE}"
    echo "=== [OK] Aplicación iniciada exitosamente con PID: ${NEW_PID} ==="
else
    echo "=== [ERROR] No se detectó proceso Java en ejecución. Revisa ${LOG_FILE} ==="
    exit 1
fi

# Verificación de liveness HTTP
echo "Verificando disponibilidad HTTP..."
for attempt in {1..15}; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/ || true)
    if [ "${HTTP_CODE}" == "200" ] || [ "${HTTP_CODE}" == "302" ]; then
        echo "=== [EXITO] Servicio respondiendo en http://localhost:8080/ (HTTP ${HTTP_CODE}) ==="
        exit 0
    fi
    sleep 2
done

echo "=== [AVISO] El proceso sigue activo (PID ${NEW_PID}), pero la respuesta HTTP tardó más de 30s. ==="
exit 0
