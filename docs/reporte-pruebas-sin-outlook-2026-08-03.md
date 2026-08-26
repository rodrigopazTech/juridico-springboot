# Reporte de pruebas web sin Outlook/Microsoft

Fecha: 2026-08-03  
Ambiente probado: `http://localhost:8080`  
Base de datos: PostgreSQL Docker `postgres-juridico` en `localhost:5433`  
Alcance: pantallas operativas usadas desde `DIRECTOR_PRINCIPAL` hacia abajo. Se excluyen `ADMIN`, Migracion y Auditoria administrativa por decision de alcance.  
Modo: pruebas en navegador con Playwright y revision de errores visibles; no se aplicaron correcciones.

## Resumen ejecutivo

La aplicacion compila y las pruebas unitarias pasan. Las pantallas operativas cargan e interactuan correctamente para `DIRECTOR`, `SUBDIRECTOR`, `GERENTE`, `JEFE_DEPTO` y `ABOGADO`.

No se detectaron llamadas fallidas a servicios de Microsoft, Outlook, Graph, Entra u Office durante la navegacion probada. Tampoco se detectaron botones o enlaces visibles a Microsoft/Outlook en las pantallas autenticadas revisadas.

El unico bloqueo vigente dentro del alcance solicitado es `DIRECTOR_PRINCIPAL`: el usuario existe y esta activo, pero no inicia sesion con las credenciales conocidas.

## Lotes ejecutados

| Lote | Alcance | Resultado |
| --- | --- | --- |
| Compilacion/pruebas backend | `mvnw.cmd test` | OK: 7 tests, 0 fallas |
| Busqueda tecnica | Referencias `Outlook`, `Microsoft`, `Graph`, `Entra`, `Office` en codigo | Persisten referencias en backend/modelo/login; requiere revision de limpieza tecnica |
| Publicas | Login, recuperar contrasena, recordar usuario, reset sin token, activar sin token, cambio obligatorio | Cargan sin errores JS |
| Pantallas por rol | Dashboard, Expedientes, Terminos, Audiencias, Agenda, Calendario, Tareas, Usuarios, Catalogos, Perfil, Notificaciones | OK para roles que inician sesion |
| Interacciones por pantalla | Modales, filtros, tabs, cambios de vista calendario | OK sin errores JS |
| Deteccion Outlook/Microsoft en UI | Texto visible, links y botones | Sin botones/links Microsoft/Outlook visibles; solo aparece la palabra `outlook` en datos de prueba |
| Red/navegador | Errores de consola y peticiones fallidas | Sin errores relevantes ni requests fallidas a Microsoft/Outlook |

## Resultado por rol

| Rol | Login | Pantallas operativas | Observaciones |
| --- | --- | --- | --- |
| `DIRECTOR_PRINCIPAL` | Falla | No ejecutadas por falta de sesion | Usuario activo, credencial conocida no funciona |
| `DIRECTOR` | OK | OK | Sin errores JS ni dependencias Outlook visibles |
| `SUBDIRECTOR` | OK | OK | Sin errores JS ni dependencias Outlook visibles |
| `GERENTE` | OK | OK | Sin errores JS ni dependencias Outlook visibles |
| `JEFE_DEPTO` | OK | OK | Sin errores JS ni dependencias Outlook visibles |
| `ABOGADO` | OK | OK | `/usuarios` bloquea correctamente con `403` |

## Resultado por pantalla

| Pantalla | Prueba ejecutada | Resultado |
| --- | --- | --- |
| Login `/` | Carga, inspeccion de texto visible, links y consola | OK; no hay link visible a Microsoft/Outlook |
| Recuperar contrasena `/auth/solicitar-reset` | Carga y revision de consola | OK |
| Recordar usuario `/auth/recordar-usuario` | Carga y revision de consola | OK; mantiene texto funcional de correo institucional |
| Reset sin token `/auth/reset-password` | Redireccion controlada a login | OK |
| Activacion sin token `/auth/activar` | Redireccion controlada a login | OK |
| Cambio obligatorio `/cambiar-password` | Carga formulario | OK |
| Dashboard `/dashboard` | Carga tablero y metricas | OK |
| Expedientes `/expedientes` | Carga listado y abre modal Nuevo Expediente | OK |
| Terminos `/terminos` | Carga listado, abre filtros y modal Nuevo Termino | OK |
| Audiencias `/audiencias` | Carga listado, abre filtros y modal Nueva Audiencia | OK |
| Agenda `/agenda` | Carga pantalla e interactua con botones de concluidos | OK |
| Calendario `/calendario` | Cambia vista Semana, Mes y periodo siguiente | OK |
| Tareas `/tareas` | Abre modal Nueva tarea y captura titulo sin guardar | OK |
| Usuarios `/usuarios` | Carga con roles autorizados y abre modal Agregar Usuario | OK |
| Usuarios como `ABOGADO` | Acceso directo a `/usuarios` | Bloqueado con `403`, esperado |
| Catalogos `/catalogos` | Carga tabs y cambia a Materias | OK |
| Perfil `/perfil` | Cambia a tabs Notificaciones y Contrasena | OK |
| Notificaciones `/notificaciones` | Carga listado/centro de notificaciones | OK |

## Hallazgos

| ID | Severidad | Modulo | Hallazgo | Evidencia | Estado |
| --- | --- | --- | --- | --- | --- |
| SO-001 | Alta | Autenticacion | `DIRECTOR_PRINCIPAL` no inicia sesion con `director.principal` / `12345`. | Login termina en `/?error=true`; el usuario existe y esta activo en BD desde revision previa. | Pendiente |
| SO-002 | Media | Limpieza tecnica | Aunque la UI no muestra botones/links a Outlook/Microsoft, aun hay referencias en clases y templates: `MicrosoftAuthController`, `MicrosoftAuthProperties`, `MicrosoftGraphClient`, DTOs `microsoft`, campos `microsoft_*` en usuario y fragmentos de login/activacion. | Busqueda `rg` encontro referencias `Microsoft`, `Graph`, `microsoft_*`, `/auth/microsoft/*`. | Pendiente de revision tecnica |
| SO-003 | Baja | Datos de prueba | Aparece texto con `outlook` en Terminos, Calendario y Notificaciones, pero corresponde al dato `QA termino sin outlook`, no a una integracion activa. | Lineas visibles: `QA termino sin outlook` y notificacion relacionada. | Limpieza opcional de datos QA |
| SO-004 | Baja | UX / Seguridad | `ABOGADO` bloqueado correctamente en `/usuarios`, pero la respuesta sigue siendo Whitelabel Error Page. | `/usuarios` como `ABOGADO` devuelve `403` con Whitelabel. | Pendiente |

## Confirmaciones sobre Outlook/Microsoft

| Punto revisado | Resultado |
| --- | --- |
| Botones visibles Microsoft/Outlook en pantallas autenticadas | No detectados |
| Links visibles a rutas Microsoft/Outlook | No detectados |
| Peticiones fallidas a Microsoft/Outlook/Graph/Office durante navegacion | No detectadas |
| Errores JS por ausencia de Outlook/Microsoft | No detectados |
| Referencias tecnicas en codigo | Si persisten, requieren limpieza si la meta es eliminacion completa |

## Recomendacion

Se puede proceder con pruebas funcionales del sistema operativo sin esperar a Migracion/Admin. Antes de una prueba formal con `DIRECTOR_PRINCIPAL`, hay que restablecer o confirmar su contrasena. Si el objetivo es eliminar por completo Microsoft/Outlook, no basta la UI: aun queda trabajo tecnico de limpieza en backend/modelo/templates.
