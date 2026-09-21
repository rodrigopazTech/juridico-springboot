# Auditoría técnica del módulo de Tareas y Notificaciones

Fecha de revisión: 21/09/2026  
Repositorio revisado: `juridico-springboot`  
Commit base observado: `6acc117dc55e72894eb76ae3e1522c27f3f8ea08`  
Alcance: código Java/Spring, Thymeleaf/JavaScript, esquema PostgreSQL, scripts de despliegue, historial Git, configuración, pruebas y logs locales.

> Nota de estado: las secciones 1–12 documentan la línea base encontrada durante la auditoría. Después del diagnóstico se implementaron las correcciones descritas en la sección 13; por tanto, las afirmaciones históricas “no existe edición” y “hay un job diario a las 07:30” describen el estado anterior al cambio.

## 1. Resumen ejecutivo

### Conclusión sobre la hora `11:41`

La hora visible en la esquina superior derecha del elemento del menú de la campana se alimenta de `n.hora` en el frontend. El endpoint `GET /notificaciones/api/recientes` construye ese valor formateando `Notificacion.notificarEn` como `HH:mm`. La entidad mapea esa propiedad a la columna `notificaciones.notificar_en`, y `NotificacionService` la asigna con `LocalDateTime.now()` en el instante en que crea la notificación.

Por tanto, en esa vista, `11:41` es la hora de creación/programación inmediata de la **notificación**, según el reloj y zona horaria por defecto de la JVM. No es la hora programada de la tarea (`recordatorios.hora_recordatorio = 11:50`), ni la creación de la tarea, ni la lectura, ni un timestamp generado por el navegador.

Hay una segunda ruta visual que debe distinguirse: el toast de tiempo real toma `payload.fecha`; el trigger PostgreSQL llena ese dato desde `notificaciones.created_at` y JavaScript lo formatea con `Date(...).toLocaleTimeString(...)`. En el código actual, `notificar_en` y `created_at` se asignan en dos llamadas consecutivas a `LocalDateTime.now()`, por lo que normalmente muestran el mismo minuto, aunque conceptualmente sean campos diferentes.

### Conclusión sobre el caso 21/09/2026, tarea 11:50

La regla implementada es avisar desde 15 minutos antes: `11:50 - 15 minutos = 11:35`. El scheduler se evalúa cada minuto, en el segundo cero. Si la tarea ya existía, estaba pendiente, el evento estaba habilitado y no había otra notificación `TAREA_RECORDATORIO` para la misma tarea, la creación debió ocurrir en la primera ejecución no anterior a las `11:35`.

Una creación a las `11:41` es posible si la tarea todavía no existía a las 11:35, la aplicación/scheduler no estaba ejecutándose, una ejecución se retrasó o reanudó, o hubo una condición operativa equivalente. El código no contiene una regla de “enviar a las 11:41” ni de “enviar a la hora exacta 11:50”.

No puede probarse en este checkout cuál de esas causas ocurrió: no hay servidor ni PostgreSQL activos, Docker Desktop no está iniciado, los logs locales más recientes son de julio y el texto exacto `Presentar Jurídico` no aparece en logs ni respaldos disponibles. Tampoco se adjuntó la captura mencionada; el único adjunto disponible fue el texto de la solicitud. En consecuencia:

- timestamp real almacenado: **No encontrado** en los artefactos locales;
- timestamp real enviado por WebSocket/Web Push: **No almacenado por el diseño actual**;
- timestamp real de aparición/render en cliente: **No almacenado por el diseño actual**;
- explicación exacta del retraso 11:41 → 11:51/11:52: **No demostrable sin el registro productivo y telemetría de entrega**.

El menú de la campana se carga al abrir la página y al abrir el dropdown. Por ello, ver a las 11:51 una fila cuya hora dice 11:41 demuestra que la notificación fue registrada a las 11:41; no demuestra que el sistema la haya entregado por primera vez a las 11:51.

### Hallazgos principales

1. Existen dos jobs para tareas: diario a las 07:30 e intradía cada minuto.
2. El job intradía avisa 15 minutos antes, pero carece de ventana superior; también avisa después del vencimiento durante el mismo día.
3. El job diario puede suprimir el aviso intradía porque ambos usan el mismo evento e ID de entidad.
4. La entrega en pantalla usa PostgreSQL `LISTEN/NOTIFY` + WebSocket STOMP/SockJS; la carga inicial usa una llamada REST única, no polling periódico.
5. También existe Web Push asíncrono con VAPID, condicionado a claves, permiso, suscripción y sesión activa.
6. No se encontró SignalR, Firebase, correo para recordatorios de tarea ni una cola/broker externo.
7. La prioridad de una tarea es parcialmente funcional: se persiste, se propaga y cambia estilos, pero no modifica fecha, frecuencia, orden ni decisión de envío.
8. No existe edición de tareas. Los DTOs `RecordatorioRequest/Response` están sin uso y son arquitectura abandonada/incompleta, no una implementación de edición.

