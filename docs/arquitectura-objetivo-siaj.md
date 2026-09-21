# SIAJ — Arquitectura objetivo del producto

**Versión objetivo:** SIAJ 2.0  
**Fecha:** 10 de septiembre de 2026  
**Enfoque:** Product Architecture, Software Architecture, UX Strategy y LegalTech

## 1. Visión del producto

SIAJ será el sistema de registro, ejecución, control y evidencia de la operación jurídica. Su unidad de valor no será la pantalla ni el documento aislado, sino el **hecho jurídico trazable**: qué ocurrió, sobre qué asunto, quién lo realizó, bajo qué regla, en qué momento y con qué evidencia.

La versión objetivo se rige por siete decisiones:

1. **Expedientes es el centro del asunto formal**, pero Términos y Audiencias también pueden existir como asuntos independientes plenamente identificados.
2. **Términos y Audiencias son módulos operativos**, responsables de ejecutar trabajo futuro y pendiente.
3. **Historial Procesal es una proyección de consulta**, no una segunda fuente de datos.
4. **Trazabilidad es un registro transversal, inmutable y append-only**.
5. **Catálogos contiene sólo datos maestros**, no usuarios, permisos ni políticas.
6. **Administración concentra identidad, seguridad, calendarios y políticas versionadas**.
7. **Plazo, estado de trabajo y alerta son dimensiones distintas**; el color siempre se deriva y nunca constituye un estado editable.

## 2. Conceptos canónicos

| Concepto | Definición objetivo |
|---|---|
| Expediente | Contenedor formal del caso, con identidad jurídica, partes, competencia, etapa y responsables. |
| Contexto procesal | Identidad mínima obligatoria de todo Término o Audiencia. Es una referencia a Expediente o un Asunto independiente, nunca ambos. |
| Asunto independiente | Contexto jurídico legítimo que no requiere expediente. No equivale a dato incompleto. |
| Sin clasificar | Registro excepcional que no cumple contexto mínimo y entra a una bandeja de saneamiento. No es un estado operativo normal. |
| Término | Obligación con fecha base, regla de cómputo, vencimiento, flujo de preparación y evidencia de cumplimiento. |
| Audiencia | Evento jurídico programado, con participantes, modalidad/lugar, resultado y documentación de cierre. |
| Historial Procesal | Lectura cronológica de actuaciones cerradas, anuladas o reabiertas. |
| Evento de auditoría | Hecho inmutable que describe una creación, cambio, transición, vínculo, documento, decisión o anulación. |
| Política de plazo | Regla versionada que calcula vencimiento usando fecha base, unidad, calendario y excepciones. |
| Política de alerta | Regla versionada que traduce tiempo restante y contexto en nivel de atención. |

---

# 3. Diseño objetivo por módulo

## 3.1 Expedientes

### 1. Objetivo

Permitir crear, gestionar y consultar el asunto jurídico formal completo, ofreciendo una vista consolidada de identidad, situación procesal, responsables, próximos hitos, documentos y evidencia histórica.

### 2. Responsabilidad única

Ser la **fuente de verdad del caso formal**. Coordina la relación con Términos, Audiencias y Documentos, pero no implementa sus flujos internos.

### 3. Información que debe contener

- Número y tipo de expediente.
- Descripción/asunto y referencia externa.
- Partes estructuradas y rol procesal.
- Materia, órgano, jurisdicción, sede y competencia.
- Gerencia, equipo y abogado responsable.
- Etapa procesal actual y periodos de etapa.
- Prioridad administrativa.
- Próximo hito calculado y nivel de atención derivado.
- Resumen de Términos, Audiencias y Documentos relacionados.
- Fechas de apertura, conclusión, archivo y última actividad.
- Estado de integridad: completo, requiere datos o en saneamiento.

### 4. Información que NO debe contener

- Umbrales de semáforo configurables.
- Copias editables de Términos, Audiencias o sus históricos.
- Credenciales, roles o permisos de usuarios.
- Catálogos embebidos como texto libre cuando exista una referencia maestra.
- Bitácoras editables.
- Estado de urgencia capturado manualmente como color.

### 5. Relación con otros módulos

- Es padre opcional de Términos y Audiencias mediante `ContextoProcesal`.
- Consume referencias vigentes de Catálogos.
- Publica eventos hacia Trazabilidad.
- Sus actuaciones concluidas aparecen en Historial Procesal.
- Sus responsables, permisos y políticas proceden de Administración.

### 6. Flujos principales

1. Crear expediente y validar duplicados.
2. Completar identidad, partes, competencia y responsables.
3. Cambiar etapa con fecha efectiva y motivo.
4. Crear Término o Audiencia desde el contexto del expediente.
5. Consultar próximos hitos y atender alertas.
6. Agregar, clasificar y consultar documentos.
7. Concluir, reabrir, archivar o anular con autorización y motivo.
8. Consultar/exportar trazabilidad.

### 7. Navegación recomendada

- Listado con vistas guardadas y acceso directo a detalle.
- Detalle con pestañas: **Resumen**, **Términos**, **Audiencias**, **Documentos**, **Trazabilidad**.
- Breadcrumb: `Expedientes / [Número] / [Pestaña]`.
- Volver al listado conserva consulta, filtros, orden y página.

### 8. Componentes UI

- Barra de búsqueda global del módulo.
- Selector de vistas: Todos, Mis expedientes, Riesgo, Sin responsable, Sin actividad.
- Tabla responsiva con modo tarjeta móvil.
- Chip de etapa, estado y nivel de atención con texto/icono.
- Tarjeta “Próximo hito” con explicación del cálculo.
- Resumen de integridad y datos faltantes.
- Timeline compacto de actividad reciente.
- Menú de acciones contextual según estado y permiso.
- Modales sólo para acciones breves; formularios complejos en página/panel lateral.

### 9. Filtros

**Primarios:** búsqueda, Materia, Etapa, nivel de atención y responsable.  
**Secundarios:** Gerencia, tipo de expediente, órgano, jurisdicción, prioridad, fecha de apertura, fecha de última actividad, próximo hito y estado.  
**Vistas rápidas:** vencidos, vence hoy, próximos 7 días, sin responsable, sin actividad 30 días y con datos incompletos.

### 10. KPIs

- Expedientes activos.
- Expedientes con vencimiento vencido/hoy/próximos 7 días.
- Expedientes sin responsable.
- Expedientes sin actividad en el umbral definido.
- Tiempo mediano por etapa.
- Distribución de carga por abogado.
- Expedientes con integridad incompleta.

Cada KPI debe abrir la vista filtrada que lo explica.

### 11. Acciones críticas

- Cambiar etapa.
- Reasignar responsable.
- Concluir o reabrir.
- Anular.
- Vincular/desvincular una actuación.
- Exportar trazabilidad.
- Corregir identidad jurídica.

