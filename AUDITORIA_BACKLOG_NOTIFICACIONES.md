# Auditoria y Backlog de Notificaciones

## Objetivo

Convertir la auditoria funcional del sistema de notificaciones en un backlog accionable, priorizado y listo para implementacion por etapas.

## Convenciones

- `Estado actual`: `Implementada`, `Parcial`, `No implementada`
- `Control usuario`: `Si`, `No`, `Parcial`
- `Canal actual`: `In-app`, `WebSocket`, `N/A`
- `Prioridad`: `P0`, `P1`, `P2`

## Matriz Formal

| ID | Categoria | Notificacion | Estado actual | Control usuario | Canal actual | Origen tecnico actual | Hallazgo / brecha | Prioridad |
|---|---|---|---|---|---|---|---|---|
| NTF-001 | Expedientes | Nuevo expediente registrado | Implementada | No | In-app, WebSocket | `ExpedientesController.enviarNotificacionesNuevoExpediente` | No existe preferencia dedicada para expediente nuevo | P1 |
| NTF-002 | Expedientes | Expediente reasignado | Implementada | Si | In-app, WebSocket | `ExpedientesController` + `PreferenciaNotificacion.notificarReasignacion` | Cubre solo reasignacion al nuevo responsable | P0 |
| NTF-003 | Expedientes | Cambio relevante de estado del expediente | No implementada | No | N/A | Solo hay auditoria | Falta notificacion funcional separada de auditoria | P1 |
| NTF-004 | Expedientes | Nueva actuacion / observacion en expediente | No implementada | No | N/A | Solo hay auditoria | Existe actividad, no alerta al usuario | P1 |
| NTF-005 | Expedientes | Documento agregado al expediente | No implementada | No | N/A | `DocumentoService` solo registra auditoria | Evento importante sin notificacion | P0 |
| NTF-006 | Expedientes | Expediente sin actividad | No implementada | No | N/A | No encontrado | Falta scheduler y regla de negocio | P2 |
| NTF-007 | Terminos | Nuevo termino asignado | Implementada | Parcial | In-app, WebSocket | `TerminosController.guardar` | El control depende del titulo, no de un evento formal | P0 |
| NTF-008 | Terminos | Termino por vencer en 3 dias | Implementada | Si | In-app, WebSocket | `NotificacionAutomaticaService` | Solo cubre 3 dias, no configurable por usuario | P1 |
| NTF-009 | Terminos | Termino por vencer en 1 dia | Implementada | Si | In-app, WebSocket | `NotificacionAutomaticaService` | No configurable por usuario | P1 |
| NTF-010 | Terminos | Termino vence hoy | Implementada | Si | In-app, WebSocket | `NotificacionAutomaticaService` | No distinguido como obligatorio | P0 |
| NTF-011 | Terminos | Termino vencido | Parcial | Parcial | In-app, WebSocket | `NotificacionAutomaticaService` usa titulos de vencimiento | No existe evento normalizado ni politica obligatoria | P0 |
| NTF-012 | Terminos | Cambio de fecha limite | No implementada | No | N/A | No encontrado | Falta detectar edicion de fecha y avisar | P0 |
| NTF-013 | Terminos | Cambio de etapa del termino | Implementada | Parcial | In-app, WebSocket | `TerminosController.notificarCambioDeEtapa` | No hay preferencia especifica para transiciones | P1 |
| NTF-014 | Terminos | Termino validado y concluido | Implementada | Parcial | In-app, WebSocket | `TerminoService.concluirTermino` | Entra por heuristica general de `TERMINO` | P1 |
| NTF-015 | Terminos | Termino atendido / cancelado | No implementada | No | N/A | No encontrado | Falta evento de cierre operacional | P1 |
| NTF-016 | Audiencias | Nueva audiencia programada | Implementada | Si | In-app, WebSocket | `AudienciaService.notificarCreacionAudiencia` | Preferencia demasiado amplia para todo tipo `AUDIENCIA` | P1 |
| NTF-017 | Audiencias | Asignacion de comparecencia | Implementada | Parcial | In-app, WebSocket | `AudienciaService.notificarCreacionAudiencia` | No hay switch propio | P1 |
| NTF-018 | Audiencias | Recordatorio 3 dias antes | Implementada | Si | In-app, WebSocket | `AudienciaNotificacionScheduler` | No configurable | P1 |
| NTF-019 | Audiencias | Recordatorio 1 dia antes | Implementada | Si | In-app, WebSocket | `AudienciaNotificacionScheduler` | No configurable | P1 |
| NTF-020 | Audiencias | Recordatorio el mismo dia | Implementada | Si | In-app, WebSocket | `AudienciaNotificacionScheduler` | No configurable | P1 |
| NTF-021 | Audiencias | Audiencia reprogramada | No implementada | No | N/A | No encontrado | Falta detectar cambios de fecha/hora/lugar | P0 |
| NTF-022 | Audiencias | Audiencia cancelada | No implementada | No | N/A | No encontrado | Falta evento y flujo de cancelacion | P0 |
| NTF-023 | Audiencias | Acta disponible | Implementada | Parcial | In-app, WebSocket | `AudienciaService.notificarActaDisponible` | Se mezcla con otras alertas de audiencia | P1 |
| NTF-024 | Audiencias | Audiencia validada y concluida | Implementada | Parcial | In-app, WebSocket | `AudienciaService.concluirAudiencia` | Sin preferencia especifica | P1 |
| NTF-025 | Documentos | Documento pendiente de revision | No implementada | No | N/A | No encontrado | Alta necesidad operacional | P0 |
| NTF-026 | Documentos | Documento rechazado / observado | No implementada | No | N/A | No encontrado | Debe ser obligatoria | P0 |
| NTF-027 | Documentos | Documento pendiente de firma | No implementada | No | N/A | No encontrado | Sin flujo actual | P2 |
| NTF-028 | Documentos | Documento firmado | No implementada | No | N/A | No encontrado | Sin flujo actual | P2 |
| NTF-029 | Tareas | Nueva tarea / recordatorio personal | No implementada | No | N/A | `TareasController` guarda recordatorios solamente | No genera alertas activas | P1 |
| NTF-030 | Tareas | Tarea por vencer | No implementada | No | N/A | No encontrado | Requiere scheduler | P1 |
| NTF-031 | Tareas | Tarea vencida | No implementada | No | N/A | No encontrado | Requiere scheduler | P1 |
| NTF-032 | Seguridad | Contrasena por vencer | Implementada | No | In-app, WebSocket | `PasswordExpirationScheduler` | Debe quedar marcada como obligatoria | P0 |
| NTF-033 | Seguridad | Cambio de contrasena | No implementada | No | N/A | Solo ejecuta cambio | Falta alerta posterior al cambio | P0 |
| NTF-034 | Seguridad | Cambio de correo o telefono | No implementada | No | N/A | No encontrado | Sin evento de seguridad | P1 |
| NTF-035 | Seguridad | Inicio de sesion inusual | No implementada | No | N/A | Solo auditoria de login en `SecurityConfig` | Falta deteccion y alerta | P0 |
| NTF-036 | Seguridad | Cambio de rol o permisos | No implementada | No | N/A | No encontrado | Debe ser obligatoria | P1 |
| NTF-037 | Seguridad | Cuenta bloqueada / reactivada | No implementada | No | N/A | No encontrado | Debe ser obligatoria | P1 |
| NTF-038 | Sistema | Centro de alertas y campana | Implementada | N/A | In-app, WebSocket | `NotificacionesController`, `navbar.html` | Funciona, pero depende de clasificacion debil | P0 |
| NTF-039 | Sistema | Preferencias por evento | No implementada | No | N/A | Solo hay 4 booleanos | Brecha principal del modulo | P0 |
| NTF-040 | Sistema | Canales externos correo/SMS/WhatsApp | No implementada | No | N/A | No encontrado | El sistema hoy solo notifica in-app | P2 |