## 2. Arquitectura encontrada

| Capa | Componente | Responsabilidad | Evidencia |
|---|---|---|---|
| Base de datos | `recordatorios` | Tarea personal: título, fecha, hora, prioridad, completado | `db_schema_actual.sql:1229-1239`; `Recordatorio.java:18-54` |
| Base de datos | `notificaciones` | Notificación materializada y estado de lectura | `db_schema_actual.sql:1040-1054`; `Notificacion.java:15-59` |
| Base de datos | `trg_notificaciones_realtime` | Publica cada INSERT de notificación | `deploy/database/02-realtime-notifications.sql:194-197` |
| Scheduler | `RecordatorioNotificacionScheduler` | Revisa tareas de hoy y crea notificaciones | `RecordatorioNotificacionScheduler.java:30-105` |
| Servicio | `NotificacionService` | Deduplica, aplica preferencias, guarda y lanza Web Push | `NotificacionService.java:53-105` |
| Realtime | `RealtimeNotificationService` | Hace LISTEN en PostgreSQL y reenvía por STOMP privado | `RealtimeNotificationService.java:58-100,127-180` |
| API | `GET /notificaciones/api/recientes` | Devuelve hasta cinco no leídas y formatea fecha/hora | `NotificacionesController.java:411-454` |
| WebSocket | `/ws-realtime`, `/user/queue/alertas` | Canal reactivo de la UI | `WebSocketConfig.java:26-41`; `navbar.html:489-517` |
| Frontend | `navbar.html` | Dropdown y toast | `navbar.html:218-338,489-576` |
| Web Push | `WebPushService`, `sw.js` | Notificación del sistema/navegador | `WebPushService.java:69-123`; `sw.js:1-45` |

No hay microservicios ni cola durable entre el scheduler y la tabla. Todo reside en la misma aplicación Spring Boot; el broker STOMP es el broker simple en memoria de Spring.

## 3. Flujo de datos completo

### Creación de tarea

1. El usuario envía `POST /tareas/guardar` desde el formulario Thymeleaf.
2. Spring enlaza directamente el formulario a la entidad `Recordatorio`.
3. El controller asigna usuario, `createdAt = LocalDateTime.now()` y `completado = false`.
4. La prioridad se busca por `prioridadId` y se asigna si existe.
5. JPA inserta en `recordatorios`.

Evidencia: `modal-recordatorio.html:9-69`; `TareasController.java:53-73`; tablas `recordatorios` y `cat_prioridad`.

### Generación de la notificación

1. A las 07:30, el job diario consulta todas las tareas pendientes del día y crea un aviso “Tarea para hoy”.
2. Cada minuto, el job intradía vuelve a consultar todas las tareas pendientes del día.
3. Calcula `limiteAviso = horaRecordatorio.minusMinutes(15)`.
4. Si `ahora >= limiteAviso`, busca cualquier notificación con evento `TAREA_RECORDATORIO` y `entidad_id = recordatorio.id`.
5. Si no existe, llama a `NotificacionService.crearNotificacion(...)`.
6. El servicio consulta la preferencia del usuario, crea `notificar_en` y `created_at` con la hora actual, inserta la fila y lanza Web Push asíncrono.

Evidencia: `RecordatorioNotificacionScheduler.java:30-105`; `NotificacionService.java:72-105`; `NotificacionRepository.java:15-19`.

Consulta JPA derivada para seleccionar candidatas:

```sql
SELECT *
FROM recordatorios
WHERE fecha_recordatorio = :hoy
  AND completado = false;
```

Consulta conceptual de deduplicación intradía:

```sql
SELECT EXISTS (
  SELECT 1
  FROM notificaciones
  WHERE evento = 'TAREA_RECORDATORIO'
    AND entidad_id = CAST(:recordatorio_id AS varchar)
);
```

### Entrega por WebSocket

1. El `INSERT` activa `trg_notificaciones_realtime`.
2. `fn_notify_realtime()` construye JSON. Para notificaciones, `fecha = COALESCE(created_at, CURRENT_TIMESTAMP)`.
3. PostgreSQL publica con `pg_notify('canal_notificaciones_realtime', payload)`.
4. `RealtimeNotificationService` sondea la conexión dedicada cada 500 ms, recibe el evento y ejecuta `convertAndSendToUser(email, "/queue/alertas", json)`.
5. El navegador está suscrito a `/user/queue/alertas`, crea el toast y formatea `payload.fecha` con la zona local del cliente.

