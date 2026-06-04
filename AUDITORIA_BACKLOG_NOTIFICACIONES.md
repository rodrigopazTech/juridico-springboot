# Auditoria y Backlog de Notificaciones

## Objetivo

Convertir la auditoria funcional del sistema de notificaciones en un backlog accionable, depurado y alineado con el alcance real del producto.

## Contexto funcional aprobado

El sistema de notificaciones debe separarse en tres capas:

1. `Catalogo de tipos de notificacion`
   Define los eventos formales del sistema, sus reglas, categoria, prioridad por defecto y si son obligatorios o configurables.

2. `Preferencias por evento`
   Define que eventos puede activar o desactivar cada usuario segun su perfil y necesidades operativas.

3. `Registro historico de notificaciones enviadas`
   Guarda cada notificacion real entregada a un usuario para consulta, lectura, archivado y auditoria.

## Aclaracion clave

Cuando en este documento se habla de "normalizar el modelo de eventos de notificacion", no se propone solo guardar mejor el historial. Se propone:

- mantener la tabla de notificaciones como historico auditable;
- agregar un catalogo formal de tipos de notificacion;
- y conectar las preferencias del usuario a ese catalogo.

Esto evita que el sistema dependa de texto libre en el titulo para decidir si una alerta debe enviarse o no.

## Convenciones

- `Estado actual`: `Implementada`, `Parcial`, `No implementada`
- `Control usuario`: `Si`, `No`, `Parcial`
- `Canal actual`: `In-app`, `WebSocket`, `N/A`
- `Decision`: `Se implementa`, `Se elimina`, `Se mueve a dashboard`, `Se analiza despues`
- `Prioridad`: `P0`, `P1`, `P2`

## Matriz Formal Depurada

