<#
.SYNOPSIS
    Script de despliegue automatizado para SIAJ (Desarrollo y Produccion).

.DESCRIPTION
    Compila el proyecto con Maven y lo transfiere vía SSH/SCP al servidor correspondiente,
    ejecutando el reinicio del servicio de forma segura.

.PARAMETER Env
    Ambiente a desplegar: 'dev' (Desarrollo: 30.0.0.151) o 'prod' (Producción: 40.0.0.61).

.PARAMETER Branch
    (Opcional) Nombre de la rama git que deseas desplegar. Si no se indica, usa la rama actual.

.PARAMETER SkipBuild
    (Opcional) Si se especifica, no compila con Maven y utiliza el último JAR existente en /target.

.PARAMETER Force
    (Opcional) Omite la solicitud interactiva de confirmación al desplegar a Producción.

.EXAMPLE
    .\deploy.ps1 -Env dev
    .\deploy.ps1 -Env dev -Branch develop
    .\deploy.ps1 -Env prod -Branch main
    .\deploy.ps1 -Env prod -Force
    .\deploy.ps1 -Env dev -SkipBuild
#>

param(
    [Parameter(Mandatory = $true, HelpMessage = "Indica el ambiente: dev o prod")]
    [ValidateSet("dev", "prod")]
    [string]$Env,

    [Parameter(Mandatory = $false)]
    [string]$Branch = "",

    [Parameter(Mandatory = $false)]
    [switch]$SkipBuild,

    [Parameter(Mandatory = $false)]
    [switch]$Force
)

$ErrorActionPreference = "Stop"

# Colores de consola
function Write-Info($msg) { Write-Host "[INFO] $msg" -ForegroundColor Cyan }
function Write-Success($msg) { Write-Host "[EXITO] $msg" -ForegroundColor Green }
function Write-Warn($msg) { Write-Host "[AVISO] $msg" -ForegroundColor Yellow }
function Write-Err($msg) { Write-Host "[ERROR] $msg" -ForegroundColor Red }

# Validar confirmación en Producción
if ($Env -eq "prod" -and -not $Force) {
    Write-Warn "*****************************************************"
    Write-Warn " ATENCION: Estas a punto de desplegar a PRODUCCION   "
    Write-Warn " Servidor: 40.0.0.61 (ajappprod)                     "
    Write-Warn "*****************************************************"
    $confirm = Read-Host "¿Confirmas que deseas continuar con el despliegue a PRODUCCION? (s/N)"
    if ($confirm -ne "s" -and $confirm -ne "S") {
        Write-Info "Despliegue cancelado por el usuario."
        exit 0
    }
}

# 1. Cambio de rama si se especificó
if ($Branch -ne "") {
    Write-Info "Cambiando a la rama '$Branch'..."
    git checkout $Branch
    if ($LASTEXITCODE -ne 0) {
        Write-Err "Fallo al cambiar a la rama $Branch."
        exit 1
    }
    Write-Info "Actualizando cambios con git pull..."
    git pull
} else {
    $currentBranch = (git branch --show-current).Trim()
    Write-Info "Usando la rama actual: '$currentBranch'"
}

# 2. Compilación del proyecto con Maven Wrapper
$jarPath = "target\sistema-juridico-0.0.1-SNAPSHOT.jar"

if (-not $SkipBuild) {
    # Liberar proceso local si está bloqueando el JAR anterior
    try {
        $lockingProcs = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" -ErrorAction SilentlyContinue | 
            Where-Object { $_.CommandLine -like "*sistema-juridico-0.0.1-SNAPSHOT.jar*" }
        foreach ($proc in $lockingProcs) {
            Write-Warn "Deteniendo proceso Java local previo (PID $($proc.ProcessId)) para liberar el archivo JAR..."
            Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
        }
    } catch {
        # Ignorar si no hay permisos de consulta
    }

    Write-Info "Iniciando compilacion con Maven (saltando tests para mayor rapidez)..."
    $buildStart = Get-Date
    cmd /c ".\mvnw.cmd clean package -DskipTests"
    if ($LASTEXITCODE -ne 0) {
        Write-Err "Error durante la compilacion de Maven. Abortando despliegue."
        exit 1
    }
    $duration = [math]::Round(((Get-Date) - $buildStart).TotalSeconds, 1)
    Write-Success "Compilacion exitosa en $duration segundos."
} else {
    Write-Warn "Saltando fase de compilacion (-SkipBuild activo)."
}