Todas exigen confirmación contextual; cambio sensible, reapertura y anulación exigen motivo.

### 12. Estados permitidos

**Ciclo de vida:** `BORRADOR → ACTIVO ↔ SUSPENDIDO → CONCLUIDO → ARCHIVADO`.  
Transiciones excepcionales: `CONCLUIDO → ACTIVO` mediante reapertura; cualquier estado no archivado puede pasar a `ANULADO` con autorización.  
**Etapa procesal:** referencia versionada independiente del ciclo de vida.  
**Atención temporal derivada:** `SIN_HITO`, `EN_TIEMPO`, `PROXIMO`, `VENCE_HOY`, `VENCIDO`, `PAUSADO`, `CERRADO`.

### 13. Casos excepcionales

- Número duplicado en la misma competencia.
- Cambio de órgano o materia con actuaciones existentes.
- Reasignación masiva por ausencia/baja de abogado.
- Reapertura de expediente concluido.
- Fusión o separación de expedientes.
- Expediente reservado o con acceso restringido.
- Evento recibido fuera de orden o durante una transición concurrente.

### 14. Riesgos de confusión

- Confundir etapa con estado del expediente.
- Confundir prioridad administrativa con urgencia por plazo.
- Interpretar “sin hito” como “sin riesgo”.
- Suponer que archivar elimina información.
- Tratar partes como texto único e impedir búsqueda/normalización.

La UI debe nombrar siempre la dimensión: **Estado**, **Etapa**, **Prioridad** y **Atención**.

### 15. Recomendación final

Mantener Expedientes como núcleo del producto y convertir su detalle en la vista de control del caso, no en una réplica de los módulos relacionados.

---

## 3.2 Términos

### 1. Objetivo

Gestionar de extremo a extremo las obligaciones sujetas a plazo: origen, cómputo, preparación, revisión, presentación, conclusión y evidencia.

### 2. Responsabilidad única

Ser la **fuente operativa de cada término y de su cumplimiento**.

### 3. Información que debe contener

- Contexto procesal: expediente o asunto independiente.
- Tipo de término.
- Actuación/asunto.
- Fecha y evento base.
- Política de plazo aplicada y versión.
- Vencimiento calculado y explicación reproducible.
- Fecha manual y justificación, cuando la política lo permita.
- Etapa del flujo de trabajo y responsables.
- Estado del ciclo de vida.
- Estado temporal derivado.
- Prioridad administrativa.
- Documento/acuse/evidencia.
- Fechas y autores de presentación, conclusión, cancelación o anulación.

### 4. Información que NO debe contener

- Umbrales de color duplicados.
- Copia de datos completos del expediente.
- Un `updatedAt` usado como fecha de conclusión.
- Estados de revisión mezclados con estados de cumplimiento.
- Reglas de calendario codificadas en la entidad.
- Histórico paralelo editable.

### 5. Relación con otros módulos

- Pertenece a un Expediente o Asunto independiente mediante XOR.
- Consume Tipo de término de Catálogos.
- Consume política de plazo, calendario y permisos de Administración.
- Publica cada transición y cambio en Trazabilidad.
- Al cerrar, aparece en Historial Procesal como proyección.

### 6. Flujos principales

1. Crear desde expediente o como asunto independiente.
2. Elegir tipo y fecha/evento base.
3. Previsualizar y aceptar el cómputo.
4. Asignar responsable y avanzar por revisión/aprobación.
5. Adjuntar versión/documento y registrar presentación.
6. Concluir con resultado y evidencia.
7. Cancelar, anular o reabrir mediante flujo controlado.
8. Vincular posteriormente un asunto independiente a expediente.

### 7. Navegación recomendada

- Vistas: **Mi trabajo**, **Hoy**, **Vencidos**, **Próximos**, **En revisión**, **Presentados pendientes de cierre**, **Todos**.
- Alternancia Lista/Calendario conservando los mismos filtros.
- Detalle en página o drawer ancho con resumen, cómputo, flujo, documentos y trazabilidad contextual.

### 8. Componentes UI

- Tarjeta de vencimiento con fecha, días hábiles/naturales y explicación.
- Stepper de flujo: Preparación → Revisión → Aprobación → Presentación → Cierre.
- Badge textual de estado temporal.
- Panel de contexto procesal.
- Checklist de cierre y evidencia.
- Calendario accesible.
- Historial de versiones/documentos.
- Acción primaria única según el estado actual.

### 9. Filtros

**Primarios:** vencimiento/periodo, estado temporal, etapa de trabajo y responsable.  
**Secundarios:** estado de ciclo de vida, tipo, Materia, Gerencia, prioridad, origen, vinculación, con/sin evidencia y rango exacto.  
**Vistas rápidas:** vencidos, hoy, 3 días, 7 días, sin expediente, sin acuse y presentados sin cierre.

### 10. KPIs

- Vencidos abiertos.
- Vencen hoy.
- Próximos 3 y 7 días.
- En revisión por nivel.
- Presentados pendientes de cierre.
- Sin responsable.
- Sin evidencia de cumplimiento.
- Cumplimiento en tiempo por periodo, tipo y equipo.
- Tiempo mediano por etapa de revisión.

### 11. Acciones críticas

- Modificar fecha/evento base.
- Aplicar excepción manual al vencimiento.
- Aprobar/rechazar.
- Registrar presentación.
- Concluir/reabrir.
- Cancelar/anular.
- Vincular/desvincular expediente.

Fecha manual, reapertura y anulación exigen motivo, permiso y evento auditable.

### 12. Estados permitidos

**Ciclo de vida:** `BORRADOR → EN_GESTION → PRESENTADO → CONCLUIDO`.  
Salidas alternativas: `CANCELADO` antes de conclusión y `ANULADO` por corrección jurídica autorizada. `CONCLUIDO → EN_GESTION` sólo por reapertura.  
**Etapa de trabajo:** `PREPARACION`, `REVISION`, `GERENCIA`, `DIRECCION`, `LIBERADO`; sólo aplica mientras está `EN_GESTION`.  
**Estado temporal derivado:** `SIN_CALCULO`, `EN_TIEMPO`, `PROXIMO`, `VENCE_HOY`, `VENCIDO`, `PAUSADO`, `CERRADO`.

### 13. Casos excepcionales

- Plazo manual o indeterminado.
- Suspensión de cómputo.
- Día inhábil extraordinario publicado después de crear el término.
- Presentación parcial o rechazada.
- Varios documentos de cumplimiento.
- Reapertura posterior a conclusión.
- Término creado sin expediente y vinculado después.
- Cambio de política vigente con términos abiertos.

### 14. Riesgos de confusión

- Interpretar “Presentado” como “Concluido”.
- Confundir prioridad con vencimiento.
- Mostrar días sin indicar si son hábiles o naturales.
- Cambiar una regla y recalcular históricos sin advertencia.
- Permitir color manual o estados técnicos visibles.