Evidencia: `02-realtime-notifications.sql:34-46,182,194-197`; `RealtimeNotificationService.java:72-100,127-141`; `navbar.html:489-517`.

### Carga REST/dropdown

1. Al cargar la página se ejecuta una sola llamada a `GET /notificaciones/api/recientes`.
2. Esa llamada inicial actualiza el badge y registra IDs existentes; deliberadamente no muestra toasts antiguos.
3. Al abrir la campana se vuelve a consultar el endpoint y se renderiza el dropdown.
4. La hora alineada a la derecha proviene literalmente de `n.hora`.

Evidencia: `navbar.html:190-199,218-267,520-550,572-576`.

## 4. Origen de la hora mostrada

### Dropdown en la esquina superior derecha

| Pregunta | Respuesta probada |
|---|---|
| Campo frontend | `n.hora` |
| Transformación backend | `n.getNotificarEn().format(DateTimeFormatter.ofPattern("HH:mm"))` |
| Campo Java | `Notificacion.notificarEn` |
| Columna SQL | `notificaciones.notificar_en` (`timestamp without time zone`) |
| Servicio generador | `NotificacionService.crearNotificacion` |
| Asignación | `.notificarEn(LocalDateTime.now())` |
| Endpoint | `GET /notificaciones/api/recientes` |
| Formato | Servidor, patrón `HH:mm` |
| Naturaleza | Hora en que el backend construyó la notificación |

Evidencia: `Notificacion.java:49-50`; `NotificacionService.java:86-99`; `NotificacionesController.java:431-445`; `navbar.html:249-254`.

### Toast de tiempo real dentro de la aplicación

| Pregunta | Respuesta probada |
|---|---|
| Campo recibido | `payload.fecha` |
| Origen SQL | `COALESCE(notificaciones.created_at, CURRENT_TIMESTAMP)` |
| Campo Java usual | `Notificacion.createdAt` |
| Transformación | `new Date(payload.fecha).toLocaleTimeString(...)` |
| Naturaleza | Hora de creación de la fila, reinterpretada/formateada por el cliente |

Evidencia: `02-realtime-notifications.sql:34-46`; `NotificacionService.java:95-98`; `navbar.html:497-505,310-314`.

### Clasificación de orígenes solicitada

- Fecha de creación de la notificación: **Sí**, semánticamente. Dropdown: `notificar_en`; WebSocket: `created_at`.
- Fecha de creación de la tarea: **No** (`recordatorios.created_at` no se copia).
- Fecha/hora programada: **No**; sólo aparece dentro del texto del mensaje.
- Fecha de envío: **No existe un campo persistido**. `notificar_en` tiene nombre de programación, pero se asigna en creación y no prueba entrega.
- Fecha de lectura: **No**; es `fecha_leida`, asignada al abrir `/notificaciones/leer/{id}`.
- Timestamp del servidor: **Sí**; `LocalDateTime.now()` de la JVM.
- Timestamp del cliente: **No como origen**; el cliente sólo formatea `created_at` en la ruta WebSocket.
- Otro origen: para Web Push nativo, el service worker no establece `timestamp`; cualquier hora decorativa del sistema operativo sería controlada por navegador/SO, no por el payload de SIAJ.

## 5. Lógica de envío de notificaciones

### Jobs y evento disparador

| Proceso | Cron | Acción |
|---|---|---|
| `notificarRecordatoriosHoy` | `0 30 7 * * ?` | A las 07:30 crea un aviso para cada tarea pendiente del día |
| `notificarRecordatoriosIntraday` | `0 * * * * ?` | Cada minuto, en segundo 0, evalúa el umbral de 15 minutos |

Scheduling está habilitado por `@EnableScheduling` en `SistemaJuridicoApplication.java:11`. No se encontró `TaskScheduler`, pool, `zone` en `@Scheduled` ni `spring.task.scheduling.*`; usa la zona horaria por defecto de la JVM y no hay evidencia en configuración de cuál es en producción.

### Cuándo se crea, envía y aparece

- **Crea:** durante la transacción del job, cuando pasa la regla y preferencias.
- **Persiste:** antes de invocar Web Push (`save` en `NotificacionService.java:99`).
- **WebSocket:** el trigger se ejecuta después del INSERT; el evento se hace visible tras commit y el listener lo recoge en ciclos de aproximadamente 500 ms.
- **Web Push:** se invoca con `@Async`; el tiempo de respuesta del proveedor no se persiste.
- **Aparece en la aplicación abierta:** al recibir el STOMP, sujeto a conexión, carga de SockJS/STOMP, sesión y ejecución del navegador.
- **Aparece al abrir la campana:** al completar el fetch REST, aunque la notificación sea anterior.

### Tolerancias y retrasos