if (-not (Test-Path $jarPath)) {
    Write-Err "No se encontro el archivo compilado en $jarPath."
    exit 1
}

$jarSizeMB = [math]::Round(((Get-Item $jarPath).Length / 1MB), 2)
Write-Info "Archivo a desplegar: $jarPath ($jarSizeMB MB)"

# 3. Despliegue segun el ambiente
if ($Env -eq "dev") {
    $devHost = "30.0.0.151"
    $devUser = "agendajuridicoappdev"
    $devRemoteDest = "/home/agendajuridicoappdev/sistema-juridico-0.0.1-SNAPSHOT.jar"

    Write-Info "Transfiriendo archivo JAR a Desarrollo ($devHost)..."
    scp -o BatchMode=yes $jarPath "${devUser}@${devHost}:${devRemoteDest}"
    if ($LASTEXITCODE -ne 0) {
        Write-Err "Fallo la transferencia por SCP hacia Desarrollo."
        exit 1
    }
    Write-Success "Archivo transferido a $devHost."

    Write-Info "Ejecutando script de despliegue en Desarrollo..."
    ssh -o BatchMode=yes "${devUser}@${devHost}" "/home/agendajuridicoappdev/deploy.sh"
    if ($LASTEXITCODE -ne 0) {
        Write-Err "Ocurrio un error al ejecutar deploy.sh en el servidor de Desarrollo."
        exit 1
    }

    Write-Success "=========================================================="
    Write-Success " ¡DESPLIEGUE EN DESARROLLO FINALIZADO CON EXITO!           "
    Write-Success " URL: http://30.0.0.151:8080                               "
    Write-Success "=========================================================="

} elseif ($Env -eq "prod") {
    $prodHost = "40.0.0.61"
    $prodUser = "ajappprod"
    $prodRemoteDest = "/home/ajappprod/siaj/app/siaj.jar"
    $backupTimestamp = (Get-Date -Format "yyyyMMdd-HHmmss")

    Write-Info "Creando respaldo de la version actual en Produccion..."
    ssh -o BatchMode=yes "${prodUser}@${prodHost}" "if [ -f $prodRemoteDest ]; then cp $prodRemoteDest ${prodRemoteDest}.bak-${backupTimestamp}; echo 'Respaldo creado: ${prodRemoteDest}.bak-${backupTimestamp}'; fi"

    Write-Info "Transfiriendo archivo JAR a Produccion ($prodHost)..."
    scp -o BatchMode=yes $jarPath "${prodUser}@${prodHost}:${prodRemoteDest}"
    if ($LASTEXITCODE -ne 0) {
        Write-Err "Fallo la transferencia por SCP hacia Produccion."
        exit 1
    }
    Write-Success "Archivo transferido a $prodHost."

    Write-Info "Reiniciando servicio siaj en Produccion via ciclo de vida Systemd..."
    $restartCmd = 'old_pid=$(systemctl show siaj -p MainPID --value 2>/dev/null); if [ -n "$old_pid" ] && [ "$old_pid" != "0" ]; then kill -INT "$old_pid"; fi; for i in $(seq 1 45); do sleep 1; new_pid=$(systemctl show siaj -p MainPID --value 2>/dev/null); active=$(systemctl show siaj -p ActiveState --value 2>/dev/null); if [ "$active" = "active" ] && [ -n "$new_pid" ] && [ "$new_pid" != "0" ] && [ "$new_pid" != "$old_pid" ]; then echo "SIAJ reiniciado con exito en PID: $new_pid"; exit 0; fi; done; exit 1'
    ssh -o BatchMode=yes "${prodUser}@${prodHost}" "$restartCmd"
    if ($LASTEXITCODE -ne 0) {
        Write-Err "Fallo al reiniciar el servicio siaj en Produccion."
        exit 1
    }

    Write-Info "Verificando estado del servicio..."
    Start-Sleep -Seconds 3
    ssh -o BatchMode=yes "${prodUser}@${prodHost}" "systemctl status siaj --no-pager | head -n 12"

    Write-Success "=========================================================="
    Write-Success " ¡DESPLIEGUE EN PRODUCCION FINALIZADO CON EXITO!           "
    Write-Success " URL: https://siaj.finabien.gob.mx o http://40.0.0.61:8080 "
    Write-Success "=========================================================="
}