### 15. Recomendación final

Modelar Términos con tres ejes independientes —ciclo de vida, etapa de trabajo y condición temporal— y un cálculo versionado explicable.

---

## 3.3 Audiencias

### 1. Objetivo

Planificar, atender y cerrar eventos jurídicos, asegurando disponibilidad, contexto, resultado y documentación posterior.

### 2. Responsabilidad única

Ser la **fuente operativa de agenda y resultado de audiencias**.

### 3. Información que debe contener

- Contexto procesal: expediente o asunto independiente.
- Tipo de audiencia.
- Fecha/hora de inicio, duración y zona horaria.
- Modalidad: presencial, virtual o híbrida.
- Sede, sala, dirección o enlace seguro.
- Órgano y participantes.
- Responsable y asistentes internos.
- Estado de programación.
- Resultado de celebración.
- Estado documental del acta.
- Recordatorios aplicados.
- Acta y documentos relacionados.
- Fecha/autor de conclusión, cancelación, diferimiento o anulación.

### 4. Información que NO debe contener

- Umbrales de proximidad codificados.
- Copias editables del expediente.
- Un evento “desahogado” duplicado en otra tabla operativa.
- Una fecha pasada interpretada automáticamente como concluida.
- Credenciales o datos de calendario personal no necesarios.

### 5. Relación con otros módulos

- Pertenece a Expediente o Asunto independiente mediante XOR.
- Consume Tipo de audiencia, Órgano y Sede de Catálogos.
- Consume recordatorios, permisos y zona/calendarios de Administración.
- Publica eventos en Trazabilidad.
- Al concluir, aparece en Historial Procesal.
- Puede generar Términos posteriores como una operación correlacionada.

### 6. Flujos principales

1. Programar y verificar conflicto de agenda.
2. Confirmar modalidad, lugar/enlace y participantes.
3. Reprogramar o diferir con motivo y notificación.
4. Registrar si se celebró y su resultado.
5. Adjuntar/validar acta.
6. Concluir.
7. Cancelar, anular o reabrir.
8. Crear Término derivado del resultado.

### 7. Navegación recomendada

- Vistas: **Hoy**, **Próximas**, **Calendario**, **Pendientes de acta**, **Pasadas sin cierre**, **Todas**.
- Alternancia Día/Semana/Mes/Lista.
- Detalle contextual accesible desde calendario sin perder fecha ni filtros.
- Acceso desde el expediente y retorno al origen.

### 8. Componentes UI

- Calendario accesible con botones semánticos.
- Agenda diaria por franja horaria.
- Tarjeta de audiencia con estado, hora, lugar y acción siguiente.
- Detector de conflictos.
- Panel de participantes.
- Checklist de cierre: resultado, acta, términos derivados.
- Chips con texto/icono para modalidad y atención.
- Banner destacado para pasada sin cierre.

### 9. Filtros

**Primarios:** periodo/fecha, estado de programación, tipo y responsable.  
**Secundarios:** modalidad, sede, órgano, Materia, Gerencia, origen, vinculación, resultado y estado del acta.  
**Vistas rápidas:** hoy, mañana, semana, conflicto, sin expediente, pendiente de acta y pasada sin cierre.

### 10. KPIs

- Audiencias de hoy y próximos 7 días.
- Pasadas sin cierre.
- Pendientes de acta.
- Reprogramadas/diferidas.
- Conflictos de responsable o sala.
- Canceladas.
- Tiempo medio entre celebración y carga de acta.
- Porcentaje concluido con documentación completa.

### 11. Acciones críticas

- Reprogramar/diferir.
- Cambiar modalidad o sede.
- Registrar resultado.
- Adjuntar/reemplazar acta.
- Concluir/reabrir.
- Cancelar/anular.
- Generar Término derivado.

### 12. Estados permitidos

**Ciclo de vida:** `BORRADOR → PROGRAMADA → CELEBRADA → CONCLUIDA`.  
Salidas: `CANCELADA` y `ANULADA`. Una reprogramación es un evento y mantiene `PROGRAMADA`; un diferimiento registra resultado `DIFERIDA` y crea nueva programación correlacionada.  
**Documentación:** `NO_REQUERIDA`, `PENDIENTE_ACTA`, `ACTA_ADJUNTA`, `ACTA_VALIDADA`.  
**Atención derivada:** `FUTURA`, `PROXIMA`, `HOY`, `PASADA_SIN_CIERRE`, `CERRADA`.

### 13. Casos excepcionales

- Audiencia sin hora confirmada.
- Cambio urgente de sala/modalidad.
- Varias sesiones para el mismo acto.
- Diferimiento repetido.
- Resultado no celebrado.
- Acta sustituida o corregida.
- Participante con agenda en conflicto.
- Audiencia independiente vinculada después.

### 14. Riesgos de confusión

- Confundir “celebrada” con “concluida”.
- Confundir diferimiento con edición de fecha.
- Ocultar una audiencia pasada bajo estilo neutral.
- Usar “con acta” como estado general cuando es una dimensión documental.
- Omitir zona horaria en eventos remotos.

### 15. Recomendación final

Separar programación, resultado y documentación. La audiencia sólo se concluye cuando el cierre requerido está completo o una excepción autorizada lo justifica.

---

## 3.4 Historial Procesal

### 1. Objetivo

Permitir localizar, comparar y consultar actuaciones procesales cerradas sin importar su módulo de origen o vinculación.

### 2. Responsabilidad única

Ser la **vista histórica consolidada de lectura** para Términos y Audiencias. No crea ni edita la fuente operativa.

### 3. Información que debe contener

- Tipo de actuación.
- Identificador y referencia.
- Expediente o Asunto independiente.
- Origen y tipo de vinculación.
- Asunto/actuación y partes resumidas.
- Responsable.
- Fecha jurídica relevante.
- Fecha real de conclusión.
- Resultado y estado histórico.
- Documento/evidencia disponible.
- Indicador de reapertura, cancelación o anulación.
- Acceso al detalle y a la trazabilidad.

### 4. Información que NO debe contener

- Formularios completos de edición.
- Datos duplicados independientes de la fuente.
- Paginación compartida entre tipos.
- Una fecha genérica sin significado.
- Registros abiertos como si fueran históricos, salvo vista explícita de excepción.

### 5. Relación con otros módulos

- Lee proyecciones de Términos, Audiencias y Trazabilidad.
- Navega al Expediente o Asunto independiente.
- Usa Catálogos para etiquetas históricas versionadas.
- Respeta permisos definidos en Administración.

### 6. Flujos principales

1. Buscar una actuación concluida por cualquier referencia significativa.
2. Filtrar por tipo, periodo, responsable, resultado o vinculación.
3. Revisar evidencia y trazabilidad.
4. Localizar “Sin expediente” y distinguir independiente de sin clasificar.
5. Vincular o sanear un registro excepcional mediante un flujo autorizado.
6. Solicitar reapertura desde el detalle fuente.
7. Exportar el conjunto filtrado.