- Cadencia nominal: un minuto; retraso normal del job respecto del límite: de 0 a menos de 60 segundos, más carga/pausas del proceso.
- Listener PostgreSQL: sondeo de 500 ms, más commit y red.
- Reconexión STOMP del navegador: espera fija de 5 s.
- Reconexión del listener PostgreSQL: backoff exponencial hasta 30 s.
- No existe SLA, margen de tolerancia explícito, reintento durable, confirmación de recepción ni timestamp de entrega.
- `LISTEN/NOTIFY` no constituye una cola durable en este diseño. El estado sí queda en `notificaciones`, recuperable por REST, pero el toast realtime perdido no se reproduce: la carga inicial marca notificaciones existentes como vistas para efectos de toast.

### Tecnologías solicitadas

| Tecnología | Resultado |
|---|---|
| Polling | Parcial: listener DB cada 500 ms; REST sólo inicial/al abrir dropdown. No hay `setInterval` de notificaciones en navbar |
| Push en aplicación | Sí, WebSocket STOMP/SockJS |
| WebSocket | Sí, endpoint `/ws-realtime` |
| SignalR | No encontrado |
| Firebase | No encontrado |
| Web Push | Sí, VAPID + Service Worker; opcional por configuración/permiso |
| Correo para tareas | No encontrado |
| Cola externa (Rabbit/Kafka/JMS/SQS) | No encontrada |
| Broker | Broker simple STOMP en memoria, no durable |
| Trigger DB | Sí, `AFTER INSERT ON notificaciones` |

### Caso 21/09/2026 11:50

Cálculo implementado:

```text
hora programada       11:50
- anticipación fija   00:15
= límite de aviso     11:35
```

El comportamiento observado no coincide con “notificar exactamente a las 11:50”; esa nunca fue la regla implementada. Un registro mostrado como `11:41` indica creación a ese minuto. La visualización a las 11:51/11:52 puede ser simplemente la apertura del dropdown o recuperación posterior de una notificación no leída. Para afirmar por qué el job no creó a las 11:35 hacen falta los datos y logs productivos.

Consulta forense exacta para ejecutar en la base implicada:

```sql
SELECT
    r.id                              AS tarea_id,
    r.titulo,
    r.created_at                      AS tarea_creada_en,
    r.updated_at                      AS tarea_actualizada_en,
    r.fecha_recordatorio,
    r.hora_recordatorio,
    (r.fecha_recordatorio + r.hora_recordatorio) AS tarea_programada_en,
    r.completado,
    p.nombre                          AS prioridad,
    n.id                              AS notificacion_id,
    n.evento,
    n.titulo                          AS notificacion_titulo,
    n.created_at                      AS notificacion_creada_en,
    n.notificar_en                    AS notificacion_mostrada_en_api,
    n.fecha_leida,
    n.leida
FROM recordatorios r
LEFT JOIN cat_prioridad p ON p.id = r.prioridad_id
LEFT JOIN notificaciones n
       ON n.entidad_tipo = 'RECORDATORIO'
      AND n.entidad_id = r.id::text
      AND n.evento = 'TAREA_RECORDATORIO'
WHERE r.fecha_recordatorio = DATE '2026-09-21'
  AND r.hora_recordatorio >= TIME '11:49'
  AND r.hora_recordatorio <  TIME '11:51'
  AND r.titulo ILIKE '%Presentar Jurídico%'
ORDER BY n.created_at;
```

Consulta adicional para zona horaria:

```sql
SHOW TIMEZONE;
SELECT CURRENT_TIMESTAMP, LOCALTIMESTAMP;
```

Ni estas consultas pueden devolver “enviado” o “mostrado”: esas etapas no tienen tabla de auditoría.

## 6. Análisis de prioridad

### Persistencia y valores

- `recordatorios.prioridad_id` referencia `cat_prioridad.id`.
- El catálogo tiene `nombre`, `color`, `activo`, `created_at`, `updated_at`.
- La semilla crea `ALTA`, `MEDIA`, `BAJA` con colores rojo, amarillo y verde (`DataInitializer.java:73-85`).
- No hay `CHECK` SQL que restrinja esos tres nombres. El formulario usa `catPrioridadRepository.findAll()` y no filtra `activo`; técnicamente acepta cualquier fila presente en el catálogo.
- `RecordatorioRequest` declara valores `normal|urgent`, pero no se usa y contradice el catálogo real. Es deuda técnica.

### Colores y estilos encontrados

| Vista | ALTA | MEDIA | BAJA | Evidencia |
|---|---|---|---|---|
| Lista tareas | rojo | azul | azul | `lista-tareas-completa.html:61-65,138-142` |
| Tarjetas próximas | borde/icono rojo | oro | oro | `grid-recordatorios.html:15-22` |
| Dropdown/toast | rojo | amarillo | verde | `navbar.html:177-181` |
| Color de catálogo | `bg-red-100 text-red-800` | `bg-yellow-100 text-yellow-800` | `bg-green-100 text-green-800` | `DataInitializer.java:76-83` |

