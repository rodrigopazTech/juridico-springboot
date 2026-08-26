# Reporte de despliegue a servidor de pruebas

Fecha: 2026-08-03  
Servidor aplicativo: `30.0.0.151` (`juridico-server`)  
Servidor BD: `30.0.0.150` (`juridico-db-server`)  
Aplicacion: `/opt/siaj/app/sistema-juridico.jar`  
Perfil: `test`  
BD configurada: `jdbc:postgresql://30.0.0.150:5432/sistema_juridico`

## Resultado

El JAR fue compilado, subido al servidor `30.0.0.151`, reemplazado con respaldo previo y arrancado correctamente.

## Respaldos

| Tipo | Ubicacion |
| --- | --- |
| BD pre-deploy | `/home/agendajuridicodbdev/backups/sistema_juridico_pre_deploy_20260803-145434.dump` |
| BD pre-deploy adicional | `/home/agendajuridicodbdev/backups/sistema_juridico_pre_deploy_.dump` |
| JAR pre-deploy | `/opt/siaj/app/sistema-juridico.jar.bak-20260803-145557-pre-deploy` |

## Artefacto desplegado

| Dato | Valor |
| --- | --- |
| JAR local | `target/sistema-juridico-0.0.1-SNAPSHOT.jar` |
| SHA256 local/remoto | `4f814caecd4c9508fe78fe4e21ff77135a2c1b63de5f2796f166f90f0ffbba61` |
| PID activo post-deploy | `2139261` |
| Arranque confirmado | `Started SistemaJuridicoApplication` a las 14:56:43 CST |

## Integridad de datos

Conteos antes y despues del despliegue:

| Tabla | Antes | Despues | Resultado |
| --- | ---: | ---: | --- |
| `usuarios` | 39 | 39 | OK |
| `expedientes` | 8 | 8 | OK |
| `terminos` | 3 | 3 | OK |
| `audiencias` | 5 | 5 | OK |

No se ejecutaron scripts de limpieza, migracion manual ni comandos de modificacion directa de datos.

## Validacion post-deploy

| Prueba | Resultado |
| --- | --- |
| Login page `http://30.0.0.151/` | Carga |
| App local en servidor `http://localhost:8080/` | Carga |
| Proceso Java | Activo |
| Conexion a BD | Activa; Hibernate conecto a PostgreSQL 18.1 |
| Smoke autenticado con usuarios locales `director`, `subdirector`, `gerente`, `jefe`, `abogado` | No aplica en servidor: esas cuentas no existen en BD remota |

## Hallazgos post-deploy

| ID | Severidad | Hallazgo | Evidencia | Recomendacion |
| --- | --- | --- | --- | --- |
| DEP-001 | Alta | La pantalla de login del servidor aun contiene enlace a `/auth/microsoft/login` con texto `Continuar con correo institucional`. | HTML servido por `http://30.0.0.151/`. | Corregir antes de abrir la prueba si la salida de Microsoft debe ser completa para usuarios. |
| DEP-002 | Media | No fue posible ejecutar smoke autenticado con las credenciales QA locales porque en la BD remota no existen usuarios cortos `director`, `subdirector`, `gerente`, `jefe`, `abogado`. | Logs: `Failed to find user 'director'`, etc. | Usar usuarios reales de la BD remota o generar/restablecer cuentas QA controladas. |

## Usuarios activos detectados en BD remota

La BD remota contiene 39 usuarios activos. Algunos usuarios disponibles por rol:

| Rol | Usuarios |
| --- | --- |
| `DIRECTOR` | `juan.motta`, `mayra.juarez`, `michel.michel`, `michel.prueba`, `rodrigo.prueba` |
| `SUBDIRECTOR` | `julio.castellanos` |
| `GERENTE` | `gerente8`, `hebert.mendoza`, `laura.duran`, `yafidt.santillan`, `zyham.ravello` |
| `JEFE_DEPTO` | `edgar.reyes`, `jefe8`, `edgar_ivan_reyes_hernandez` |
| `ABOGADO` | `abogado.prueba`, `abop1`, `santiago.pruebas`, y usuarios migrados |

## Estado

Despliegue tecnico completado. Datos conservados. Queda decision funcional: abrir pruebas con el enlace Microsoft visible, o corregir ese remanente antes de invitar usuarios.