### 7. Navegación recomendada

- Subvistas: **Todos**, **Términos**, **Audiencias**, **Sin expediente**, **Sin clasificar**, **Anulados/Reabiertos**.
- La URL conserva subvista, filtros, orden y cursor.
- Cada fila abre un detalle de sólo lectura con enlaces al origen y a su evidencia.

### 8. Componentes UI

- Búsqueda global con ejemplos de campos admitidos.
- Tabs con conteos.
- Tabla cronológica unificada con columna Tipo.
- Columnas específicas desplegables al elegir una sola subvista.
- Chip de vinculación: Expediente, Independiente, Sin clasificar.
- Visor de evidencia.
- Drawer de detalle histórico.
- Exportación con resumen de filtros.

### 9. Filtros

**Primarios:** búsqueda, tipo de actuación, rango de fecha y vinculación.  
**Secundarios:** Gerencia, Materia, abogado, resultado, concluyó por, origen, tipo específico, con/sin documento, reabierto/anulado.  
La búsqueda cubre número, partes, actuación, abogado, ID y referencia externa.

### 10. KPIs

- Concluidos en el periodo por tipo.
- Concluidos en tiempo/fuera de tiempo.
- Sin evidencia requerida.
- Sin expediente: independientes vs sin clasificar.
- Reabiertos.
- Anulados.
- Tiempo hasta cierre.

### 11. Acciones críticas

- Exportar.
- Abrir fuente y trazabilidad.
- Solicitar reapertura.
- Vincular un registro sin clasificar.
- Confirmar que permanece independiente.
- Reportar inconsistencia.

No existe eliminar ni edición directa.

### 12. Estados permitidos

Historial no posee una máquina de estados propia. Proyecta: `CONCLUIDO`, `CANCELADO`, `ANULADO`, `REABIERTO`. Un registro reabierto conserva su entrada histórica y muestra el evento de reapertura; la versión operativa vuelve al módulo fuente.

### 13. Casos excepcionales

- Registro histórico cuyo catálogo fue desactivado.
- Documento no disponible pero metadata conservada.
- Actuación reabierta varias veces.
- Corrección de identidad con evidencia anterior.
- Registro sin contexto suficiente.
- Datos migrados sin actor o sin fecha precisa.

### 14. Riesgos de confusión

- Presentar Historial como almacén separado.
- Mezclar fecha jurídica, fecha de registro y fecha de conclusión.
- Llamar “sin expediente” tanto a independientes válidos como a datos incompletos.
- Permitir que un filtro de una pestaña afecte silenciosamente a otra.

### 15. Recomendación final

Implementar Historial como un read model único, indexado y reconstruible desde fuentes operativas y eventos, con una bandeja explícita para saneamiento.

---

## 3.5 Catálogos

### 1. Objetivo

Gobernar los datos maestros jurídicos y territoriales usados por la operación.

### 2. Responsabilidad única

Ser la **fuente de verdad de vocabularios reutilizables**, sus alcances, vigencias y dependencias.

### 3. Información que debe contener

- Materias.
- Etapas procesales.
- Órganos y competencias.
- Tipos de Audiencia.
- Tipos de Expediente.
- Tipos de Término.
- Entidades federativas.
- Sedes, salas y lugares.
- Motivos/resultados de conclusión, cancelación, diferimiento y anulación.
- Código estable, nombre, descripción, alcance, vigencia, estado, versión y dependencias.

### 4. Información que NO debe contener

- Usuarios, abogados, roles o permisos.
- Semáforos y umbrales.
- Calendarios de días inhábiles.
- Políticas de cómputo.
- Registros operativos.
- Valores de configuración técnica.

### 5. Relación con otros módulos

- Provee referencias a Expedientes, Términos y Audiencias.
- Historial conserva la etiqueta/snapshot vigente al momento del hecho.
- Administración gobierna quién puede publicar cambios.
- Toda alta, edición, desactivación y reemplazo publica eventos de Trazabilidad administrativa.

### 6. Flujos principales

1. Consultar y buscar valores.
2. Crear borrador.
3. Validar código, nombre, alcance y duplicados.
4. Revisar dependencias e impacto.
5. Publicar con fecha de vigencia.
6. Sustituir/deprecar un valor.
7. Desactivar para captura nueva conservando históricos.
8. Exportar catálogo y cambios.

### 7. Navegación recomendada

- Grupos: **Jurídicos**, **Organización/competencia**, **Territoriales**, **Resultados**.
- Listado lateral o selector de catálogo; contenido central con alcance y vigencia visibles.
- Detalle en drawer con referencias y auditoría.
- Breadcrumb: `Administración / Catálogos / [Catálogo] / [Valor]`.

### 8. Componentes UI

- Selector de catálogo con conteo de activos.
- Tabla con código, nombre, alcance, vigencia, estado y dependencias.
- Formulario versionado.
- Panel “Dónde se usa”.
- Comparador de versión anterior/nueva.
- Confirmación de publicación con impacto.
- Sustituto recomendado al deprecar.

### 9. Filtros

**Primarios:** búsqueda y estado.  
**Secundarios:** alcance global/Gerencia, vigencia, usados/no usados, modificó por y fecha de actualización.  
La búsqueda se muestra sólo cuando el volumen la justifica.

### 10. KPIs

- Valores activos, próximos a vencer y deprecados.
- Valores sin uso.
- Valores duplicados candidatos.
- Cambios pendientes de publicación.
- Registros afectados por próxima modificación.
- Valores sin descripción/propietario.

### 11. Acciones críticas

- Publicar.
- Cambiar vigencia o alcance.
- Deprecar/sustituir.
- Fusionar duplicados.
- Desactivar.

No se permite eliminación física de valores referenciados.

### 12. Estados permitidos

`BORRADOR → VIGENTE → DEPRECADO → INACTIVO`.  
Un valor futuro puede estar `PROGRAMADO`. Un valor deprecado puede seguir apareciendo en históricos, pero no en captura nueva; opcionalmente apunta a su sustituto.

### 13. Casos excepcionales

- Dos nombres iguales con distinto alcance.
- Cambio de código externo.
- Fusión de órganos o sedes.
- Valor usado en registros históricos.
- Vigencias superpuestas.
- Migración sin código estable.
- Cambio urgente con fecha retroactiva autorizada.

### 14. Riesgos de confusión

- Confundir alcance con filtro visual.
- Asumir que inactivar modifica históricos.
- Mezclar Estado territorial con estado de ciclo de vida.
- Mostrar políticas como si fueran valores simples.
- Crear catálogos para conceptos que deben ser enums técnicos estables.

### 15. Recomendación final