## Problemas estructurales que deben entrar al backlog

| ID | Problema | Impacto | Prioridad |
|---|---|---|---|
| ARC-001 | Clasificacion por titulo en vez de evento formal | Genera falsos positivos, falsos negativos y control inconsistente | P0 |
| ARC-002 | Preferencias reducidas a 4 flags | No cubre el universo real de alertas | P0 |
| ARC-003 | Notificaciones a superiores no respetan preferencias propias | Se envian alertas aunque el destinatario las desactive | P0 |
| ARC-004 | No hay catalogo de alertas obligatorias | Riesgo funcional y de seguridad | P0 |
| ARC-005 | No hay versionado visible del trigger SQL realtime | Riesgo operativo y de despliegue | P1 |

## Backlog Inicial: Top 5 Issues

### ISSUE-01

- Titulo: Normalizar el modelo de eventos de notificacion
- Prioridad: P0
- Tipo: Arquitectura / Backend
- Problema: El sistema decide preferencias con base en `tipo` y texto del `titulo`, lo que rompe el control fino.
- Alcance:
  - Crear enum o catalogo tecnico `NotificationEventType`
  - Separar `tipo visual` de `evento funcional`
  - Persistir el evento en la tabla de notificaciones
  - Adaptar `NotificacionService` para evaluar preferencias por evento real