El campo `cat_prioridad.color` no se usa en las vistas de tareas revisadas; las clases están codificadas directamente. Esto explica la inconsistencia: BAJA aparece azul/oro en tareas y verde en notificaciones.

### Efecto funcional

- Apariencia visual: **Sí**.
- Payload/API de notificación: **Sí**, se copia la referencia y se devuelve el nombre.
- Envío o decisión de notificar: **No**.
- Frecuencia/anticipación: **No**, siempre 15 minutos para intradía.
- Vencimiento: **No**.
- Ordenamiento: **No**; tareas se ordenan sólo por fecha y ni siquiera por hora (`RecordatorioRepository.java:19,28`).
- Reportes/dashboards: el dashboard muestra el nombre, pero no calcula ni ordena por prioridad (`DashboardService.java:274-287`).
- Calendario: expone `prioridadId` para la representación/filtro general, no cambia la programación (`CalendarioService.java:344-363`).

### Impacto real de la prioridad

**Parcialmente funcional.** Persiste como relación de dominio, viaja a notificaciones/calendario/dashboard y controla estilos, pero no altera el comportamiento temporal, frecuencia, vencimiento, deduplicación ni orden de tareas. No es “solo visual” porque forma parte de datos y filtros/payloads; tampoco es “funcional crítico” porque el envío ocurre igual para ALTA, MEDIA y BAJA.

## 7. Estado actual de edición de tareas

### Resultado

**No existe funcionalidad de edición.**

- Endpoint de actualización: **No encontrado**.
- `PUT`/`PATCH` de tarea: **No encontrado**.
- Servicio backend específico: **No encontrado**; el controller usa el repositorio directamente.
- Botón/modal/formulario editar: **No encontrado**.
- Lógica frontend oculta: **No encontrada**; `tareas.js` sólo completa, elimina y filtra.
- Implementación parcial: sólo existen DTOs sin referencias (`RecordatorioRequest`, `RecordatorioResponse`), con tipos incluso incompatibles con el modelo actual.
- Eliminación histórica de edición: **No encontrada**. El historial de `TareasController` no contiene métodos de edición; los antiguos fragmentos eliminados de `recordatorios` eran placeholders vacíos.
- Componentes abandonados: los dos DTOs son candidatos claros. No deben reutilizarse sin corregir nombres (`fecha`/`hora` vs. `fechaRecordatorio`/`horaRecordatorio`), tipo de ID y prioridad.

## 8. Propuesta de edición

### Contrato recomendado

Para la UI Thymeleaf actual:

- `GET /tareas/{id}`: devuelve `RecordatorioResponse` sólo si pertenece al usuario autenticado.
- `PUT /api/tareas/{id}`: actualiza título, detalles, fecha, hora y `prioridadId`.
- Opcional para fallback HTML: `POST /tareas/{id}/editar` con Post/Redirect/Get.
- Cambiar completar y eliminar a mutaciones correctas: `PATCH /api/tareas/{id}/completado` y `DELETE /api/tareas/{id}`. Los `GET` actuales que modifican datos son un riesgo CSRF/semántico.

DTO de entrada propuesto:

```java
public record ActualizarTareaRequest(
    @NotBlank @Size(max = 200) String titulo,
    @Size(max = 5000) String detalles,
    @NotNull @FutureOrPresent LocalDate fechaRecordatorio,
    @NotNull LocalTime horaRecordatorio,
    @NotNull Integer prioridadId,
    @NotNull Long version
) {}
```

Reglas:

1. La tarea debe existir y pertenecer al usuario autenticado; responder 404 para evitar enumeración.
2. La prioridad debe existir y estar activa.
3. Fecha/hora combinadas deben estar en el futuro para tareas pendientes; definir explícitamente si se permiten atrasadas.
4. Título normalizado, no vacío y ≤ 200; detalles con límite explícito.
5. No editar tareas completadas salvo reapertura explícita.
6. Usar `@Version` para detectar edición concurrente y devolver 409.
7. Controller → `TareaService` transaccional → repositorio; no enlazar una entidad JPA directamente al request.

### Cambios de modelo y migración

Mínimo recomendado:

