# Reporte de ejecucion de pruebas SIAJ

Fecha: 2026-07-31  
Ambiente: `http://localhost:8080`  
Base de datos: PostgreSQL Docker `postgres-juridico` en `localhost:5433`  
Modo de trabajo: ejecucion de pruebas y documentacion de hallazgos. No se aplicaron correcciones.  
Alcance actualizado: se prioriza el sistema operativo desde `DIRECTOR_PRINCIPAL` hacia abajo. Administracion `ADMIN` y Migracion quedan diferidos para una fase posterior.

## Resumen ejecutivo

El sistema levanta correctamente, la base de datos esta disponible y las pruebas unitarias pasan. La navegacion principal carga sin errores para `DIRECTOR`, `SUBDIRECTOR`, `GERENTE`, `JEFE_DEPTO` y `ABOGADO`. El rol `DIRECTOR_PRINCIPAL` existe y esta activo en base de datos, pero no pudo iniciar sesion con las credenciales de prueba conocidas.

No se detectaron fallas criticas de caida general del sistema operativo. Para una prueba con usuarios reales del flujo no administrador, el principal bloqueo es recuperar/restablecer la credencial de `DIRECTOR_PRINCIPAL`; los demas hallazgos son controlables en una ronda interna.

## Lotes ejecutados

| Lote | Alcance | Resultado |
| --- | --- | --- |
| Backend Maven | `mvnw.cmd test` | OK: 7 tests, 0 fallas |
| Smoke de pantallas existente | `scripts/qa-page-smoke.js` | OK para 5 roles; falsos negativos en `ADMIN` y `DIRECTOR_PRINCIPAL` por credenciales |
| Smoke de permisos existente | `scripts/qa-role-smoke.js` | Permisos correctos para roles que inician sesion; falsos negativos en textos de menu y credenciales |
| Alcance de expedientes | `scripts/qa-scope-check.js` | Bloquea expediente fuera de alcance redirigiendo a dashboard; falta mensaje explicito |
| CRUD tareas existente | `scripts/qa-tareas-crud.js` | Timeout por credenciales/selector del script, no se considera falla funcional directa |
| CRUD tareas ajustado | Playwright con credenciales correctas | OK: crear, completar y eliminar para 6 roles |
| Rutas por rol | Playwright con credenciales diferenciadas | OK salvo `DIRECTOR_PRINCIPAL`, que no entra |
| APIs y exportaciones | HTTP autenticado como `ADMIN` | APIs OK; exportaciones OK; plantillas de migracion sospechosas |
| Inventario de pantallas | Playwright como `ADMIN` | Todas las pantallas principales cargan sin Whitelabel ni error visible |

## Hallazgos

| ID | Severidad | Pantalla/Modulo | Hallazgo | Evidencia | Estado |
| --- | --- | --- | --- | --- | --- |
| H-001 | Alta | Autenticacion QA | `DIRECTOR_PRINCIPAL` no inicia sesion con `director.principal` / `12345`, `director.principal@juridico.com` / `12345`, ni combinaciones probadas con `admin123`. El usuario existe, esta activo y no tiene cambio obligatorio. | Login termina en `/?error=true`; consulta BD confirma `activo=true`, `rol=DIRECTOR_PRINCIPAL`. | Pendiente |
| H-002 | Media | Automatizacion QA | Los scripts `qa-page-smoke.js` y `qa-role-smoke.js` usan una sola contrasena (`12345`), pero `ADMIN` entra con `admin123`. | `ADMIN` falla en scripts existentes; pasa en prueba diferenciada. | Pendiente |
| H-003 | Media | UX / Seguridad | Los accesos denegados `403` muestran Whitelabel Error Page. | `/admin/importar` como no admin y `/usuarios` como abogado devuelven Whitelabel. | Pendiente |
| H-004 | Media | Scripts QA | `qa-tareas-crud.js` puede colgarse por login fallido y no corta temprano. | Timeout de 184 s. | Pendiente |
| H-005 | Baja | Scripts QA | Selector `text=Nueva tarea` es ambiguo porque coincide con boton y titulo/modal. | Playwright strict mode detecta 2 o 3 coincidencias. | Pendiente |
| H-006 | Media | Migracion | Diferido por decision de alcance. Plantillas `/admin/importar/plantilla/terminos` y `/admin/importar/plantilla/audiencias` se descargan como `.xlsx`, pero los bytes iniciales son texto, no formato XLSX. | Pesan 106/123 bytes; primeros bytes `NumeroEx...`; un XLSX valido inicia como ZIP `PK`. | Diferido |
| H-007 | Baja | Alcance expedientes | Al intentar abrir expediente fuera de alcance se redirige al dashboard sin mensaje claro de acceso denegado. | `GERENTE`, `JEFE_DEPTO` y `ABOGADO` terminan en `/dashboard`. | Pendiente |
| H-008 | Baja | QA / Menu | Expectativas del script no coinciden con textos actuales: `Agenda Historial`, `Catalogos Generales`. | Menu real muestra otros textos o variantes. | Pendiente |
| H-009 | Baja | Calendario API | `/api/calendario/usuarios` devuelve arreglo vacio como `ADMIN`. | Respuesta `[]`. Puede ser valido, pero requiere confirmacion funcional. | Por confirmar |

## Cobertura por pantalla