Reducir Catálogos a datos maestros gobernados, con códigos estables, vigencia y desactivación segura. Todo comportamiento configurable pertenece a Administración.

---

## 3.6 Trazabilidad

### 1. Objetivo

Reconstruir de forma completa, confiable y exportable la historia de cualquier objeto jurídico o administrativo.

### 2. Responsabilidad única

Ser la **fuente canónica e inmutable de eventos de auditoría**.

### 3. Información que debe contener

- ID y tipo de evento.
- Tipo/ID del agregado afectado.
- Expediente padre cuando exista.
- Fecha/hora UTC y zona de presentación.
- Actor, rol y origen técnico.
- Antes/después estructurado.
- Motivo y comentario.
- Correlación y causalidad.
- Versión del objeto.
- Política/calendario aplicados.
- Metadata de documento y hash cuando corresponda.
- Clasificación de sensibilidad y retención.

### 4. Información que NO debe contener

- Eventos editables o eliminables desde UI.
- Descripciones opacas como única evidencia.
- Contraseñas, tokens o secretos.
- Payloads completos innecesarios de datos sensibles.
- Copias sin relación con un objeto fuente.

### 5. Relación con otros módulos

- Recibe eventos de todos los módulos y procesos de Administración.
- Alimenta la pestaña Trazabilidad del Expediente.
- Alimenta Historial Procesal y reportes de auditoría.
- No gobierna los estados de negocio; los registra.

### 6. Flujos principales

1. Consultar cronología de un expediente/actuación.
2. Alternar vista Cronología/Etapas/Cambios.
3. Filtrar y buscar eventos.
4. Expandir un grupo correlacionado.
5. Comparar antes/después.
6. Abrir objeto/documento fuente.
7. Exportar con filtros y sello de generación.
8. Investigar inconsistencias sin alterar la evidencia.

### 7. Navegación recomendada

- Pestaña dentro de cada detalle y consola transversal para perfiles autorizados.
- Subvistas: **Cronología**, **Etapas**, **Cambios**, **Documentos**, **Excepciones**.
- Deep links por `eventId` y retorno al objeto origen.

### 8. Componentes UI

- Timeline virtualizada.
- Grupos por operación/correlación.
- Diff legible de campos.
- Filtros persistentes.
- Identidad de actor y timestamp preciso.
- Bloque “Sin etapa asociada”.
- Visor de metadata/documento.
- Exportación firmada o con hash de integridad cuando la política lo exija.

### 9. Filtros

Tipo de evento, actor, rol, fecha, módulo fuente, etapa, objeto, campo modificado, evento sensible, con documento, anulación/reapertura, correlación e ID.

### 10. KPIs

Trazabilidad no debe convertirse en dashboard operativo. Para administración/auditoría sí puede mostrar:

- Eventos fallidos o enviados a cola de excepción.
- Operaciones sin correlación esperada.
- Cambios sensibles por periodo.
- Anulaciones/reaperturas.
- Latencia de proyección.
- Exportaciones realizadas.

### 11. Acciones críticas

- Exportar.
- Verificar integridad.
- Marcar inconsistencia para investigación.
- Navegar a fuente.
- Generar evento compensatorio desde el módulo dueño.

Nunca editar ni borrar un evento.

### 12. Estados permitidos

Un evento confirmado es `REGISTRADO` e inmutable. La infraestructura puede manejar `PENDIENTE_PROYECCION`, `PROYECTADO` o `ERROR_PROYECCION`, sin cambiar el hecho jurídico. Las correcciones se representan con eventos compensatorios enlazados.

### 13. Casos excepcionales

- Evento sin etapa.
- Evento anterior a la migración sin actor identificable.
- Eventos simultáneos.
- Reintento idempotente.
- Proyección atrasada.
- Documento retirado por política de retención conservando metadata.
- Cambio en objeto independiente vinculado posteriormente.
- Solicitud de rectificación de dato personal sin destruir evidencia legal obligatoria.

### 14. Riesgos de confusión

- Confundir actividad informativa con cambio de datos.
- Duplicar un hecho en evento resumen y eventos de campo sin agruparlos.
- Interpretar el orden de recepción como momento jurídico.
- Mostrar valores internos o IDs sin etiqueta legible.
- Suponer que exportar equivale a congelar la fuente si no se registra hash/fecha.

### 15. Recomendación final

Implementar un event log append-only transaccional con outbox, proyecciones reconstruibles y una UI que agrupe operaciones sin ocultar detalle.

---

## 3.7 Administración

### 1. Objetivo

Configurar y gobernar identidad, acceso, estructura organizacional, calendarios, políticas, notificaciones e integraciones.

### 2. Responsabilidad única

Ser el **plano de control del producto**, separado de la operación jurídica diaria.

### 3. Información que debe contener

- Usuarios, perfiles, roles y permisos.
- Equipos, gerencias y asignaciones.
- Alcances de datos por rol/área.
- Calendarios jurídicos, días inhábiles y suspensiones.
- Políticas de plazo y alerta versionadas.
- Plantillas y preferencias de notificación.
- Configuración de integraciones.
- Retención, privacidad y clasificación de información.
- Jobs, colas, proyecciones y salud operativa para administradores técnicos.
- Registro administrativo auditable.

### 4. Información que NO debe contener

- Expedientes o actuaciones editables.
- Datos maestros jurídicos que pertenecen a Catálogos.
- Semáforos como colores aislados.
- Credenciales o secretos visibles en texto.
- Parámetros sin descripción, alcance, versión y efecto.

### 5. Relación con otros módulos

- Autoriza acceso a todos los módulos.
- Provee políticas y calendarios a Términos/Audiencias.
- Gobierna publicadores de Catálogos.
- Emite eventos administrativos a Trazabilidad.
- Controla notificaciones derivadas de eventos, no lógica duplicada.

### 6. Flujos principales

1. Alta, invitación, asignación y baja de usuario.
2. Asignar roles y alcance con principio de menor privilegio.
3. Crear/probar/publicar política de plazo o alerta.
4. Mantener calendario y publicar suspensión extraordinaria.
5. Simular impacto antes de activar configuración.
6. Configurar canales/plantillas de notificación.
7. Supervisar colas e integraciones.
8. Revisar/exportar cambios administrativos.

### 7. Navegación recomendada

- **Usuarios y acceso**.
- **Organización**.
- **Calendarios jurídicos**.
- **Políticas de plazos y alertas**.
- **Notificaciones**.
- **Integraciones**.
- **Auditoría administrativa**.
- **Operación técnica**, sólo para rol técnico.

### 8. Componentes UI

- Matriz rol-permiso con explicación de alcance.
- Detalle de usuario y sesiones/estado.
- Constructor de política por pasos.
- Simulador de vencimiento con casos ejemplo.
- Comparador de versiones e impacto.
- Calendario anual de inhábiles.
- Centro de salud de integraciones.
- Confirmación reforzada para publicación y revocación.