```sql
ALTER TABLE recordatorios
  ADD COLUMN version bigint NOT NULL DEFAULT 0,
  ADD COLUMN proximo_recordatorio_en timestamp without time zone,
  ADD COLUMN ultima_notificacion_en timestamp without time zone,
  ADD COLUMN estado_notificacion varchar(20) NOT NULL DEFAULT 'PENDIENTE',
  ADD CONSTRAINT ck_recordatorio_estado_notif
    CHECK (estado_notificacion IN ('PENDIENTE','ENVIADA','CANCELADA','FALLIDA'));

CREATE INDEX idx_recordatorios_pendientes_programacion
  ON recordatorios (proximo_recordatorio_en)
  WHERE completado = false AND estado_notificacion = 'PENDIENTE';
```

Para trazabilidad real, agregar una tabla de entregas en lugar de sobrecargar `notificaciones`:

```sql
CREATE TABLE notificacion_entregas (
  id bigserial PRIMARY KEY,
  notificacion_id integer NOT NULL REFERENCES notificaciones(id),
  canal varchar(20) NOT NULL,
  intentada_en timestamp without time zone NOT NULL,
  enviada_en timestamp without time zone,
  confirmada_en timestamp without time zone,
  estado varchar(20) NOT NULL,
  detalle_error text
);
```

### Recalcular al editar

- Cambio de fecha u hora: `proximo_recordatorio_en = fecha/hora - anticipación`; estado vuelve a `PENDIENTE` si aún es futuro. No borrar la notificación anterior: marcarla `CANCELADA/SUPERADA` mediante un nuevo estado o registrar una nueva versión de programación.
- Cambio de prioridad: actualizar tarea y cualquier notificación aún pendiente/no materializada; no reenviar por sí solo, salvo que se defina una política temporal distinta por prioridad.
- Cambio después de haber enviado: conservar el envío como auditoría y programar uno nuevo sólo si cambió el instante y la nueva política lo exige. Deduplicar por `(recordatorio_id, programacion_version, tipo_aviso)`, no sólo por entidad.
- Cambio a una fecha/hora dentro de la ventana de 15 minutos: crear de inmediato una nueva notificación una vez confirmado el commit.
- Cambio a pasado: rechazar (recomendado) o exigir confirmación explícita y marcar `VENCIDA_SIN_AVISO`.

### UI

1. Agregar botón lápiz en escritorio y móvil, siempre visible/focalizable, junto a eliminar.
2. Reutilizar un modal con modo crear/editar y título/botón dinámicos.
3. Precargar por `GET /tareas/{id}` y enviar `PUT` con CSRF.
4. Mostrar errores inline y conservar los datos del usuario.
5. Tras éxito, actualizar fila/tarjeta o recargar con mensaje accesible.
6. Mostrar resumen dinámico: “Se enviará el 21/09/2026 a las 11:35 para la tarea de las 11:50”.

### Plan paso a paso

1. Acordar política temporal y zona horaria (`America/Mexico_City` explícita).
2. Crear migración versionada; dejar de depender sólo de `ddl-auto=update`.
3. Corregir DTOs y agregar validadores de fecha/hora/prioridad.
4. Introducir `TareaService` y pruebas unitarias de propiedad, concurrencia y recálculo.
5. Implementar endpoints GET/PUT/PATCH/DELETE con respuestas 400/403/404/409 consistentes.
6. Reemplazar deduplicación por versión/instante programado.
7. Hacer que el scheduler consulte sólo vencimientos pendientes indexados.
8. Implementar modal/botón y pruebas de accesibilidad/responsive.
9. Agregar pruebas de integración con reloj inyectable (`Clock`) para 11:34, 11:35, 11:50 y después.
10. Agregar telemetría de entrega y pruebas WebSocket/Web Push.
11. Migrar datos existentes calculando próximo recordatorio y validar en staging.
12. Desplegar con métricas de atrasos, duplicados y fallos.

## 9. Configuración de notificaciones

### Lo que sí puede modificar el usuario

En Perfil → Notificaciones, puede habilitar/deshabilitar eventos configurables, incluido `TAREA_RECORDATORIO`. El formulario publica a `POST /perfil/preferencias/guardar`; el estado se almacena por usuario y evento en `preferencias_notificacion_evento.habilitada`.

Evidencia: `notificaciones.html:1-37`; `PerfilController.java:50-86`; `PreferenciaNotificacionEventoService.java:33-47,73-111`; `NotificacionEvento.java:126-133`.

También puede permitir o denegar Web Push en el navegador. Esa decisión vive en permisos del navegador y suscripciones `push_subscriptions`, no en las preferencias de anticipación.

### Lo que no puede modificar para tareas

- minutos de anticipación (fijo en 15);
- cadencia del scheduler (fija en un minuto);
- aviso diario de 07:30;
- canales por evento (WebSocket/Web Push/correo);
- número de repeticiones;
- zona horaria;
- horario silencioso;
- escalamiento por prioridad.

