# 🤖 Guía Técnica para Agentes de IA - Proyecto SIAJ Backend

> **Propósito de este documento:**
> Este archivo resume el conocimiento operativo, arquitectónico, credenciales y trampas técnicas identificadas en el proyecto **SIAJ (Sistema de Información para la Administración de Justicia - Spring Boot)**. Todo futuro agente que trabaje en este repositorio debe consultar este documento antes de realizar cambios o despliegues.

---

## 📌 1. Visión General y Entorno

* **Stack:** Java 17 (OpenJDK), Spring Boot 3.4.12, Spring Data JPA, PostgreSQL (15 / latest), Thymeleaf, Spring Security + JWT, Nginx, Docker.
* **Rama principal activa de trabajo:** `codex/siaj-alcance-2-asuntos-independientes`
* **Worktree local:** `C:\Users\Rodrigo.DESKTOP-2MLHEH3\.codex\worktrees\siaj-alcance-1-semaforos\juridico-springboot`
* **Condición de red crítica:**
  * **Los servidores son AIR-GAPPED (NO tienen salida a Internet).**
  * **NUNCA intentes ejecutar comandos en los servidores que requieran red externa** (ej. `mvn clean install`, `docker pull`, `curl` a repositorios externos, `npm install`, `apt`/`dnf update`).
  * Toda compilación o descarga de dependencias debe realizarse **en la máquina local de trabajo (Windows)** y transferirse a los servidores empaquetada como Fat JAR o mediante archivos estáticos vía SCP.

---

## 🌐 2. Topología de Infraestructura y Credenciales

La máquina de desarrollo local tiene llaves SSH configuradas en `~/.ssh/` para acceso sin contraseña a los siguientes servidores:

### 🟡 Ambiente de Desarrollo (Red 30.0.0.0/24)

| Componente | IP | Usuario SSH | Contraseña SSH | Detalles del Servicio |
|---|---|---|---|---|
| **GitLab Interno** | `30.0.0.56` | — | — | Repositorio: `git@30.0.0.56:Rodrigo/juridico-siaj.git` |
| **Servidor BD Dev** | `30.0.0.150` | `agendajuridicodbdev` | `DBag3$#38` | Host Rocky Linux 9.1. Contenedores Docker `postgres_db` y `pgadmin4`. |
| **Servidor App Dev** | `30.0.0.151` | `agendajuridicoappdev` | `ag3App$dev#19` | Host Rocky Linux 9.7. Nginx (puerto 80) + Spring Boot (puerto 8080). |

#### Servidor de Base de Datos Dev (`30.0.0.150`)
* **Contenedor Postgres:** `postgres_db` (puerto expuesto `5432`).
* **Base de datos:** `sistema_juridico`
* **Usuario de BD:** `agendajuridicodbdev` (Superusuario)
* **Contraseña de BD:** `DBag3$#38`
* **Contenedor pgAdmin:** `pgadmin4` disponible en `http://30.0.0.150:5050`
  * **Login pgAdmin:** `admin@juridico.com`
  * **Password pgAdmin:** `admin123`
  * **Servidor configurado en pgAdmin:** "Postgres SIAJ Dev" (`host: postgres_db`, `port: 5432`, `user: agendajuridicodbdev`).
* **Persistencia Docker:**
  * Volumen: `agendajuridicodbdev_postgres_data` (marcado como `external: true` en `~/docker-compose.yml`).
  * Red: `agendajuridicodbdev_postgres_network` (marcado como `external: true`).

#### Servidor de Aplicación Dev (`30.0.0.151`)
* **Ubicación del JAR:** `/opt/siaj/app/sistema-juridico.jar`
* **Archivo de Entorno:** `/opt/siaj/config/.env`
* **Logs de la aplicación:** `/opt/siaj/logs/app.log`
* **Script de arranque del host:** `/opt/siaj/app/start.sh`
* **Script de despliegue:** `/home/agendajuridicoappdev/deploy.sh`
* **URL:** `http://30.0.0.151/` o `http://30.0.0.151:8080/`