| ID | Categoria | Notificacion o KPI | Estado actual | Control usuario | Canal actual | Decision | Hallazgo / brecha | Prioridad |
|---|---|---|---|---|---|---|---|---|
| NTF-001 | Expedientes | Nuevo expediente registrado | Implementada | No | In-app, WebSocket | Se implementa | No existe preferencia dedicada por evento | P1 |
| NTF-002 | Expedientes | Expediente reasignado | Implementada | Si | In-app, WebSocket | Se implementa | Cubre solo reasignacion al nuevo responsable | P0 |
| KPI-003 | Expedientes | Cambio relevante de estado del expediente | No implementada | N/A | N/A | Se mueve a dashboard | Mejor como dato consultable o bitacora visible que como alerta push | P2 |
| KPI-004 | Expedientes | Nueva actividad general del expediente | No implementada | N/A | N/A | Se elimina como notificacion generica | Duplicaria alertas de terminos y audiencias si no se define mejor | P2 |
| NTF-005 | Expedientes | Documento agregado al expediente | No implementada | No | N/A | Se implementa | El gestor documental registra auditoria, pero no alerta al responsable | P1 |
| KPI-006 | Expedientes | Expediente sin actividad | No implementada | N/A | N/A | Se mueve a dashboard | Debe mostrarse como metrica o widget, no como notificacion push | P2 |
| NTF-007 | Terminos | Nuevo termino asignado | Implementada | Parcial | In-app, WebSocket | Se implementa | El control depende del titulo, no de un evento formal | P0 |
| NTF-008 | Terminos | Termino por vencer en 3 dias | Implementada | Si | In-app, WebSocket | Se implementa | No configurable por evento ni anticipacion | P1 |
| NTF-009 | Terminos | Termino por vencer en 1 dia | Implementada | Si | In-app, WebSocket | Se implementa | No configurable por evento ni anticipacion | P1 |
| NTF-010 | Terminos | Termino vence hoy | Implementada | Si | In-app, WebSocket | Se implementa | Debe evaluarse como alerta mas fuerte dentro del catalogo | P0 |
| NTF-011 | Terminos | Termino vencido | Parcial | Parcial | In-app, WebSocket | Se implementa | Debe quedar como evento formal distinto a recordatorio preventivo | P0 |
| NTF-012 | Terminos | Cambio de fecha limite | No implementada | No | N/A | Se implementa | Falta detectar cambio de vencimiento y notificarlo | P0 |
| NTF-013 | Terminos | Cambio de etapa del termino | Implementada | Parcial | In-app, WebSocket | Se implementa | No tiene preferencia especifica por transicion | P1 |
| NTF-014 | Terminos | Termino validado y concluido | Implementada | Parcial | In-app, WebSocket | Se implementa | Ya cubre el cierre validado actual | P1 |
| NTF-015 | Terminos | Termino atendido o cancelado | No implementada | No | N/A | Se elimina | Duplicaria el cierre si el flujo real ya usa "validado y concluido" | P2 |
| NTF-016 | Audiencias | Nueva audiencia programada | Implementada | Si | In-app, WebSocket | Se implementa | La preferencia actual es demasiado amplia para todo `AUDIENCIA` | P1 |
| NTF-017 | Audiencias | Asignacion de comparecencia | Implementada | Parcial | In-app, WebSocket | Se implementa | No tiene switch propio | P1 |
| NTF-018 | Audiencias | Recordatorio 3 dias antes | Implementada | Si | In-app, WebSocket | Se implementa | No configurable por usuario | P1 |
| NTF-019 | Audiencias | Recordatorio 1 dia antes | Implementada | Si | In-app, WebSocket | Se implementa | No configurable por usuario | P1 |
| NTF-020 | Audiencias | Recordatorio el mismo dia | Implementada | Si | In-app, WebSocket | Se implementa | No configurable por usuario | P1 |
| NTF-021 | Audiencias | Audiencia reprogramada | No implementada | No | N/A | Se implementa | Falta detectar cambios de fecha, hora o lugar | P0 |
| NTF-022 | Audiencias | Audiencia cancelada | No implementada | No | N/A | Se implementa | Falta evento y flujo de cancelacion | P0 |
| NTF-023 | Audiencias | Acta disponible | Implementada | Parcial | In-app, WebSocket | Se implementa | Se mezcla hoy con otras alertas de audiencia | P1 |
| NTF-024 | Audiencias | Audiencia validada y concluida | Implementada | Parcial | In-app, WebSocket | Se implementa | No tiene preferencia especifica | P1 |
| NTF-029 | Tareas | Nueva tarea o recordatorio personal | No implementada | No | N/A | Se analiza despues | El modulo existe, pero no se definio si debe notificar activamente | P2 |
| NTF-030 | Tareas | Tarea por vencer | No implementada | No | N/A | Se analiza despues | Requiere definicion funcional y scheduler | P2 |
| NTF-031 | Tareas | Tarea vencida | No implementada | No | N/A | Se analiza despues | Requiere definicion funcional y scheduler | P2 |
| NTF-032 | Seguridad | Contrasena por vencer | Implementada | No | In-app, WebSocket | Se implementa | Debe quedar catalogada como alerta obligatoria | P0 |
| NTF-033 | Seguridad | Cambio de contrasena | No implementada | No | N/A | Se analiza despues | Puede servir como confirmacion de seguridad, pero no es prioritaria frente a acceso desde otro equipo | P2 |
| NTF-035 | Seguridad | Inicio de sesion desde otro equipo | No implementada | No | N/A | Se implementa | Caso de seguridad claro y de alto valor para el usuario | P0 |
| NTF-036 | Seguridad | Cambio de rol o permisos | No implementada | No | N/A | Se implementa | Debe avisar al usuario afectado cuando le modifiquen el rol | P1 |

## Fuera de alcance confirmado

Los siguientes puntos se retiran del backlog de notificaciones del sistema actual:

- `NTF-025`: Documento pendiente de revision
- `NTF-026`: Documento rechazado u observado
- `NTF-027`: Documento pendiente de firma
- `NTF-028`: Documento firmado
- `NTF-034`: Cambio de correo o telefono
- `NTF-037`: Cuenta bloqueada o reactivada
- `NTF-038`: Centro de alertas y campana como issue funcional
- `NTF-040`: Canales externos correo, SMS o WhatsApp

## KPIs o datos que deben evaluarse para dashboard

Estos puntos no deben tratarse como notificaciones push en esta etapa:

- `KPI-003`: Cambios relevantes del expediente
- `KPI-006`: Expedientes sin actividad

## Problemas estructurales que si deben entrar al backlog

| ID | Problema | Impacto | Prioridad |
|---|---|---|---|
| ARC-001 | Clasificacion por titulo en vez de evento formal | Genera falsos positivos, falsos negativos y control inconsistente | P0 |
| ARC-002 | Preferencias reducidas a 4 flags | No cubre el universo real de alertas | P0 |
| ARC-003 | Notificaciones a superiores no respetan preferencias propias | Se envian alertas aunque el destinatario las desactive | P0 |
| ARC-004 | No hay catalogo de alertas obligatorias | Riesgo funcional y de seguridad | P0 |
| ARC-005 | No hay versionado visible del trigger SQL realtime | Riesgo operativo y de despliegue | P1 |

## Backlog Inicial Depurado: Top 5 Issues

### ISSUE-01