Existen configuraciones globales/gerencia/usuario/asunto y semáforos para `TERMINO` y `AUDIENCIA`, pero el enum `TipoEventoAlerta` sólo contiene esos dos valores. No aplican a tareas (`TipoEventoAlerta.java:3-6`; `ConfiguracionAlertasController.java:63-149`).

### Comportamiento actual por defecto

`TAREA_RECORDATORIO` es configurable y las preferencias base nuevas se crean habilitadas. Si faltara la fila por evento, el fallback legado devuelve `true` para tareas porque el switch no tiene caso específico y cae en `default` (`NotificacionEvento.java:188-204`).

## 10. Mejoras UX recomendadas

### Texto informativo correcto para la lógica actual

En creación/edición:

> La tarea está programada para el 21/09/2026 a las 11:50. El sistema intentará generar un recordatorio desde las 11:35 y revisa las tareas cada minuto.

No usar “se enviará a las 11:50”, porque contradice el código. “Intentará generar” es más exacto que “llegará”, dado que WebSocket/Web Push no tienen garantía ni acuse.

### Preferencias de notificaciones

Crear una sección específica “Tareas” con:

- activar recordatorios de tareas;
- anticipación: a la hora, 5, 15, 30, 60 minutos, un día;
- canales: dentro de SIAJ y Web Push; correo sólo si se implementa;
- horario silencioso;
- zona horaria visible;
- repetir si no se marca completada (desactivado por defecto y con límite).

Separar “guardar preferencias” del permiso del navegador y mostrar su estado: habilitado, bloqueado, no compatible o sin suscripción.

### Columnas/estados

Agregar a la tabla:

- Próximo recordatorio: timestamp calculado y zona;
- Estado: Pendiente, Enviada, Cancelada, Fallida, Sin permiso;
- Última notificación: timestamp de entrega persistido, o “No registrada” mientras no exista telemetría.

En la campana, etiquetar la hora como “Generada 11:41”, no dejar un `11:41` ambiguo. Para la tarea, mostrar por separado “Programada 11:50”.

## 11. Riesgos encontrados

| Severidad | Riesgo | Evidencia/impacto |
|---|---|---|
| Alta | El aviso “15 minutos antes” puede no ocurrir para tareas existentes a las 07:30 | El job diario crea el mismo evento/entidad; el intradía usa `existsByEventoAndEntidadId` y queda bloqueado |
| Alta | Envíos tardíos sin límite | Sólo se comprueba `ahora >= limiteAviso`; no `ahora <= horaRecordatorio` |
| Alta | No hay trazabilidad de envío/recepción/render | Sólo existen `created_at`, `notificar_en`, `fecha_leida` |
| Media | Mutaciones por GET | `/tareas/completar/{id}`, `/tareas/eliminar/{id}` |
| Media | Sin validación de tarea | Enlace directo a entidad; no `@Valid`; permite fecha pasada y textos fuera de política |
| Media | Zona horaria implícita y columnas sin zona | `LocalDateTime/LocalTime`, cron sin `zone`, SQL `timestamp without time zone` |
| Media | Evento realtime no durable | Broker simple + PostgreSQL NOTIFY; el fallback REST no reproduce toast antiguo |
| Media | Sin índice específico para la consulta del scheduler | El esquema no muestra índice parcial por fecha/completado |
| Media | Consulta cada minuto de todas las tareas del día + deduplicación N+1 | Una consulta inicial y un `exists` por candidata |
| Media | Dos fuentes de hora visual | REST usa `notificar_en`; WebSocket usa `created_at` |
| Baja | Prioridad visual inconsistente | BAJA es azul/oro en tareas y verde en navbar; `cat_prioridad.color` ignorado |
| Baja | Orden incompleto | “Próximos” ordena por fecha, no por hora/prioridad |
| Baja | DTOs incompatibles/sin uso | `normal|urgent` vs `ALTA|MEDIA|BAJA`; `Long` vs entidad `Integer` |
| Baja | Pruebas insuficientes | No se encontraron tests de `RecordatorioNotificacionScheduler` ni del caso 15 minutos; la prueba E2E llamada “scheduled” sólo navega a términos |

## 12. Evidencia técnica

### Archivos y líneas clave