- Dependencias: Ninguna
- Criterios de aceptacion:
  - Cada notificacion nueva guarda un `evento`
  - No se usa `titulo.contains(...)` para resolver preferencias
  - Los eventos de terminos y audiencias quedan diferenciados

### ISSUE-02

- Titulo: Redisenar preferencias de notificacion por evento
- Prioridad: P0
- Tipo: Backend / Frontend / Datos
- Problema: `Mi Cuenta` solo maneja 4 switches y no representa lo que el sistema ya envia ni lo que falta por enviar.
- Alcance:
  - Sustituir `PreferenciaNotificacion` por un modelo por evento
  - Permitir `habilitada`, `obligatoria`, `canal`, `frecuencia`
  - Migrar valores actuales a la nueva estructura
  - Actualizar la vista `/perfil` para grupos por categoria
- Dependencias: `ISSUE-01`
- Criterios de aceptacion:
  - El usuario puede controlar al menos expedientes, terminos, audiencias, documentos y seguridad
  - Las notificaciones obligatorias aparecen activas y bloqueadas
  - La UI refleja el estado real guardado en base de datos

### ISSUE-03

- Titulo: Implementar alertas criticas faltantes de audiencia y termino
- Prioridad: P0
- Tipo: Backend
- Problema: Faltan eventos de alto impacto operativo como reprogramacion, cancelacion y cambio de fecha limite.
- Alcance:
  - Notificar `audiencia reprogramada`
  - Notificar `audiencia cancelada`
  - Notificar `cambio de fecha limite de termino`
  - Notificar `termino vencido` como evento formal
- Dependencias: `ISSUE-01`
- Criterios de aceptacion:
  - Cambiar fecha/hora/lugar de audiencia genera alerta
  - Cancelar audiencia genera alerta
  - Editar fecha de vencimiento de termino genera alerta
  - El vencimiento fuera de tiempo se registra como evento distinto a recordatorio

### ISSUE-04

- Titulo: Implementar alertas documentales operativas
- Prioridad: P0
- Tipo: Backend
- Problema: El gestor documental registra auditoria pero no alerta a responsables y revisores.
- Alcance:
  - Notificar `documento agregado`
  - Notificar `documento pendiente de revision`
  - Notificar `documento rechazado / observado`
  - Definir destinatarios por expediente, responsable y superior
- Dependencias: `ISSUE-01`, `ISSUE-02`
- Criterios de aceptacion:
  - Subir documento genera notificacion segun regla
  - Solicitar revision genera notificacion al revisor
  - Rechazar u observar documento genera alerta al responsable

### ISSUE-05

- Titulo: Implementar alertas obligatorias de seguridad
- Prioridad: P0
- Tipo: Backend / Seguridad
- Problema: Solo existe aviso por expiracion de contrasena; faltan alertas basicas de seguridad y trazabilidad.
- Alcance:
  - Notificar `cambio de contrasena`
  - Notificar `inicio de sesion inusual`
  - Notificar `cambio de rol o permisos`
  - Marcar estas alertas como obligatorias
- Dependencias: `ISSUE-01`, `ISSUE-02`
- Criterios de aceptacion:
  - El sistema genera alerta posterior al cambio de contrasena
  - Se detecta login desde contexto no habitual con criterio inicial configurable
  - Cambios de rol disparan alerta al usuario afectado

## Orden recomendado de ejecucion

1. `ISSUE-01` porque corrige el fundamento del modulo.
2. `ISSUE-02` porque convierte la arquitectura nueva en control real para el usuario.
3. `ISSUE-03` porque cubre eventos operativos criticos hoy ausentes.
4. `ISSUE-04` porque abre el frente documental, hoy sin alertas.
5. `ISSUE-05` porque completa el minimo de seguridad exigible.

## Siguiente corte sugerido

Despues de los 5 issues iniciales, el siguiente bloque recomendable es:

- `NTF-003`, `NTF-004`: cambios y actividad de expediente
- `NTF-029`, `NTF-030`, `NTF-031`: recordatorios personales activos
- `ARC-005`: versionado del trigger realtime
- `NTF-040`: canales externos