### 9. Filtros

- Usuarios: nombre/email, rol, área, estado, última actividad.
- Políticas: tipo, alcance, estado, versión, vigencia.
- Calendarios: jurisdicción, año, estado, vigencia.
- Auditoría: actor, acción, fecha, objeto y sensibilidad.
- Integraciones: sistema, estado, última ejecución y error.

### 10. KPIs

- Usuarios activos, bloqueados e inactivos.
- Accesos privilegiados.
- Políticas vigentes/próximas a entrar en vigor.
- Registros afectados por cambios programados.
- Excepciones manuales de plazo.
- Notificaciones fallidas.
- Integraciones degradadas.
- Cola de eventos/proyecciones atrasada.

### 11. Acciones críticas

- Otorgar/revocar rol.
- Desactivar usuario.
- Publicar/revertir política mediante nueva versión.
- Publicar día inhábil extraordinario.
- Rotar/revocar integración.
- Modificar retención.

Requieren motivo, confirmación, permisos reforzados y evento de auditoría.

### 12. Estados permitidos

- Usuario: `INVITADO`, `ACTIVO`, `BLOQUEADO`, `SUSPENDIDO`, `INACTIVO`.
- Política: `BORRADOR`, `EN_REVISION`, `PROGRAMADA`, `VIGENTE`, `RETIRADA`.
- Calendario: `BORRADOR`, `PUBLICADO`, `SUSTITUIDO`.
- Integración: `ACTIVA`, `DEGRADADA`, `PAUSADA`, `REVOCADA`.

### 13. Casos excepcionales

- Baja de usuario con asuntos asignados.
- Revocación urgente de acceso.
- Política publicada con error.
- Día inhábil retroactivo.
- Gerencia fusionada o eliminada.
- Integración caída con eventos pendientes.
- Administrador que intenta modificar su propio acceso crítico.

### 14. Riesgos de confusión

- Mezclar rol con puesto organizacional.
- Mostrar una política como si fuera un catálogo simple.
- No distinguir guardar borrador de publicar.
- Ocultar a cuántos registros afectará un cambio.
- Permitir configuración global sin visibilidad de alcance.

### 15. Recomendación final

Diseñar Administración como un plano de control versionado y seguro: ninguna política entra en vigor sin simulación, alcance, fecha efectiva, responsable y trazabilidad.

---

# 4. Arquitectura de información completa

## 4.1 Mapa de navegación

```text
SIAJ
├── Inicio
│   ├── Mi jornada
│   ├── Riesgos y vencimientos
│   ├── Audiencias próximas
│   └── Carga de trabajo
├── Expedientes
│   ├── Todos
│   ├── Mis expedientes
│   ├── En riesgo
│   ├── Sin responsable
│   └── Detalle de expediente
│       ├── Resumen
│       ├── Términos
│       ├── Audiencias
│       ├── Documentos
│       └── Trazabilidad
├── Términos
│   ├── Mi trabajo
│   ├── Vencidos
│   ├── Hoy
│   ├── Próximos
│   ├── En revisión
│   ├── Presentados sin cierre
│   ├── Calendario
│   └── Todos
├── Audiencias
│   ├── Hoy
│   ├── Próximas
│   ├── Calendario
│   ├── Pendientes de acta
│   ├── Pasadas sin cierre
│   └── Todas
├── Historial Procesal
│   ├── Todos
│   ├── Términos
│   ├── Audiencias
│   ├── Sin expediente
│   ├── Sin clasificar
│   └── Anulados y reabiertos
└── Administración
    ├── Catálogos
    │   ├── Jurídicos
    │   ├── Órganos y competencia
    │   ├── Territoriales
    │   └── Resultados y motivos
    ├── Usuarios y acceso
    ├── Organización
    ├── Calendarios jurídicos
    ├── Políticas de plazos y alertas
    ├── Notificaciones
    ├── Integraciones
    ├── Auditoría administrativa
    └── Operación técnica
```

## 4.2 Navegación por rol

| Rol | Entrada predeterminada | Navegación prioritaria |
|---|---|---|
| Abogado | Mi jornada | Mis Términos, Audiencias de hoy, Mis Expedientes. |
| Jefatura/Gerencia | Riesgos y carga | Riesgos, revisión, asignación, desempeño operativo. |
| Dirección | Panorama ejecutivo | Excepciones, cumplimiento, carga y trazabilidad. |
| Administrador funcional | Administración | Catálogos, usuarios, políticas y calendarios. |
| Auditor | Historial/Trazabilidad | Consulta transversal y exportación; sin edición operativa. |

## 4.3 Reglas de navegación

- Una entidad tiene una URL canónica y puede abrirse desde varios módulos.
- Los filtros se codifican en URL y se restauran al volver.
- La navegación secundaria del expediente permanece estable entre pestañas.
- Las acciones disponibles dependen de estado y permiso; no se muestran acciones imposibles.
- La búsqueda global superior localiza Expedientes, Términos, Audiencias y documentos por referencia, pero dirige al módulo dueño.
- “Historial” y “Trazabilidad” nunca ofrecen edición directa.

---

# 5. Arquitectura funcional

## 5.1 Capas funcionales

```text
┌──────────────────────────────────────────────────────────────────────┐
│ EXPERIENCIA                                                          │
│ Inicio │ Expedientes │ Términos │ Audiencias │ Historial │ Admin    │
└──────────────────────────────┬───────────────────────────────────────┘
                               │ comandos / consultas
┌──────────────────────────────▼───────────────────────────────────────┐
│ APLICACIÓN                                                           │
│ Casos de uso │ autorización │ validación │ transacciones │ outbox   │
└──────────────────────────────┬───────────────────────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────────────────────┐
│ DOMINIO                                                              │
│ Expediente │ Término │ Audiencia │ Catálogo │ Política │ Calendario │
└──────────────────────────────┬───────────────────────────────────────┘
                               │ eventos de dominio
┌──────────────────────────────▼───────────────────────────────────────┐
│ EVIDENCIA Y PROYECCIONES                                             │
│ AuditEvent append-only │ Historial Procesal │ KPIs │ Notificaciones │
└──────────────────────────────┬───────────────────────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────────────────────┐
│ INFRAESTRUCTURA                                                      │
│ PostgreSQL │ documentos │ búsqueda │ colas │ correo/push │ exportes │
└──────────────────────────────────────────────────────────────────────┘
```

## 5.2 Módulos y submódulos

