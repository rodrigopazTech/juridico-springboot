# Auditoría integral UX/UI, producto, arquitectura de información y trazabilidad — SIAJ

**Fecha:** 9 de septiembre de 2026  
**Alcance:** Expedientes, Términos, Audiencias, Catálogos, Concluidos, Trazabilidad y Semáforos  
**Tipo de evaluación:** inspección heurística, recorrido funcional en navegador, revisión de flujos, contraste con modelo de datos y código fuente  
**Perspectivas:** UX/UI, Product Management, LegalTech, arquitectura de información, UCD, carga cognitiva, heurísticas de Nielsen, JTBD y operación empresarial

## 1. Resumen ejecutivo

SIAJ tiene una base visual sólida: lenguaje institucional consistente, buena jerarquía en listados, navegación reconocible y una adaptación móvil superior a la esperada para un sistema administrativo. El problema principal no es estético. Está en la **coherencia del modelo funcional y de auditoría**: la interfaz presenta una experiencia razonablemente unificada, pero por debajo conviven fuentes, reglas y estados que pueden producir información incompleta o difícil de explicar en un contexto jurídico.

La conclusión de producto es clara: **no conviene rediseñar todo**. Conviene conservar el sistema visual y concentrar el siguiente ciclo en cuatro frentes:

1. establecer una fuente única y versionada para plazos y semáforos;
2. convertir “Concluidos” en un historial procesal confiable y localizable, con tratamiento explícito de registros sin expediente;
3. sustituir la trazabilidad parcial por eventos auditables, inmutables y transversales;
4. normalizar filtros, accesibilidad y estados de navegación.

### Valoración heurística

Escala de 1 a 5. No representa investigación con usuarios ni analítica de producción; es una valoración experta del estado inspeccionado.

| Dimensión | Valoración | Diagnóstico |
|---|---:|---|
| Consistencia visual | 4/5 | Sistema cromático, tarjetas, tablas y jerarquía generalmente coherentes. |
| Adaptación móvil | 4/5 | Listados pasan correctamente a tarjetas y los controles se apilan bien. |
| Claridad de navegación | 3/5 | Los módulos son reconocibles, pero “Agenda/Concluidos”, semáforos y usuarios dentro de catálogos generan ambigüedad. |
| Eficiencia de flujos | 3/5 | Búsqueda y filtros asíncronos ayudan, aunque hay controles redundantes, cargas innecesarias y falta de vistas guardadas. |
| Coherencia funcional | 2/5 | Estados, fechas de conclusión, búsquedas prometidas y fuentes históricas no siempre coinciden. |
| Trazabilidad jurídica | 2/5 | Hay periodos, actividades y cambios, pero no existe todavía una bitácora transversal e inmutable. |
| Accesibilidad | 2/5 | Varios controles interactivos no son semánticos, faltan nombres accesibles y manejo completo de foco. |
| Escalabilidad del modelo | 2/5 | Hay duplicidad de reglas y consultas/catálogos con alcance inconsistente. |

### Riesgos prioritarios

| Prioridad | Hallazgo | Impacto |
|---|---|---|
| P0 | La trazabilidad recibe actividades y cambios sin periodo, pero la vista no los representa. | El historial visible puede estar incompleto aunque los datos existan. |
| P0 | Términos y audiencias concluidos pueden eliminarse físicamente; además existen tablas históricas paralelas. | Pérdida o divergencia de evidencia operativa y jurídica. |
| P0 | Las reglas de semáforo están duplicadas y Audiencias aplica umbrales codificados directamente. | Un mismo plazo puede mostrar urgencias distintas según módulo o tipo. |
| P1 | Términos usa `updatedAt` como fecha de conclusión en el historial. | Una edición posterior altera la fecha y el orden histórico. |
| P1 | “Concluidos” promete búsqueda por abogado, pero las consultas no la contemplan. | Falsa expectativa y baja encontrabilidad. |
| P1 | Registros independientes, sin expediente o incompletos no se distinguen visualmente. | Aumenta el riesgo de duplicación, abandono y pérdida de contexto. |
| P1 | La pantalla de trazabilidad presenta fallos de recursos estáticos en una ruta profunda. | Menú/logo degradados y errores JavaScript reproducibles. |
| P1 | Una audiencia pasada no concluida se muestra gris. | Un atraso puede perder prominencia justo cuando requiere atención. |
| P2 | Etiquetas, botones de icono, calendarios y KPIs presentan barreras de teclado/lector de pantalla. | Menor inclusión, operabilidad y cumplimiento. |
| P2 | La solicitud persistente de permisos de notificación obstruye títulos y controles. | Interrupción repetida en tareas de consulta frecuentes. |