---

### 🔴 Ambiente de Producción (Red 40.0.0.0/24)

> ⚠️ Las redes `30.0.0.x` y `40.0.0.x` están completamente aisladas entre sí.

| Componente | IP | Usuario SSH | Contraseña SSH | Detalles del Servicio |
|---|---|---|---|---|
| **Servidor BD Prod** | `40.0.0.60` | `ajdbprod` | `Ag3Ju$pR0` | Contenedores Docker `juridico-postgres` y `juridico-pgadmin`. |
| **Servidor App Prod** | `40.0.0.61` | `ajappprod` | `Ag3Ju$app3` | Dominio institucional: `https://siaj.finabien.gob.mx`. |

#### Servidor de Base de Datos Prod (`40.0.0.60`)
* **Contenedor Postgres:** `juridico-postgres` (puerto expuesto `5432`).
* **Base de datos:** `siaj`
* **Usuarios de BD:**
  * Usuario Aplicativo: `siaj_app` (credenciales administradas en el env del app).
  * Administrador: `siaj_postgres_admin`
  * Usuario Paridad Institucional: `agendajuridicodbdev` | `DBag3$#38` (Superusuario).
* **Contenedor pgAdmin:** `juridico-pgadmin` en `http://40.0.0.60:5050`
  * **Login pgAdmin:** `admin@finabien.gob.mx`

#### Servidor de Aplicación Prod (`40.0.0.61`)
* **Ubicación del JAR:** `/home/ajappprod/siaj/app/siaj.jar`
* **Servicio Systemd:** `siaj.service` (`systemctl status siaj`)
* **Archivo de Variables de Entorno:** `/etc/siaj/siaj.env`

---

## 🚀 3. Flujo de Despliegue Automatizado