| Módulo | Submódulos/capacidades | Fuente de verdad |
|---|---|---|
| Expedientes | Identidad, partes, competencia, asignación, etapas, resumen del caso | Aggregate `Expediente` |
| Términos | Cómputo, flujo de trabajo, presentación, cierre, evidencia | Aggregate `Termino` |
| Audiencias | Programación, conflictos, celebración, acta, cierre | Aggregate `Audiencia` |
| Historial Procesal | Índice combinado, búsqueda, saneamiento, exportación | Read model reconstruible |
| Catálogos | Jurídicos, órganos, territoriales, resultados | Aggregate/versiones de Catálogo |
| Trazabilidad | Event log, correlación, proyecciones, exportación | `AuditEvent` append-only |
| Administración | IAM, organización, políticas, calendarios, notificaciones, integraciones | Agregados administrativos versionados |

## 5.3 Dependencias permitidas

```text
Catálogos ───────────────► Expedientes
Catálogos ───────────────► Términos
Catálogos ───────────────► Audiencias

Administración/IAM ──────► Todos los módulos
Administración/Políticas ─► Términos
Administración/Políticas ─► Audiencias
Administración/Calendario ► Motor de plazos

Expedientes ─────────────► Términos      (contexto, no propiedad interna)
Expedientes ─────────────► Audiencias    (contexto, no propiedad interna)

Expedientes ─┐
Términos ────┼───────────► Trazabilidad
Audiencias ──┤
Catálogos ───┤
Admin ───────┘

Términos ────┐
Audiencias ──┼───────────► Historial Procesal
Trazabilidad ┘

Trazabilidad ────────────► Notificaciones / KPIs / Exportación
```

Regla estructural: ningún módulo operativo depende de Historial Procesal. Historial sólo lee. Ningún agregado modifica directamente el event log; publica eventos dentro de la misma unidad transaccional mediante outbox.

## 5.4 Contexto procesal

```text
                   ┌────────────────────┐
                   │ ContextoProcesal   │
                   │ tipo + referencia  │
                   └─────────┬──────────┘
                             │ exactamente uno
                 ┌───────────┴───────────┐
                 ▼                       ▼
        ┌────────────────┐      ┌────────────────────┐
        │ ExpedienteRef  │      │ AsuntoIndependiente│
        └────────────────┘      └────────────────────┘
                 ▲                       ▲
                 └───────────┬───────────┘
                             │
                     Término / Audiencia
```

Invariantes:

- Todo Término/Audiencia debe tener un contexto válido.
- Expediente y Asunto independiente son mutuamente excluyentes.
- “Sin clasificar” es una cola de excepción por migración/fallo, no una tercera alternativa normal.
- Vincular un independiente a expediente registra un evento y conserva toda su historia.

## 5.5 Motor de plazos y alertas

```text
Fecha/evento base
        │
        ▼
Tipo de actuación ──► DeadlinePolicy(versionada)
                              │
Calendario jurídico ──────────┤
Excepciones autorizadas ──────┤
                              ▼
                    Vencimiento calculado
                              │
                              ▼
                     AlertPolicy(versionada)
                              │
                 ┌────────────┼────────────┐
                 ▼            ▼            ▼
              Estado       Mensaje     Notificación
              temporal     explicable   por canal
```

Precedencia explícita de configuración:

```text
Política global
   └─ override por Gerencia
       └─ override por Tipo
           └─ excepción por Registro (motivo obligatorio)
```

Cada cálculo persiste: política, versión, calendario, parámetros, vencimiento y explicación. Los cambios futuros crean una nueva versión.

## 5.6 Flujo transaccional y auditoría

```text
Usuario ejecuta comando
        │
        ▼
Autorización + validación de invariantes
        │
        ▼
Transacción de dominio
  ├─ actualiza agregado
  └─ escribe evento en Outbox
        │ commit
        ▼
Publicador idempotente
  ├─ AuditEvent append-only
  ├─ Proyección Historial
  ├─ Índice de búsqueda
  ├─ KPIs
  └─ Notificaciones
```

La escritura del cambio y del outbox es atómica. Las proyecciones pueden ser asíncronas, medibles y reconstruibles. Cada consumidor usa `eventId` para idempotencia.

## 5.7 Flujo de conclusión

```text
Término/Audiencia operativa
        │
        ├─ valida resultado requerido
        ├─ valida evidencia requerida
        ├─ captura completedAt + completedBy
        └─ emite *_CONCLUIDO
                 │
                 ├─► Trazabilidad inmutable
                 ├─► Historial Procesal
                 ├─► Resumen de Expediente
                 └─► KPIs / notificaciones
```

Una reapertura no borra el cierre:

```text
CONCLUIDO ──(motivo + permiso)──► REABIERTO/EN_GESTION
     │                                  │
     └──────── cierre preservado ───────┘
```

## 5.8 Flujo de asunto sin expediente

```text
Crear actuación
      │
      ├─ ¿Tiene expediente? ─ Sí ─► vincular ExpedienteRef
      │
      └─ No
          ├─ ¿Es legítimamente independiente? ─ Sí ─► crear AsuntoIndependiente
          │
          └─ Datos insuficientes/migración ─► Sin clasificar
                                                │
                           ┌────────────────────┼────────────────────┐
                           ▼                    ▼                    ▼
                    Vincular existente   Crear expediente   Confirmar independiente
```

## 5.9 Flujo de anulación

```text
Solicitar anulación
      │
      ▼
Validar permiso + dependencias
      │
      ▼
Capturar motivo y confirmación reforzada
      │
      ▼
Estado ANULADO + evento compensatorio
      │
      ├─ registro permanece consultable
      ├─ deja de participar en operación futura
      └─ aparece en Historial/Trazabilidad
```

No existe borrado físico desde los casos de uso jurídicos.

---

# 6. Arquitectura de software objetivo

## 6.1 Estilo recomendado

Adoptar **monolito modular** como siguiente estado, con límites de dominio explícitos. No se recomienda dividir en microservicios antes de estabilizar estados, políticas y contratos.

```text
com.juridico.siaj
├── expedientes
│   ├── domain
│   ├── application
│   ├── infrastructure
│   └── web
├── terminos
├── audiencias
├── historial
├── catalogos
├── trazabilidad
├── administracion
│   ├── identidad
│   ├── politicas
│   ├── calendarios
│   └── notificaciones
└── shared
    ├── kernel
    ├── security
    └── outbox
```

Cada módulo expone casos de uso y eventos; queda prohibido acceder directamente a repositorios internos de otro módulo.

## 6.2 Separación comando/consulta

- **Comandos:** crean o cambian agregados, validan transiciones y emiten eventos.
- **Consultas:** leen proyecciones optimizadas para listados, calendario, dashboard, historial y trazabilidad.
- No es necesario implantar infraestructura CQRS compleja; basta separar modelos y responsabilidades en aplicación.

## 6.3 Agregados principales