- Titulo: Normalizar el modelo de eventos de notificacion
- Prioridad: P0
- Tipo: Arquitectura / Backend / Datos
- Problema: El sistema decide preferencias con base en `tipo` y fragmentos del `titulo`, lo que rompe el control fino y complica la auditoria.
- Perspectiva aprobada:
  - no se trata solo de guardar historico;
  - se requiere un catalogo formal de tipos de notificacion;
  - y la tabla actual de notificaciones debe mantenerse como historico auditable.
- Alcance:
  - Crear catalogo tecnico de tipos de notificacion
  - Separar `tipo visual` de `evento funcional`
  - Persistir el evento en cada notificacion enviada
  - Preparar el modelo para reglas como obligatoriedad, prioridad y categoria
- Dependencias: Ninguna
- Criterios de aceptacion:
  - Cada notificacion nueva guarda un `evento` formal
  - No se usa `titulo.contains(...)` para resolver preferencias
  - El catalogo y el historico quedan conceptualmente separados

### ISSUE-02

- Titulo: Redisenar preferencias de notificacion por evento
- Prioridad: P0
- Tipo: Backend / Frontend / Datos
- Problema: `Mi Cuenta` solo maneja 4 switches y no representa el comportamiento real que el sistema ya envia ni el que se agregara.
- Alcance:
  - Sustituir la preferencia actual por un modelo basado en eventos del catalogo
  - Permitir distinguir entre alerta obligatoria y configurable
  - Actualizar la vista `/perfil` para grupos por categoria
  - Respetar preferencias tambien en notificaciones jerarquicas cuando aplique
- Dependencias: `ISSUE-01`
- Criterios de aceptacion:
  - El usuario puede controlar eventos de expedientes, terminos, audiencias y seguridad
  - Las alertas obligatorias aparecen activas y bloqueadas
  - La UI refleja el estado real guardado en base de datos

### ISSUE-03

- Titulo: Implementar alertas criticas faltantes de audiencia y termino
- Prioridad: P0
- Tipo: Backend
- Problema: Faltan eventos de alto impacto operativo y juridico.
- Alcance:
  - Notificar `audiencia reprogramada`
  - Notificar `audiencia cancelada`
  - Notificar `cambio de fecha limite de termino`
  - Notificar `termino vencido` como evento formal distinto a recordatorio
- Dependencias: `ISSUE-01`
- Criterios de aceptacion:
  - Cambiar fecha, hora o lugar de audiencia genera alerta
  - Cancelar audiencia genera alerta
  - Editar fecha de vencimiento de termino genera alerta
  - El vencimiento fuera de tiempo se registra como evento distinto a "vence pronto"

### ISSUE-04

- Titulo: Implementar alerta de documento agregado al expediente
- Prioridad: P1
- Tipo: Backend
- Problema: El gestor documental registra auditoria pero no avisa al responsable del expediente cuando se agrega un archivo.
- Alcance:
  - Notificar `documento agregado`
  - Definir destinatario minimo en funcion del responsable del expediente
  - Vincular la alerta con el expediente o documento correspondiente
- Dependencias: `ISSUE-01`, `ISSUE-02`
- Criterios de aceptacion:
  - Subir un documento al expediente genera una notificacion real
  - La alerta se puede consultar desde campana e historial
  - La preferencia por evento se respeta si aplica

### ISSUE-05

- Titulo: Implementar alertas obligatorias de seguridad
- Prioridad: P0
- Tipo: Backend / Seguridad
- Problema: Solo existe aviso por expiracion de contrasena; falta cubrir el caso mas sensible: acceso desde otro equipo.
- Alcance:
  - Mantener `contrasena por vencer` como alerta obligatoria
  - Implementar `inicio de sesion desde otro equipo`
  - Implementar `cambio de rol o permisos`
  - Catalogar estas alertas como obligatorias cuando corresponda
- Dependencias: `ISSUE-01`, `ISSUE-02`
- Criterios de aceptacion:
  - El sistema detecta e informa acceso desde otro equipo con criterio inicial definido
  - Los cambios de rol disparan alerta al usuario afectado
  - Las alertas obligatorias no pueden desactivarse desde `Mi Cuenta`

## Orden recomendado de ejecucion

1. `ISSUE-01`
2. `ISSUE-02`
3. `ISSUE-03`
4. `ISSUE-05`
5. `ISSUE-04`

## Pendientes para corte posterior

- Evaluar `KPI-003` y `KPI-006` en dashboard
- Definir si `NTF-029`, `NTF-030` y `NTF-031` deben ser alertas activas o solo gestion interna
- Versionar el trigger realtime asociado a notificaciones