Se ha diseñado el script PowerShell [`deploy.ps1`](file:///C:/Users/Rodrigo.DESKTOP-2MLHEH3/.codex/worktrees/siaj-alcance-1-semaforos/juridico-springboot/deploy.ps1) ubicado en la raíz del repositorio.

### ¿Cómo desplegar a Desarrollo?
```powershell
# Compilar y desplegar automáticamente
.\deploy.ps1 -Env dev

# Desplegar sin recompilar (si el JAR ya fue compilado previamente)
.\deploy.ps1 -Env dev -SkipBuild
```

### ¿Qué hace internamente `deploy.ps1`?
1. Detecta y termina automáticamente procesos `java.exe` locales que retengan locks sobre `target/` en Windows.
2. Ejecuta Maven clean package (`.\mvnw.cmd clean package -DskipTests`).
3. Transfiere el Fat JAR hacia el servidor remoto mediante SCP.
4. Ejecuta el script remoto `deploy.sh` que:
   - Respalda el JAR existente (`sistema-juridico.jar.bak-<fecha>`).
   - Mueve el nuevo JAR a su ubicación.
   - Detiene de forma segura el proceso previo (`SIGTERM` con fallback a `SIGKILL`).
   - Inicia la nueva versión en segundo plano y verifica que el nuevo PID permanezca activo y responda.

---

## ⚠️ 4. Trampas Técnicas Críticas y Gotchas (¡Evitar a toda costa!)

### 1. Variables con Caracteres Especiales en Bash (`$` y `#`)
* **Problema:** En archivos `.env` leídos mediante `source` de bash, una contraseña como `DB_PASSWORD=DBag3$#38` interpreta `$#` como el número de argumentos de la función/script (que evalúa a `0`), transformando silenciosamente la contraseña en `DBag3038` y causando fallos de autenticación `FATAL: password authentication failed for user "agendajuridicodbdev"`. Además, `#` sin entrecomillar puede interpretarse como inicio de comentario.
* **Regla estricta:** Todas las contraseñas en archivos `.env` o scripts bash **DEBEN IR ENTRE COMILLAS SIMPLES**:
  ```bash
  DB_PASSWORD='DBag3$#38'
  DEV_DB_PASSWORD='DBag3$#38'
  ```

### 2. Regla Absoluta de Nombres de Usuario de Base de Datos
* **NUNCA volver a introducir `villalobos_admin`:**
  * Ese usuario fue un remanente obsoleto que generó inconsistencias graves.
  * Fue completamente eliminado del motor Postgres (OID 10 fue renombrado a `agendajuridicodbdev`).
  * En todos los archivos (`application.properties`, `.env`, `docker-compose.yml`, documentación), el usuario oficial es **`agendajuridicodbdev`**.

### 3. Longitud de Columna `codigo` en Catálogos (`VARCHAR(50)`)
* En las tablas `tipos_audiencia` y `tipos_termino`, la columna `codigo` tiene restricción física `VARCHAR(50)`.
* Al generar slugs o códigos automáticos (en `AtencionTemporalInitializer` o servicios de dominio), el slug generado **debe ser truncado a máximo 38 caracteres** antes de concatenar `_` y el identificador:
  ```java
  // Ejemplo implementado:
  String safeBase = baseCodigo.length() > 38 ? baseCodigo.substring(0, 38) : baseCodigo;
  String codigo = safeBase + "_" + item.getId();
  ```
  Esto previene la excepción `value too long for type character varying(50)` que rompía el arranque de Spring Boot.

### 4. Preservación de Datos vs DDL en el Arranque
* **No borrar datos de desarrollo:** La base de datos `sistema_juridico` en `30.0.0.150` contiene registros vinculados (expedientes, audiencias, términos, usuarios).
* Antes de agregar restricciones `CHECK` (como la restricción XOR `(expediente_id IS NOT NULL) <> (asunto_independiente_id IS NOT NULL)`), [`DatabaseSchemaCompatibilityInitializer.java`](file:///C:/Users/Rodrigo.DESKTOP-2MLHEH3/.codex/worktrees/siaj-alcance-1-semaforos/juridico-springboot/src/main/java/com/juridico/sistema_juridico/config/DatabaseSchemaCompatibilityInitializer.java) elimina registros huérfanos incompatibles para evitar que Postgres falle al validar el constraint.

### 5. Configuración de Redes y Volúmenes en Docker Compose Remoto
* En `30.0.0.150`, el volumen `agendajuridicodbdev_postgres_data` ya contiene los datos persistentes del sistema.
* Si se ejecuta `docker compose up` sin declarar `external: true`, Docker Compose intentará crear un volumen nuevo o reportará conflicto de nombres.
* Mantener siempre en `docker-compose.yml`:
  ```yaml
  volumes:
    agendajuridicodbdev_postgres_data:
      external: true

  networks:
    postgres_network:
      name: agendajuridicodbdev_postgres_network
      external: true
  ```

---

## 🗺️ 5. Hoja de Ruta de Arquitectura (Decisión de Caminos)

* **Camino A (Implementado y Activo):**
  * Despliegue seguro mediante Fat JAR y script `deploy.ps1`.
  * Preservación intacta de datos en `30.0.0.150`.
  * Correcciones de esquema delegadas a [`DatabaseSchemaCompatibilityInitializer.java`](file:///C:/Users/Rodrigo.DESKTOP-2MLHEH3/.codex/worktrees/siaj-alcance-1-semaforos/juridico-springboot/src/main/java/com/juridico/sistema_juridico/config/DatabaseSchemaCompatibilityInitializer.java) y control de slugs en [`AtencionTemporalInitializer.java`](file:///C:/Users/Rodrigo.DESKTOP-2MLHEH3/.codex/worktrees/siaj-alcance-1-semaforos/juridico-springboot/src/main/java/com/juridico/sistema_juridico/config/AtencionTemporalInitializer.java).
* **Camino B (Siguiente Fase Planificada):**
  * Migración estructurada con Flyway (`V1__baseline_siaj.sql`).
  * Cambio de `spring.jpa.hibernate.ddl-auto` de `update` a `validate`.
  * Retiro gradual de inicializadores Java que ejecutan DDL al arranque.