| Agregado | Invariantes esenciales |
|---|---|
| Expediente | Identidad única por competencia; etapa vigente única; conclusión/reapertura controladas. |
| Termino | Contexto XOR; vencimiento explicable; transición válida; cierre con metadata inmutable. |
| Audiencia | Contexto XOR; programación consistente; resultado/documentación separados. |
| CatalogEntry | Código estable; vigencias sin solapamiento; no borrar si está referenciado. |
| DeadlinePolicy | Versión inmutable publicada; alcance y vigencia únicos. |
| LegalCalendar | Jurisdicción/año; versión y días efectivos. |
| UserAccess | Roles y alcances válidos; revocación auditable. |

`AuditEvent` es un registro inmutable, no un agregado editable.

## 6.4 Fuentes de verdad

| Dato | Dueño | Consumidores |
|---|---|---|
| Identidad del expediente | Expedientes | Términos, Audiencias, Historial, Trazabilidad |
| Estado/cumplimiento de término | Términos | Expedientes, Historial, KPIs |
| Programación/resultado de audiencia | Audiencias | Expedientes, Historial, Calendario |
| Nombres/códigos maestros | Catálogos | Todos los módulos operativos |
| Usuarios/permisos | Administración/IAM | Todos |
| Reglas y calendarios | Administración | Motor de plazos, alertas |
| Evidencia de cambios | Trazabilidad | Historial, auditoría, exportes |
| Vista combinada de concluidos | Historial read model | UI y reportes, nunca escritura |

## 6.5 Servicios compartidos

- `ContextoProcesalService`: validación y vinculación.
- `DeadlineCalculationService`: cómputo puro y explicable.
- `AlertEvaluationService`: nivel de atención derivado.
- `TransitionPolicy`: validación de máquina de estados por módulo.
- `AuditEventPublisher`: outbox y correlación.
- `AuthorizationService`: rol + alcance + objeto.
- `DocumentEvidenceService`: metadata, hash, clasificación y retención.
- `SearchProjectionService`: índice transversal y proyecciones por módulo.
- `NotificationOrchestrator`: reglas por evento, preferencia y canal.

## 6.6 Reglas de integridad obligatorias

1. Expediente/contexto independiente cumple XOR.
2. Ningún concluido se elimina físicamente.
3. `completedAt` y `completedBy` son inmutables; la reapertura crea eventos.
4. Todo cambio sensible y transición se confirma junto con evento/outbox.
5. Las políticas publicadas son inmutables y versionadas.
6. Todo vencimiento puede explicar parámetros y calendario.
7. Las proyecciones se reconstruyen desde fuentes canónicas.
8. Los filtros/exportes respetan el mismo alcance de seguridad que la pantalla.
9. Las fechas de negocio se almacenan con semántica explícita; eventos en UTC.
10. Los catálogos referenciados se deprecian, no se borran.

---

# 7. Sistema de experiencia transversal

## 7.1 Shell común

- Navegación lateral por tarea, no por entidad técnica.
- Encabezado con búsqueda global, contexto del usuario y alertas no invasivas.
- Breadcrumb y título único por página.
- Centro de notificaciones accesible; la invitación de permisos no bloquea el contenido.
- Diseño responsivo: tabla en escritorio, tarjetas en móvil sin pérdida de acciones.

## 7.2 Patrón de listado

```text
[Título] [conteo]                              [Acción primaria]
[Vista rápida] [Vista rápida] [Vista rápida]
[Buscar________________] [Filtros] [Orden] [Guardar vista]
[chips de filtros activos]                         [Limpiar]
────────────────────────────────────────────────────────────
Resultados / estado de carga / vacío / error recuperable
────────────────────────────────────────────────────────────
Paginación o cursor + total + exportar conjunto filtrado
```

## 7.3 Patrón de detalle

```text
Breadcrumb
Identidad + estado + atención                 Acción siguiente
Resumen del contexto y próximo hito
Tabs: Resumen | Relacionados | Documentos | Trazabilidad
Contenido con panel lateral de metadata sólo cuando aporte valor
```

## 7.4 Semántica visual

| Dimensión | Tratamiento |
|---|---|
| Estado de ciclo de vida | Chip neutro con texto. |
| Etapa de trabajo/procesal | Stepper o chip independiente. |
| Atención temporal | Color + icono + texto + fecha/días. |
| Prioridad administrativa | Indicador separado; nunca reutiliza el color temporal. |
| Integridad documental | Checklist o badge “Completo/Pendiente”. |
| Anulado/reabierto | Etiqueta explícita y acceso a motivo/evento. |

## 7.5 Estados de interfaz obligatorios

Todo listado y formulario implementa: carga, vacío inicial, cero resultados por filtros, error recuperable, sin permiso, conflicto de concurrencia, guardado exitoso y operación parcial/pendiente de proyección.

---

# 8. Dependencias de implementación

```text
1. Modelo canónico y máquinas de estado
   ├─ ContextoProcesal XOR
   ├─ completedAt/completedBy
   └─ no borrado físico
            │
            ▼
2. Outbox + AuditEvent
            │
            ├──────────────┐
            ▼              ▼
3. Motor de políticas   4. Read models
   y calendarios           Historial/KPIs/Búsqueda
            │              │
            └──────┬───────┘
                   ▼
5. UX objetivo por módulo
   ├─ listados/filtros
   ├─ detalles
   ├─ calendarios
   └─ administración
                   │
                   ▼
6. Optimización, analítica y automatización
```

No debe construirse el nuevo Historial antes de definir fuente de verdad y cierre inmutable. No debe migrarse el semáforo antes de separar cálculo de plazo y alerta. No debe rediseñarse la UI de estados antes de aprobar las máquinas de estado.

---

# 9. Definición de terminado de SIAJ 2.0

La arquitectura objetivo se considera implementada cuando:

- cada módulo tiene dueño de datos y responsabilidad única;
- Términos/Audiencias requieren Expediente o Asunto independiente válido;
- Historial no persiste una segunda versión editable del hecho;
- Trazabilidad registra todos los cambios y transiciones sin borrado;
- estados, etapas, prioridad y atención son dimensiones distintas;
- plazos y alertas proceden de políticas versionadas y explicables;
- Catálogos y Administración están separados;
- navegación, filtros, URL, exportación y permisos comparten el mismo contrato;
- todas las acciones críticas exigen motivo y dejan evidencia;
- escritorio, móvil, teclado y lector de pantalla soportan los flujos principales;
- las proyecciones pueden reconstruirse y su latencia es observable.

## Decisión final de arquitectura

SIAJ 2.0 debe evolucionar como un **monolito modular orientado a dominios, con escritura transaccional, eventos auditables y proyecciones de lectura**. Expedientes, Términos y Audiencias serán las fuentes operativas; Historial y dashboards serán vistas derivadas; Trazabilidad será la evidencia inmutable; Catálogos gobernará vocabularios; Administración gobernará personas, políticas y calendarios.

Este diseño preserva la simplicidad operativa actual, elimina ambigüedad entre módulos y deja una ruta de escalamiento futuro sin introducir distribución técnica prematura.