## 2. Metodología y límites

Se evaluaron las pantallas disponibles en la instancia local, en escritorio y viewport móvil, sin crear, modificar ni eliminar datos. La inspección se contrastó con controladores, entidades, repositorios, servicios, plantillas y JavaScript. Se aplicaron heurísticas de Nielsen, criterios de accesibilidad y convenciones modernas de interfaz; como referencia complementaria se usaron las [Web Interface Guidelines](https://github.com/vercel-labs/web-interface-guidelines).

La auditoría no incluyó entrevistas, observación contextual, analítica de producción, pruebas de carga, revisión jurídica normativa ni validación con cada rol. Por ello, las recomendaciones sobre frecuencia de uso deben validarse después con analítica y cinco a ocho sesiones por perfil prioritario.

Perfiles considerados:

- Abogado: consultar sus asuntos, registrar actuaciones, atender plazos y documentar resultados.
- Jefatura/Gerencia: asignar carga, identificar riesgo, supervisar atrasos y asegurar integridad.
- Dirección: obtener panorama consolidado, excepciones y evidencia de cumplimiento.
- Administración funcional: gobernar catálogos, reglas, usuarios y vigencias sin romper históricos.
- Auditoría/Control: reconstruir qué ocurrió, cuándo, quién lo hizo y con qué dato previo.

## 3. Jobs to Be Done principales

1. **Cuando inicio mi jornada**, quiero identificar en segundos qué requiere atención hoy y por qué, para priorizar sin recorrer múltiples módulos.
2. **Cuando cambia un asunto**, quiero registrar el cambio una sola vez y que se refleje en expediente, término, audiencia e historial, para evitar contradicciones.
3. **Cuando concluyo una actuación**, quiero conservar resultado, documento, responsable y fecha real, para que el antecedente sea defendible.
4. **Cuando un registro no pertenece todavía a un expediente**, quiero localizarlo y vincularlo después sin perder su historia, para no duplicarlo ni inventar expedientes.
5. **Cuando audito un expediente**, quiero reconstruir una línea temporal completa y exportable, para conocer datos anteriores, autor, causa y módulos afectados.
6. **Cuando administro reglas**, quiero saber su alcance y vigencia antes de publicarlas, para que un cambio no altere silenciosamente casos históricos.

## 4. Arquitectura de información recomendada

La navegación debe separar operación, historial y configuración:

```text
Inicio
├─ Expedientes
│  ├─ Todos / Mis expedientes / Riesgo / Sin responsable
│  └─ Detalle
│     ├─ Resumen
│     ├─ Términos
│     ├─ Audiencias
│     ├─ Documentos
│     └─ Trazabilidad
├─ Términos
│  ├─ Hoy / Vencidos / Próximos / Todos
│  └─ Calendario
├─ Audiencias
│  ├─ Hoy / Próximas / Pendientes de acta / Todas
│  └─ Calendario
├─ Historial procesal
│  ├─ Todos
│  ├─ Términos
│  ├─ Audiencias
│  └─ Sin expediente / Sin clasificar
└─ Administración
   ├─ Catálogos maestros
   ├─ Usuarios y permisos
   ├─ Calendarios jurídicos
   └─ Políticas de plazos y alertas
```

Decisiones clave:

- Renombrar **Concluidos** como **Historial procesal**. “Concluido” es un estado; “historial” expresa la tarea de consulta y auditoría.
- Mantener Términos y Audiencias como módulos operativos separados, pero compartir patrón visual, filtros, semántica de estados y motor de plazos.
- Retirar **Semáforos procesales** y **Usuarios/Abogados** de Catálogos. El primero es una política; el segundo es administración de identidad y permisos.
- Dividir **Sedes/Estados**: Entidad federativa es referencia territorial; sede, sala o lugar de audiencia es una entidad operativa con dirección y competencia.
- Permitir acceso al historial tanto globalmente como desde cada expediente, sin copiar información.

## 5. Auditoría por módulo

### 5.1 Expedientes

**Fortalezas**

- La tabla prioriza correctamente número, partes, abogado y estado.
- Los filtros principales son visibles y la vista móvil transforma el listado en tarjetas legibles.
- La búsqueda servidor y la paginación permiten crecer más que un filtrado puramente local.
- El expediente funciona como agregado central y el detalle reúne información procesal relevante.

**Problemas**

- No se expone el riesgo temporal como criterio principal. Un expediente puede ser crítico por un término o audiencia aunque su etapa parezca normal.
- La búsqueda implementada contempla más campos que los comunicados en el placeholder; el sistema subexplica su capacidad.
- Existen dos manejadores de búsqueda en vivo sobre el mismo campo, con debounces y actualizaciones diferentes. Esto puede provocar solicitudes duplicadas, carreras y estados de URL inconsistentes (`static/js/main-EXP.js:639-678` y `static/js/main-EXP.js:724-806`).
- El indicador de “nuevos” usa el total como valor provisional en el controlador; no debe mostrarse como métrica real hasta definir ventana temporal y evento de alta.

**Recomendación**

- Añadir vistas rápidas: **Vencimientos hoy**, **Vencidos**, **Próximos 7 días**, **Sin responsable** y **Sin actividad reciente**.
- Mostrar en cada fila el siguiente hito procesal y su explicación: “Término vence en 2 días hábiles”, no sólo un color.
- Consolidar una sola capa de búsqueda/filtros con cancelación de solicitudes, estado en URL y restauración al volver atrás.
- Reservar las métricas de cabecera para indicadores accionables y definidos, no conteos provisionales.

### 5.2 Términos

**Fortalezas**

- La combinación de calendario, lista, pipeline de estados y leyenda reduce el cambio de contexto.
- El flujo distingue avance operativo —proyectista, revisión, gerencia, dirección, liberado, presentado y concluido—.
- La interfaz soporta plazo fijo/dinámico y contexto con o sin expediente.

**Problemas**

- Se mezclan tres conceptos: estado de trabajo, cumplimiento y urgencia. “Presentado” puede dejar de aparecer en algunas consultas activas, pero tampoco entrar a Concluidos.
- El historial filtra sólo `CONCLUIDO`, aunque algunos textos de interfaz hablan de “presentados o concluidos”.
- No existe `completedAt` inmutable; el historial usa `updatedAt`, por lo que una corrección posterior cambia la supuesta fecha de conclusión (`repository/procesal/TerminoRepository.java:172-201`).
- El término conserva umbrales propios del tipo y también recibe otra configuración de semáforo, con una precedencia implícita que el usuario no puede entender.
- La leyenda de semáforo en Términos habla de alertas preventivas de expedientes, lo que diluye el objeto real.

**Recomendación**

- Separar estado de flujo (`BORRADOR`, `EN_REVISION`, etc.) de estado temporal (`EN_TIEMPO`, `PROXIMO`, `VENCIDO`) y de resultado (`PRESENTADO`, `CANCELADO`, `CONCLUIDO`).
- Definir si “presentado” cierra el plazo o si exige un cierre posterior; reflejar la misma decisión en tablero, consultas e historial.
- Registrar `completedAt`, `completedBy`, resultado, motivo y documento probatorio.
- Ofrecer accesos rápidos **Hoy**, **Vencidos**, **3/7 días**, **Sin expediente**, **Sin documento/acuse**.

### 5.3 Audiencias

**Fortalezas**

- Los KPIs facilitan segmentar Pendientes, Con acta y Concluidas.
- El minicalendario y la vista tabular responden a necesidades temporales diferentes.
- La entidad admite audiencia vinculada a expediente o asunto independiente.

**Problemas**

- El color se calcula con valores codificados de 1 y 3 días y no con la política ni con los umbrales de Tipo de audiencia (`Entity/procesal/Audiencia.java:93-101`).
- Cualquier audiencia pasada se vuelve gris, incluso si sigue pendiente. Lo vencido no atendido necesita mayor, no menor, prominencia.
- Las tarjetas KPI son `div` con `onclick`, sin semántica de botón ni operación de teclado (`templates/views/audiencias/index.html:91-147`).
- Los días del calendario también se generan como `div` clicables, sin nombre accesible ni manejo de teclado (`static/js/audiencias.js:1390-1419` y `1433-1464`).
- Para poblar algunos selectores/modal se cargan colecciones completas y se filtran en Java; este patrón no escalará con miles de registros.

**Recomendación**

- Introducir un estado visible **Pasada sin concluir** o **Requiere cierre/acta**, siempre con texto e icono además de color.
- Convertir KPI y días en botones reales con foco visible, `aria-pressed`/selección y etiqueta completa de fecha.
- Añadir filtros de modalidad, sede/sala, rango exacto, existencia de acta y vinculación.
- Sustituir listas completas por autocompletado servidor con límite, búsqueda y alcance por rol.

### 5.4 Concluidos / Historial procesal

**Diagnóstico de arquitectura**

La separación actual en pestañas es comprensible porque Términos y Audiencias poseen campos diferentes. Debe conservarse, pero dentro de una sola experiencia de **Historial procesal**, con una vista inicial “Todos” si el volumen y la consulta lo justifican. No se recomienda crear dos módulos históricos aislados.

El sistema ya modela contextos independientes (`TerminoContexto` y `AudienciaContexto`) y permite que el expediente sea nulo. Esto es correcto: no debe forzarse un expediente artificial. Sin embargo, falta hacer visible la diferencia entre:

- **Vinculado:** pertenece a un expediente.
- **Asunto independiente:** no debe pertenecer a uno y conserva un contexto jurídico suficiente.
- **Sin clasificar:** carece de vinculación o contexto completo y requiere saneamiento.

No se encontró una restricción de base de datos que obligue a tener exactamente expediente o contexto, pero no ambos. Debe aplicarse una regla XOR en base de datos y servicio.

**Problemas concretos**

- La pantalla consulta y pagina Audiencias y Términos en cada petición aunque sólo una pestaña sea visible (`controller/AgendaController.java:56-80`).
- Ambas pestañas comparten el parámetro `page`; al cambiar de pestaña puede heredarse una página inexistente o incorrecta.
- El cambio de pestaña alterna contenido ya cargado; no reinicia ni consulta la paginación específica (`static/js/agenda.js:5-39`).
- El placeholder promete buscar expediente, partes, actuación o abogado, pero los repositorios históricos no buscan por abogado (`templates/fragments/agenda/barra-filtros.html:22-29`; `AudienciaRepository.java:78-109`; `TerminoRepository.java:172-201`).
- Existen `TerminoPresentado` y `AudienciaDesahogada`, pero la pantalla histórica consulta las entidades operativas. Mantener ambas fuentes abre la puerta a duplicidad o divergencia.
- Los flujos de eliminación borran físicamente registros y, en Términos, eliminan además la proyección `TerminoPresentado` antes del término (`controller/TerminosController.java:1159-1165`).

**Diseño recomendado**

- Cabecera con conteos: **Todos**, **Términos**, **Audiencias**, **Sin expediente** y **Sin clasificar**.
- Columna/chip siempre visible **Vinculación** y otra de **Origen**.
- Para “Sin clasificar”: acciones **Vincular a expediente**, **Crear expediente desde este registro** o **Mantener independiente**, exigiendo motivo.
- Búsqueda real por número, partes, actuación/asunto, abogado, ID, origen y referencia externa.
- Fecha rotulada según pestaña: “Fecha de audiencia” o “Fecha de conclusión”; nunca una fecha genérica.
- Paginación independiente por tipo, o cursor único si se implementa una lista cronológica combinada.
- Una sola fuente histórica: entidad operativa con estado y eventos inmutables, o proyección derivada de eventos; no dos tablas editables que representen el mismo hecho.
- Sustituir eliminación por **anulación lógica** con motivo, actor, fecha y payload preservado. Los históricos no deben borrarse desde la UI operativa.

### 5.5 Catálogos

**Fortalezas**

- La selección por Gerencia y la etiqueta “Gobierno de datos” explican razonablemente el alcance administrativo.
- El patrón lateral facilita recorrer catálogos sin perder contexto.
- Se muestran conteos y se diferencia visualmente la sección de reglas.

**Problemas**

- Conviven catálogos maestros, usuarios y políticas de negocio en un mismo nivel.
- Tipos de término y audiencia permiten relación con Gerencia, pero sus nombres son globalmente únicos. La UI sugiere alcance local que el modelo no permite repetir.
- `listaTiposTermino` se carga sin el mismo filtro de Gerencia aplicado a otros catálogos (`controller/CatalogosController.java:41-161`).
- Faltan vigencia, versión, responsable, dependencias y alcance efectivo de cada valor.
- Eliminar un valor ya utilizado puede romper lectura histórica o cambiar significado; la interfaz debe privilegiar desactivación.

**Taxonomía propuesta**

| Grupo | Elementos | Regla de gobierno |
|---|---|---|
| Organización | Gerencias, usuarios, roles, abogados | Administración separada; permisos y alcance explícitos. |
| Jurídico maestro | Materias, etapas, tipos de expediente, tipos de término, tipos de audiencia, órganos | Activo/inactivo, alcance global/local, vigencia y referencias. |
| Territorial | Entidades federativas, sedes, salas/lugares | Entidad global; sede con dirección, zona horaria y competencia. |
| Calendarios | Días inhábiles, suspensiones, calendarios por jurisdicción | Versionado y fecha efectiva; nunca reescritura retroactiva silenciosa. |
| Resultados | Motivos de conclusión, cancelación, diferimiento y resultados | Catálogo controlado para reportes y auditoría. |
| Políticas | Reglas de cómputo y alertas | Fuera de Catálogos; simulación, publicación y versionado. |

No todo enum necesita catálogo. Sólo deben ser configurables los valores que cambian por operación, requieren gobierno o alimentan reportes.

### 5.6 Trazabilidad dentro del expediente

**Fortalezas**

- Agrupar por periodos de etapa responde a una pregunta jurídica útil: cuánto permaneció el expediente en cada etapa.
- Se distinguen actividades y cambios de datos; la exportación ya considera periodos, actividades y bitácora.
- El acceso se valida contra el expediente.

**Falla crítica de completitud**

El controlador incorpora `actividadesSinPeriodo` y `cambiosSinPeriodo` al modelo (`ExpedientesController.java:746-768`), pero la plantilla sólo itera `bloquesTrazabilidad` y nunca presenta esas colecciones (`templates/views/expedientes/trazabilidad.html:79-173`). Por tanto, eventos heredados o sin relación con un periodo pueden existir y no ser visibles. La exportación sí recibe las colecciones completas, por lo que pantalla y archivo pueden discrepar.

**Otras brechas**

- Los cambios estructurados cubren principalmente campos del expediente; ediciones de Términos y Audiencias generan eventos parciales o descripciones generales, no un antes/después consistente.
- Las eliminaciones físicas no dejan un evento de anulación reconstruible.
- La actividad resumen y cada cambio de campo pueden duplicar ruido sin un `correlationId` que los agrupe.
- La tabla de cambios muestra campo/anterior/nuevo, pero no actor y fecha por renglón.
- No hay búsqueda ni filtros por tipo, fecha, actor, fuente o etapa.
- Si un término/audiencia independiente se vincula después, falta una estrategia explícita para integrar su historia previa.
- La pantalla inspeccionada cargó recursos locales como redirecciones HTML en la ruta profunda; el navegador rechazó tres scripts por MIME y se degradaron logotipo/menú. También se carga Tailwind por CDN en una vista de producción.

**Arquitectura recomendada**

Crear un registro append-only `AuditEvent` como fuente canónica:

| Campo | Propósito |
|---|---|
| `eventId` | Identificador inmutable. |
| `aggregateType`, `aggregateId` | Expediente, término, audiencia, documento, regla o catálogo afectado. |
| `expedienteId` | Correlación padre cuando exista; puede agregarse al vincular sin reescribir eventos. |
| `eventType` | Tipo gobernado: creado, editado, vinculado, concluido, anulado, archivo agregado, etapa cambiada, etc. |
| `occurredAtUtc` | Fecha/hora inmutable en UTC; presentación en zona local. |
| `actorId`, `actorRole` | Autor y rol efectivos. |
| `sourceModule` | Origen funcional o integración. |
| `correlationId`, `causationId` | Agrupación de cambios de una misma operación y causa anterior. |
| `before`, `after` | Snapshot o diff estructurado y legible. |
| `reason` | Obligatorio en anulación, reversión y cambios sensibles. |
| `objectVersion` | Control de concurrencia y orden. |
| `policyVersion` | Regla de plazo/alerta aplicada en ese momento. |

La UI debe ofrecer dos perspectivas sin duplicar datos:

- **Cronología:** todos los eventos ordenados, con agrupación de una misma operación.
- **Etapas:** proyección por periodos, duración e hitos.

Los eventos sin periodo deben aparecer en un bloque “Antes del seguimiento por etapas / Sin etapa asociada”, nunca omitirse.

## 6. Semáforos y cómputo de plazos

### Diagnóstico

Actualmente hay tres lugares capaces de determinar color o umbral:

1. `ConfiguracionSemaforo`, con configuración por tipo de evento, nivel y Gerencia (`Entity/configuracion/ConfiguracionSemaforo.java:31-54`).
2. Campos de umbral en `TipoTermino` y `TipoAudiencia` (`TipoTermino.java:39-53`; `TipoAudiencia.java:34-44`).
3. Lógica directa en entidades: Término prioriza los campos del tipo sobre la configuración inyectada; Audiencia usa 1/3 días fijos (`Termino.java:161-179`; `Audiencia.java:93-101`).

Esto hace que “configurar” una regla no garantice que se aplique. Además, un semáforo no es un catálogo: es una **política derivada** de un plazo, una fecha base, un calendario y una estrategia de alerta.

### Decisión recomendada

- Mover Semáforos a **Administración → Políticas de plazos y alertas**.
- Separar el **cálculo del vencimiento** de la **presentación de alerta**.
- Usar una única resolución de política con precedencia visible: sistema → Gerencia → tipo → excepción del registro.
- Guardar en cada término/audiencia la versión de política y el vencimiento calculado. Un cambio futuro no debe alterar silenciosamente la historia.
- Permitir “simular” la regla con ejemplos antes de publicar y mostrar cantidad de registros afectados.

### Modelo de plazo

Una política debe definir al menos:

- evento/fecha base;
- cantidad y unidad;
- días hábiles o naturales;
- calendario/jurisdicción y zona horaria;
- inclusión o exclusión del día inicial;
- reglas de prórroga al siguiente día hábil;
- regla fija, calculada o captura manual justificada;
- umbrales de advertencia y escalamiento;
- vigencia, alcance y versión.

El color siempre debe ser derivado. Debe acompañarse de icono y texto: **Vencido hace 2 días**, **Vence hoy**, **3 días hábiles**, **Concluido**. Para Audiencias conviene hablar de proximidad y cierre pendiente; para Términos, de vencimiento.

## 7. Auditoría y rediseño de filtros

### Principios comunes

- Búsqueda y filtros deben compartir una sola fuente de estado y reflejarse en la URL.
- Mostrar chips de filtros activos y un contador; “Limpiar” debe restaurar también página y calendario.
- Usar presets para tareas frecuentes y panel secundario para filtros avanzados.
- Aplicar listas dependientes —Gerencia → Materia → Abogado— y autocompletado para grandes volúmenes.
- Conservar filtros al abrir un detalle y volver; el botón Atrás debe restaurar contenido, no sólo la URL.
- Anunciar carga, resultado y error con `aria-live`; cancelar solicitudes anteriores.
- Medir uso por filtro antes de retirar campos. La frecuencia real debe decidir la jerarquía final.

### Matriz propuesta

| Pantalla | Mantener visibles | Pasar a avanzados | Añadir | Corregir/retirar |
|---|---|---|---|---|
| Expedientes | Búsqueda, Materia, Etapa | Gerencia, Abogado, Prioridad | Próximo hito, riesgo/vencidos, sin responsable, tipo, órgano, última actividad | Unificar los dos manejadores de búsqueda. |
| Términos | Periodo, Estado, Abogado según rol | Prioridad, Gerencia, Materia | Vencido/próximo, tipo, rango de vencimiento, origen, sin expediente, documento/acuse | Separar estado de flujo de urgencia. |
| Audiencias | Periodo, Estado, Tipo, Abogado según rol | Gerencia, Materia | Modalidad, sede/sala, rango exacto, origen, sin expediente, con/sin acta | No convertir pasadas pendientes en gris/concluidas. |
| Historial | Rango, Tipo de registro, búsqueda | Gerencia, Materia, Abogado, documento | Vinculación, sin expediente, sin clasificar, resultado, concluyó por, origen | Búsqueda debe cumplir todos los campos prometidos; paginación separada. |
| Catálogos | Búsqueda del catálogo activo, Activo | Alcance, Gerencia | Vigencia, modificó por, dependencias, usados/no usados | No mostrar búsqueda en listas muy pequeñas; retirar Semáforos y Usuarios. |
| Trazabilidad | Tipo de evento, fecha | Actor, fuente, etapa | ID/referencia, cambios sensibles, anulaciones, sólo inconsistencias | No ocultar eventos sin etapa. |

### Observaciones por filtro actual

- **Gerencia:** útil para supervisión, redundante para roles ya restringidos; debe ocultarse o fijarse según permisos.
- **Materia:** frecuente y comprensible; conviene depender de Gerencia para evitar opciones imposibles.
- **Abogado:** esencial para jefaturas, innecesario para un abogado que sólo ve sus registros.
- **Prioridad:** útil, pero no sustituye el riesgo temporal. Debe ser secundaria frente a vencimiento.
- **Etapa/Estatus:** ambos son útiles, pero necesitan vocabulario coherente y valores legibles, no enums técnicos.
- **Periodo/Rango/Desde/Hasta:** no deben competir. Elegir preset actualiza fechas; editar fechas cambia el preset a “Personalizado”.
- **Documento:** valioso en historial como control de completitud, con nombres específicos “acta”, “acuse” o “evidencia” cuando aplique.

## 8. Accesibilidad, interacción y consistencia UI

### Hallazgos

- Las etiquetas visuales de filtros no están asociadas a sus controles mediante `for`/`id`; varios `select` no reciben un nombre accesible (`templates/fragments/agenda/barra-filtros.html:38-123`).
- Los placeholders se usan como principal explicación del buscador; no sustituyen una etiqueta persistente.
- Botones de icono dependen de `title` o del glifo, sin `aria-label` consistente.
- KPI y celdas de calendario clicables no son controles semánticos.
- Los modales inspeccionados abren/cierran cambiando visibilidad, pero no muestran una gestión completa de foco, Escape y retorno al disparador.
- Las fechas aparecen en ISO y `dd/MM/yyyy` según módulo; esto incrementa ambigüedad.
- Hay uso extendido de `transition-all`, lo que anima propiedades no previstas y dificulta controlar movimiento reducido.
- La invitación a activar notificaciones reaparece sobre el contenido y bloquea controles. Debe ser descartable, recordar la decisión y no interceptar la tarea primaria.

### Criterios mínimos de aceptación

- Toda acción operable por teclado y con foco visible.
- Todo control con nombre accesible; iconos decorativos ocultos del árbol accesible.
- Modales con `role="dialog"`, nombre, foco inicial, trampa de foco, Escape y retorno de foco.
- Mensajes asíncronos en región `aria-live` y estado de carga sin saltos de diseño.
- Color nunca como único indicador.
- Fechas con formato consistente, zona horaria cuando incluya hora y texto no ambiguo.
- Respeto a `prefers-reduced-motion`.

## 9. Matriz de pruebas de trazabilidad e integridad

| ID | Escenario | Resultado esperado |
|---|---|---|
| TR-01 | Crear expediente | Evento único con actor, UTC, ID, valores iniciales y versión. |
| TR-02 | Editar varios campos en una operación | Un grupo correlacionado, con antes/después por campo y motivo cuando aplique. |
| TR-03 | Cambiar etapa | Cierre del periodo anterior y apertura del nuevo dentro de la misma transacción. |
| TR-04 | Crear término desde expediente | Evento visible tanto en término como en trazabilidad del expediente, sin duplicar el hecho. |
| TR-05 | Editar fecha/tipo de término | Recalcula vencimiento, conserva política anterior y registra diff/regla nueva. |
| TR-06 | Presentar y concluir término | Fechas inmutables, usuario, resultado y documento; aparece una vez en historial. |
| TR-07 | Crear audiencia y cambiar fecha | Registra reprogramación, causa, fecha anterior/nueva y notificación derivada. |
| TR-08 | Audiencia pasada pendiente | Se muestra como atraso/requiere cierre, nunca como neutral o concluida. |
| TR-09 | Concluir audiencia con acta | Evento, documento y resultado quedan vinculados y exportables. |
| TR-10 | Crear término/audiencia independiente | Contexto suficiente, origen visible y localizable en “Sin expediente”. |
| TR-11 | Vincular independiente a expediente | Conserva eventos previos y registra vínculo con actor/fecha; no duplica registro. |
| TR-12 | Intentar expediente y contexto simultáneos | Rechazo transaccional por restricción XOR. |
| TR-13 | Intentar registro sin expediente ni contexto | Rechazo o envío explícito a “Sin clasificar”, según regla aprobada. |
| TR-14 | Anular registro concluido | No se borra; queda tombstone, motivo, actor y payload anterior. |
| TR-15 | Evento sin periodo de etapa | Aparece en bloque “Sin etapa asociada” y también en exportación. |
| TR-16 | Eliminar/agregar documento | Evento con hash/metadata; el histórico conserva evidencia de la acción. |
| TR-17 | Dos ediciones concurrentes | Una falla por versión o se fusiona de forma controlada; nunca sobrescritura silenciosa. |
| TR-18 | Error a mitad de operación | Rollback completo: ni cambio sin evento ni evento sin cambio. |
| TR-19 | Cambio de política de plazo | Registros previos mantienen versión; nuevos usan la vigente; simulación coincide. |
| TR-20 | Día inhábil/suspensión | Cálculo reproducible con calendario y regla guardados. |
| TR-21 | Cambio de zona/DST | UTC permanece estable y la presentación local es consistente. |
| TR-22 | Usuario sin permiso | No puede consultar ni exportar; el intento se audita según política de seguridad. |
| TR-23 | Búsqueda por abogado en historial | Devuelve términos y audiencias correspondientes y coincide con el placeholder. |
| TR-24 | Exportación | Mismo universo y filtros que la pantalla, con conteos y hash/fecha de generación. |
| TR-25 | Reapertura/reversión | Evento compensatorio; nunca edición o borrado del evento original. |

## 10. Roadmap recomendado

### Fase 0 — Contención, 1 a 2 semanas

- Evitar eliminación física de Términos/Audiencias concluidos.
- Mostrar eventos sin periodo en Trazabilidad.
- Crear `completedAt/completedBy` y dejar de usar `updatedAt` como conclusión.
- Hacer visible “Sin expediente” y corregir la búsqueda prometida.
- Corregir recursos estáticos de la ruta de trazabilidad.
- Cambiar audiencia pasada pendiente a estado de alerta explícito.

### Fase 1 — Coherencia funcional, 3 a 5 semanas

- Definir máquina de estados y vocabulario común.
- Unificar fuente del historial; migrar o convertir tablas históricas en proyecciones.
- Aplicar restricción expediente XOR contexto y separar independiente de sin clasificar.
- Unificar filtros, paginación por pestaña y restauración de URL.
- Resolver accesibilidad P1 de navegación, filtros, KPIs, calendario y modales.

### Fase 2 — Motor de plazos, 4 a 8 semanas

- Diseñar `DeadlinePolicy/AlertPolicy`, calendarios jurídicos, versiones y vigencias.
- Migrar umbrales duplicados y retirar lógica directa de entidades/vistas.
- Incorporar simulador, impacto antes de publicar y snapshot de política por registro.

### Fase 3 — Auditoría transversal, 6 a 10 semanas

- Implementar `AuditEvent` append-only con correlación y versión.
- Proyectar cronología, etapas e historial procesal desde la misma fuente.
- Agregar búsqueda/filtros y exportación equivalente.
- Cubrir la matriz automatizada de integridad, concurrencia y permisos.

### Fase 4 — Optimización basada en uso

- Instrumentar tiempo a primer hallazgo, filtros utilizados, búsquedas sin resultado, reaperturas y registros sin clasificar.
- Realizar pruebas moderadas por rol y simplificar filtros de baja frecuencia.
- Añadir vistas guardadas, accesos rápidos y alertas personalizadas sólo con evidencia de necesidad.

## 11. Indicadores de éxito

- 100% de cambios sensibles genera evento auditable dentro de la misma transacción.
- 0 eliminaciones físicas de registros procesales concluidos.
- 0 diferencias de universo entre pantalla de trazabilidad e informe exportado.
- 100% de términos/audiencias tiene expediente o contexto válido; 0 casos con ambos.
- Reducción de al menos 50% del tiempo para localizar un concluido sin expediente.
- Menos de 2% de búsquedas del historial sin resultado causadas por discrepancia entre placeholder y campos indexados.
- 100% de vencimientos conserva versión de política y explicación del cálculo.
- Navegación primaria y filtros operables completamente con teclado.
- Cero errores de recursos/MIME en las rutas auditadas.

## 12. Criterio de priorización de producto

La secuencia correcta no es “mejorar colores → añadir filtros → rediseñar pantallas”. Es:

1. proteger la integridad histórica;
2. definir estados, fechas y reglas como una sola verdad;
3. hacer visible esa verdad en la arquitectura de información;
4. optimizar interacción, accesibilidad y estética;
5. medir y ajustar con usuarios reales.

El mayor valor de SIAJ no será tener más pantallas, sino conseguir que cada dato jurídico tenga **origen, contexto, vigencia, responsable y explicación**. La UI actual puede evolucionar hacia ese objetivo sin perder su identidad visual.

## Anexo A. Evidencias visuales

- `output/playwright/auditoria-ux-2026-09-09/expedientes.png`
- `output/playwright/auditoria-ux-2026-09-09/terminos.png`
- `output/playwright/auditoria-ux-2026-09-09/audiencias.png`
- `output/playwright/auditoria-ux-2026-09-09/concluidos.png`
- `output/playwright/auditoria-ux-2026-09-09/catalogos.png`
- `output/playwright/auditoria-ux-2026-09-09/trazabilidad.png`
- `output/playwright/auditoria-ux-2026-09-09/expedientes-mobile.png`
- `output/playwright/auditoria-ux-2026-09-09/terminos-mobile.png`

## Anexo B. Evidencias técnicas principales

- Semáforo separado: `src/main/java/com/juridico/sistema_juridico/Entity/configuracion/ConfiguracionSemaforo.java:31-54`.
- Precedencia y aplicación sólo a Términos: `src/main/java/com/juridico/sistema_juridico/service/configuracion/ConfiguracionSemaforoService.java:26-76`.
- Umbrales duplicados en tipos: `Entity/catalogo/TipoTermino.java:39-53` y `TipoAudiencia.java:34-44`.
- Cálculo divergente: `Entity/procesal/Termino.java:161-179` y `Audiencia.java:93-112`.
- Contextos independientes: `TerminoContexto.java:15-42` y `AudienciaContexto.java:15-42`.
- Consulta doble del historial: `controller/AgendaController.java:56-80`.
- Historial de términos basado en actualización: `repository/procesal/TerminoRepository.java:172-201`.
- Eventos sin periodo no representados: `controller/ExpedientesController.java:746-810` frente a `templates/views/expedientes/trazabilidad.html:79-173`.
- Eliminación física de término: `controller/TerminosController.java:1159-1165`.
- Manejadores duplicados de búsqueda: `static/js/main-EXP.js:639-678` y `724-806`.
- Interacción no semántica: `templates/views/audiencias/index.html:91-147` y `static/js/audiencias.js:1390-1464`.