- `src/main/java/com/juridico/sistema_juridico/Entity/usuario/Recordatorio.java:18-54` — entidad/tabla de tareas.
- `src/main/java/com/juridico/sistema_juridico/Entity/usuario/Notificacion.java:15-59` — entidad/tabla de notificaciones.
- `src/main/java/com/juridico/sistema_juridico/controller/TareasController.java:31-96` — listado, alta, completar y eliminar; no editar.
- `src/main/java/com/juridico/sistema_juridico/service/scheduler/RecordatorioNotificacionScheduler.java:30-105` — cron diario e intradía.
- `src/main/java/com/juridico/sistema_juridico/service/NotificacionService.java:53-105` — generación, timestamps, preferencias y Web Push.
- `src/main/java/com/juridico/sistema_juridico/controller/NotificacionesController.java:411-454` — API y formato `HH:mm`.
- `src/main/resources/templates/fragments/navbar.html:218-338,489-576` — dropdown, toast, REST y WebSocket.
- `deploy/database/02-realtime-notifications.sql:34-46,182,194-197` — payload, `pg_notify` y trigger.
- `src/main/java/com/juridico/sistema_juridico/service/RealtimeNotificationService.java:58-100,127-180` — listener y STOMP.
- `src/main/java/com/juridico/sistema_juridico/config/WebSocketConfig.java:26-41` — broker y endpoint.
- `src/main/java/com/juridico/sistema_juridico/service/WebPushService.java:69-123` — push asíncrono sin auditoría persistente.
- `src/main/resources/static/sw.js:1-45` — visualización Web Push.
- `src/main/java/com/juridico/sistema_juridico/service/PreferenciaNotificacionEventoService.java:33-47,73-141` — aplicación/persistencia de preferencias.
- `src/main/resources/templates/fragments/perfil/notificaciones.html:1-37` — UI de preferencias.
- `db_schema_actual.sql:620-627,1040-1054,1138-1157,1229-1239,2338` — tablas y trigger.

### Endpoints involucrados

| Método | Endpoint | Uso |
|---|---|---|
| GET | `/tareas` | Página/listado |
| POST | `/tareas/guardar` | Alta de tarea |
| GET | `/tareas/completar/{id}` | Marca completada (debe migrarse a PATCH/POST) |
| GET | `/tareas/eliminar/{id}` | Elimina (debe migrarse a DELETE/POST) |
| GET | `/notificaciones/api/recientes` | Datos de campana y hora mostrada |
| GET | `/notificaciones/leer/{id}` | Marca leída y navega a tarea |
| POST | `/perfil/preferencias/guardar` | Habilita/deshabilita eventos |
| GET/POST | `/api/webpush/public-key`, `/api/webpush/subscribe` | Suscripción Web Push |
| SockJS/STOMP | `/ws-realtime` → `/user/queue/alertas` | Entrega realtime en aplicación |

### Evidencia no encontrada

- Registro de DB de la tarea “Presentar Jurídico” del 21/09/2026.
- Logs del scheduler, INSERT, WebSocket o Web Push de esa fecha.
- Captura de pantalla mencionada en la solicitud.
- Timestamp persistido de intento, envío, recepción o render.
- SignalR, Firebase, cola externa o correo en el flujo de tareas.
- Endpoint o UI de edición de tareas.
- Configuración de anticipación/frecuencia/canales específica para tareas.
- Tests unitarios/integración del scheduler de recordatorios.

## Dictamen final

La hora `11:41` no representa la hora programada `11:50`. En el dropdown representa `notificaciones.notificar_en`, creado por el servidor al materializar la notificación; en el toast realtime equivalente representa `notificaciones.created_at`. El supuesto “retraso” hasta 11:51/11:52 no puede atribuirse al scheduler con la evidencia disponible, porque la UI puede mostrar a esa hora una notificación no leída creada diez minutos antes. La arquitectura actual no registra el momento real de entrega o render, por lo que cualquier afirmación más específica sería una suposición.

## 13. Correcciones implementadas después de la auditoría

- Edición de tareas mediante `POST /tareas/editar/{id}`, restringida al propietario y a tareas pendientes.
- Validación de título, prioridad activa y fecha/hora futura para alta y edición.
- Botón y modal de edición responsive con precarga de datos.
- Cálculo visible del aviso; cuando ya se generó, la UI indica “Ya notificado” y muestra la hora real.
- Retiro de las tarjetas duplicadas de “Recordatorios próximos”; la tabla queda como única fuente de consulta en la página.
- Cambio de la etiqueta ambigua “Próximo minuto” por estados comprensibles: “Se notificará en breve” y “Ya notificado”.
- Banner explicativo en `/tareas` y acceso directo a preferencias.
- Categoría independiente “Tareas” en las preferencias del perfil, con explicación de frecuencia y anticipación.
- Retiro del job diario de las 07:30 que podía suprimir el aviso intradía; queda una única regla, cada minuto desde 15 minutos antes.
- Revisión de programación (`recordatorio_revision`) para que cambiar fecha/hora habilite un nuevo aviso sin borrar el historial anterior.
- Archivo automático de notificaciones antiguas no leídas cuando una tarea se reprograma.
- Migración `deploy/database/05-task-reminder-revision.sql` y actualización del esquema de referencia.
- Pruebas unitarias de creación, reprogramación, prioridad, fechas pasadas y aislamiento por usuario.