| Pantalla | Historia de usuario validada | Pruebas ejecutadas | Resultado |
| --- | --- | --- | --- |
| Login | Como usuario, quiero entrar con credenciales validas. | Login por rol; login fallido; redireccion sin sesion. | OK salvo `DIRECTOR_PRINCIPAL` |
| Recuperar contrasena | Como usuario, quiero solicitar recuperacion. | Carga `/auth/solicitar-reset`; valida token invalido. | OK carga; flujo de correo no probado |
| Recordar usuario | Como usuario, quiero recuperar mi usuario. | Carga `/auth/recordar-usuario`. | OK carga; envio no probado |
| Cambio obligatorio | Como usuario, quiero actualizar contrasena requerida. | Carga `/cambiar-password`. | OK carga; guardado no probado |
| Dashboard | Como usuario, quiero ver metricas. | Carga por rol; `/dashboard/data`. | OK |
| Expedientes | Como usuario, quiero listar y acceder a expedientes. | Carga por rol; exportacion Excel; alcance por expediente permitido/no permitido. | OK con observacion H-007 |
| Detalle expediente | Como usuario, quiero abrir un expediente permitido. | Acceso a expediente civil de prueba por roles operativos. | OK |
| Terminos | Como usuario, quiero consultar y exportar terminos. | Carga por rol; exportacion Excel; inventario de acciones. | OK carga/exportacion; CRUD no ejecutado en esta ronda |
| Audiencias | Como usuario, quiero consultar y exportar audiencias. | Carga por rol; exportacion Excel; inventario de acciones. | OK carga/exportacion; CRUD no ejecutado en esta ronda |
| Agenda | Como usuario, quiero ver historial de audiencias y terminos. | Carga por rol; inventario de botones. | OK |
| Calendario | Como usuario, quiero ver eventos por periodo. | Carga por rol; `/api/calendario/eventos`, gerencias, usuarios. | OK con observacion H-009 |
| Tareas | Como usuario, quiero crear, completar y eliminar mis tareas. | CRUD temporal por `ADMIN`, `DIRECTOR`, `SUBDIRECTOR`, `GERENTE`, `JEFE_DEPTO`, `ABOGADO`. | OK |
| Usuarios | Como usuario autorizado, quiero administrar usuarios. | Carga por roles autorizados; bloqueo a `ABOGADO`; inventario de acciones. | OK; 403 con Whitelabel |
| Catalogos | Como usuario, quiero consultar catalogos. | Carga por rol; bloqueo de escritura no reejecutado en esta ronda; inventario de tabs. | OK carga |
| Perfil | Como usuario, quiero administrar mi cuenta. | Carga por rol; inventario de formularios. | OK carga; guardado no probado |
| Notificaciones | Como usuario, quiero ver alertas recientes. | Carga por rol; `/notificaciones/api/recientes`. | OK |
| Migracion | Como admin, quiero importar datos. | Diferido por decision de alcance. | Diferido |
| Auditoria tecnica | Como admin, quiero revisar actividad. | Diferido por decision de alcance. | Diferido |

## Pruebas de conflicto entre funciones

| Conflicto | Resultado |
| --- | --- |
| Rol no admin accede a `/admin/importar` | Bloqueado con `403` |
| Rol no admin accede a `/admin/auditoria` | Bloqueado con `403` |
| `ABOGADO` accede a `/usuarios` | Bloqueado con `403` |
| Usuario operativo abre expediente fuera de alcance | No ve el expediente; redirige a dashboard |
| Tareas propias entre roles | Crear, completar y eliminar funciona |
| Tarea de admin visible para abogado | No visible en evidencia historica del script |
| Exportaciones Excel | Expedientes, terminos y audiencias descargan archivos XLSX validos por cabecera ZIP |
| Plantillas migracion | Diferido por decision de alcance |

## Evidencia tecnica resumida

| Verificacion | Resultado |
| --- | --- |
| App en `8080` | En ejecucion |
| PostgreSQL en `5433` | En ejecucion |
| pgAdmin | En ejecucion en `5051` |
| Tests unitarios | 7/7 OK |
| `/dashboard/data` | 200 JSON |
| `/api/calendario/eventos` | 200 JSON |
| `/notificaciones/api/recientes` | 200 JSON |
| `/api/webpush/public-key` | 200 JSON |
| `/expedientes/exportar-excel` | 200, XLSX |
| `/terminos/exportar-excel` | 200, XLSX |
| `/audiencias/exportar-excel` | 200, XLSX |

## Pendientes de prueba profunda

Estas pruebas requieren datos controlados y pueden modificar registros de negocio; conviene ejecutarlas despues de acordar si se usa una base temporal, snapshot o datos ficticios:

| Prioridad | Modulo | Pendiente |
| --- | --- | --- |
| Alta | Expedientes | Crear, editar, cambiar etapa y validar historial/documentos |
| Alta | Terminos | Crear, avanzar, concluir, subir acuse y validar notificacion |
| Alta | Audiencias | Crear, concluir/desahogar, subir acta y descargar acta |
| Media | Usuarios | Crear usuario temporal, cambiar rol, activar/desactivar y validar login |
| Media | Catalogos | Crear/editar/desactivar por rol y por URL directa |
| Media | Perfil | Cambiar datos/preferencias y cambiar contrasena en usuario temporal |
| Diferida | Migracion | Importar archivo controlado y validar lote, duplicados y rollback esperado |
| Media | Documentos | Subir, previsualizar, descargar, enviar a papelera y restaurar |
| Baja | Responsive | Revisar pantallas en mobile/tablet y detectar solapes visuales |

## Recomendacion de cierre de fase

Antes de corregir, conviene completar los pendientes de prueba profunda sobre una base de datos controlada, excluyendo por ahora Administracion y Migracion. Con el panorama actual, el primer elemento a sanar para continuar el alcance operativo es la credencial de `DIRECTOR_PRINCIPAL`; despues, pagina 403 personalizada y actualizacion de scripts automatizados.
