# Matriz GAP funcional SIAJ y cambios para layouts V1

Fecha: 8 de septiembre de 2026.

## Alcance y criterio de clasificación

Contraste entre los cinco layouts, las pantallas representadas en el código local, el modelo JPA/dump SQL y las necesidades operativas descritas. Los procesos del abogado se presentan como necesidades funcionales y consecuencias previsibles: no se observaron sesiones de usuarios ni se realizaron entrevistas. Tampoco se ejecutó una migración ni se consultó el esquema desplegado.

**Bloqueante para salida a producción:** puede perder o alterar información, romper referencias, impedir una operación necesaria o presentar como cumplida una función esencial que no lo está. **Importante:** afecta calidad u operación, pero admite una alternativa controlada. **Deseable:** mejora uso y mantenimiento sin impedir la operación básica.

La clasificación se refiere a una salida con migración histórica y operación de términos/audiencias. Donde el riesgo depende del alcance, se especifica la condición. Un requerimiento nuevo propuesto no se presenta como obligación existente de la BD. JPA, dump y ambiente desplegado no se consideran equivalentes automáticamente.

**Precisión respecto del informe anterior:** la tabla de audiencias ya muestra `#ID` y las consultas del repositorio incluyen búsqueda por ID. Un nuevo folio no es requisito para identificar unívocamente una audiencia dentro de la instalación. La brecha real es la falta de equivalencia con la clave histórica y la claridad de la identificación en todas las vistas.

## Matriz GAP

Categorías: **1** información necesaria no capturada; **2** información capturada sin consumo/resolución confirmado; **3** catálogo faltante; **4** relación faltante; **5** redundancia; **6** ambigüedad; **7** riesgo de migración; **8** riesgo operativo posterior. Un hallazgo puede pertenecer a varias categorías.

| ID / categoría | Layouts de migración | Pantallas del sistema | Modelo de BD / persistencia | Proceso del abogado y GAP | Riesgo de migración / riesgo posterior | Clasificación |
|---|---|---|---|---|---|---|
| G01 · 2,7 | Primera hoja CATALOGOS; encabezados operativos en fila 5. | Descargas administrativas ofrecen otro contrato. | Lector legado usa primera hoja y omite fila 1; servicio Excel de audiencias no persiste registros. | El paquete de captura no tiene un flujo compatible completo confirmado. | Lectura de catálogos como datos, carga fallida o aparente éxito sin registros. | Bloqueante para salida a producción |
| G02 · 2,4,7,8 | clave_origen y expediente_clave_origen relacionan archivos. | Operación usa IDs destino; no consume esas claves históricas. | No se encontró resolución persistente de estas claves en el contrato revisado. | Recuperar una actuación y adjuntar evidencia después exige equivalencia fuente–destino. | Hijos mal vinculados, duplicación en recargas y documentos asociados a otro registro. | Bloqueante para salida a producción |
| G03 · 2,3,6,7 | *_codigo y listas de ejemplo repetidas. | Pantallas seleccionan IDs/nombres de catálogos. | Entidades como Materia tienen ID/nombre, sin el código propuesto del Excel. | Homologar conceptos antes de capturar; código no equivale automáticamente a ID ni a nombre. | Referencias sin resolver y clasificaciones inconsistentes en consulta. | Bloqueante para salida a producción |
| G04 · 1,4,7,8 | Email opcional en Usuarios; responsables referenciados sólo por email. | Sistema admite usuarios locales sin correo. | Usuario posee ID y otros atributos de identidad. | Asignar responsable no debe depender de que tenga correo. | Usuarios válidos quedan sin referencia o se inventan correos; asignaciones/notificaciones incorrectas. | Bloqueante para salida a producción |
| G05 · 1,4,8 | Una gerencia por usuario; materias/extras en texto con punto y coma. | Gestión de usuarios y alcance organizacional. | Usuario tiene relación con varias gerencias y materias. | Una persona puede trabajar en varios ámbitos; el desplegable simple no representa múltiples asignaciones. | Duplicación de cuentas o pérdida de acceso a asuntos de otra gerencia. | Importante |
| G06 · 1,6,8 | Responsables deben estar activos. | Cuentas habilitadas determinan acceso operativo. | Puede conservarse una cuenta inactiva, pero atribución histórica y asignación actual no son lo mismo. | Preservar quién actuó y definir quién atenderá los pendientes. | Reactivación innecesaria de excolaboradores o pérdida de autoría. | Importante |
| G07 · 5,6,7,8 | Roles ADMIN/DIRECTOR_PRINCIPAL disponibles; extras capturados manualmente. | Permisos heredados y gestión de usuarios por autorización. | Servicio calcula base + extras; DIRECTOR_PRINCIPAL no aparece en su matriz base aunque otras rutas reconocen el rol. | Sólo personal autorizado debe asignar privilegios; extras no deben duplicar la base. | Asignaciones excesivas o diferencias de acceso entre rutas. | Bloqueante para salida a producción |
| G08 · 1,6,8 | Tipo de expediente opcional. | Alta y edición lo marcan required. | FK tipo_expediente_id nullable en dump/modelo; controlador lo asigna si viene. | Un expediente sin tipo puede persistir, pero el formulario exige completarlo para guardar. | Retrabajo al editar; sin catálogo disponible no puede satisfacerse el formulario ordinario. | Importante |
| G09 · 1,3,4,8 | Términos no captura tipo_termino_codigo; Catálogos no incluye TIPOS_TERMINO. | Hay catálogo y selector de Tipo de Término separado del asunto. | JPA tiene tipos_termino y tipo_termino_id; tabla no encontrada en dump revisado. | Clasificar actuación y asociar regla temporal. | Históricos sin relación al tipo; despliegue potencialmente incompleto. | Bloqueante para salida a producción |
| G10 · 4,8 | Añadir una columna por sí solo no completa la función. | Selector envía tipoTerminoId. | Entidad espera tipoTerminoRef y controlador no resuelve explícitamente aquel parámetro. | La selección debe sobrevivir al guardado y edición. | Usuario cree haber asociado una regla que no queda vinculada en el flujo revisado. | Bloqueante para salida a producción |
| G11 · 1,3,6,8 | No hay modalidad, fecha base, calendario ni versión de regla temporal. | Controles de plazo fijo/dinámico y configuración por tipo. | Campos parciales; no se encontró cálculo que use la bandera dinámica para obtener vencimiento. | Conocer cuándo vence y por qué. La fecha de ingreso no es automáticamente fecha de inicio de plazo. | Cálculo esperado no implementado; alertas no confiables si se promete automatización. | Bloqueante para salida a producción |
| G12 · 2,8 | Tipos de audiencia no captura umbrales que existen en modelo. | Existen configuraciones temporales. | TipoAudiencia tiene umbrales; método getSemaforoColor de Audiencia usa constantes 1/3 días. | La regla aprobada debe producir el mismo resultado en cada vista/alerta. | Configuración que no gobierna el método revisado y percepción errónea de urgencia. | Bloqueante para salida a producción |
| G13 · 1,2,6,7,8 | Captura ingreso, vencimiento, presentación y estados históricos. | Flujos ordinarios usan hoy para ciertos eventos; es válido sólo para hechos actuales. | Son fechas distintas; ruta histórica concluye por antigüedad y sustituye fechas inválidas por hoy. | Distinguir pendiente vencido, presentado y concluido. | Hechos históricos alterados y pendientes que desaparecen de la operación. | Bloqueante para salida a producción |
| G14 · 1,3,6,8 | Diccionario dice CONCLUIDO por presentación, sin motivo de conclusión. | Presentación y conclusión son acciones distintas. | Estatus y fecha no explican todos los motivos de cierre. | Distinguir cierre por presentación de otras causas. | Fecha inventada o informes de cumplimiento incorrectos. | Importante |
| G15 · 1,3,4,7,8 | Acta/acuse condicionales; no hay estado documental ni manifiesto multianexo. | Subida posterior existe; conclusión de audiencia revisada no exige acta. | Rutas documentales no sustituyen disponibilidad, motivo ni binarios. | Conservar situación procesal aunque el cliente no entregue evidencia. | PDF sustituto registrado como evidencia real; documentos faltantes ocultos. | Bloqueante para salida a producción |
| G16 · 1,4,7,8 | Referencias de archivos relativas sin transmisión de binarios en la celda. | Gestor y cargas individuales permiten consultar documentos reales. | Almacenamiento y vínculo deben conciliarse; un nombre no prueba existencia. | Recibir, localizar y completar documentación por actuación. | Archivos huérfanos, cruces incorrectos y fallas al abrir evidencia. | Bloqueante para salida a producción |
| G17 · 4,6,8 | sala_lugar libre; órgano sólo en expediente. | Tabla muestra sala si existe, o autoridad del expediente como alternativa; virtual muestra modalidad. | Audiencia no tiene FK directa a órgano; expediente sí. | Distinguir autoridad competente y recinto; decidir si pueden variar entre actuaciones. | Ubicación ambigua o interpretación histórica alterada al cambiar órgano del expediente. | Importante |
| G18 · 3,4,5,6 | ORGANOS sí existe; tipo libre y sede repetida también en expediente. | Se gestionan órganos y sedes. | Tipo/sede de órgano y sede de expediente son texto; límites distintos a Excel. | Sede territorial, tipo de autoridad y órgano concreto requieren definiciones diferentes. | Duplicados, sede contradictoria y valores que exceden el destino. | Importante |
| G19 · 1,3,4,6,8 | Partes en texto, ejemplo Actor/Demandado. | Campo de texto de partes. | Expediente guarda TEXT, sin relación estructurada por calidad procesal. | Identificar personas y su participación, incluidas varias partes y terceros. | Búsqueda libre posible; filtros/reportes fiables por calidad no garantizados. | Importante |
| G20 · 6,7,8 | Número oficial y clave de origen separados. | Usuario reconoce expediente por número/partes; alta incluye reglas de duplicidad. | UUID identifica entidad; número no tiene unicidad global en dump. Rutas de carga buscan por número. | No confundir expedientes homónimos de distintos órganos/fuentes. | Fusión incorrecta de actuaciones y responsables. | Bloqueante para salida a producción |
| G21 · 5,6,8 | Audiencia sólo tiene clave_origen como identificador de captura. | Tabla muestra #ID, expediente, tipo, fecha/hora, abogado y estado; repositorio busca por ID. | ID entero generado, estable dentro del destino. | Ya existe identificación unívoca; falta puente con origen y presentación coherente. | No es necesario inventar otro identificador para operar; sí evitar confundir tipo/fecha con identidad. | Deseable |
| G22 · 5,6 | asunto de término es descripción; nombre/código de materia pueden parecer iguales. | Tipo y actuación son controles separados. | Asunto TEXT y FK de tipo son distintos. | “Presentar contestación del expediente X” no equivale al tipo “Contestación”. | Eliminar/renombrar sin distinguir conceptos pierde contenido útil. | Importante |
| G23 · 5,6,8 | Prioridad en expediente/término; misma lista en todos los libros. | Tareas usa prioridad; audiencia no tiene selector equivalente confirmado en el modelo. | CatPrioridad compartido, FK por expediente/término/recordatorio; DTO normal/urgent. | Importancia del asunto puede diferir de urgencia de una actuación. | Propagación indebida, equivalencias erróneas y confusión prioridad–semáforo. | Importante |
| G24 · 5,7,8 | Catálogos de consulta replicados; 200 filas validadas; error de instrucción CAPTURA en Catálogos. | Importación debe indicar versión y errores por fila. | Excel no aplica integridad referencial al destino. | Capturar lotes grandes con reglas claras. | Valores libres y referencias obsoletas; retrabajo si el servidor los detecta tarde. | Importante |
| G25 · 7,8 | Claves distintas pueden corresponder a datos parecidos. | Mensajes de lote completado no prueban carga íntegra. | Ruta puede marcar PROCESADO tras capturar una excepción. | Saber exactamente qué información quedó disponible. | Omisiones silenciosas y falsa aceptación del lote. | Bloqueante para salida a producción |
| G26 · 1,4,7,8 | No representa toda la historia, contextos/asuntos únicos ni reprogramaciones. | Sistema tiene contextos y operaciones que exceden los cinco archivos. | Entidades de históricos/contextos separadas. | Conservar alcance histórico acordado y no crear expedientes ficticios sin regla. | Pérdida de cronología si se promete migración completa. | Importante |
| G27 · 8 | No define responsable de pendientes ni corte de notificaciones. | Agenda, alertas y asignaciones operan sobre datos importados. | Usuarios/fechas/estados activan procesos posteriores según su implementación. | Abrir producción con cartera conciliada y responsables vigentes. | Riesgo por verificar de avisos masivos, tareas sin atender o avisos históricos. | Importante |

G08 pasa a bloqueante si el catálogo queda vacío y se mantiene el formulario obligatorio. G11 puede diferirse sólo si se acuerda expresamente una V1 con vencimientos aportados/validados por el cliente, se desactiva la promesa de cálculo automático y existe un procedimiento operativo aceptado. G26 pasa a bloqueante si la cronología omitida forma parte contractual de la migración. Estas condiciones no equivalen a aprobación ya otorgada.

## Lectura por las ocho categorías solicitadas

| Categoría | Hallazgos principales | Interpretación |
|---|---|---|
| 1. Sistema/proceso requiere y layout no captura | G04–G06, G08–G09, G11, G14–G16, G19, G26 | Tipo de expediente sí está capturado, pero no exigido; tipo de término falta. Estado documental, calidad procesal y calendario son necesidades propuestas, no todos requisitos NOT NULL actuales. |
| 2. Layout captura y sistema no utiliza/resuelve | G01–G03, G12–G13 | Claves/códigos carecen de resolución para este contrato. Estados/fechas existen en BD pero no se preservan íntegramente en la ruta histórica revisada. No son campos inútiles: requieren integración. |
| 3. Catálogos faltantes | G03, G09, G11, G14–G15, G18–G19 | TIPOS_TERMINO falta en Excel, existe en código. Propuestos: tipo de órgano controlado, calidad procesal, disponibilidad documental y motivo de cierre; calendario/reglas si se calcularán plazos. ORGANOS y TIPOS_EXPEDIENTE ya existen. |
| 4. Relaciones faltantes | G02, G04–G06, G09–G10, G15–G19, G26 | Algunas existen en BD y falta expresarlas/resolverlas en Excel; otras, como parte-calidad o órgano propio de audiencia, requieren modelado si se adoptan. |
| 5. Redundantes | G07, G18, G21–G24 | Repetir permisos base como extras sí es evitable. Copias de catálogo, sede y tipo repetido en la tabla de audiencia necesitan control/presentación; no se deben borrar fechas o identificadores distintos. |
| 6. Ambiguos | G03, G06, G08, G11, G13–G14, G17–G23 | Código/nombre, ingreso/notificación/carga, fijo/dinámico, sala/órgano/sede, asunto/tipo y prioridad/semáforo requieren definiciones. |
| 7. Migración | G01–G04, G07, G13, G15–G16, G20, G24–G26 | Contrato incompatible, datos alterados, duplicidad, adjuntos faltantes y falso éxito. |
| 8. Operación posterior | G04–G21, G23–G27 | Responsables/accesos, edición, alertas, identificación, evidencia e historia. |

**No son redundantes:** clave_origen, número oficial e ID destino; ingreso, vencimiento y presentación; asunto y tipo de término; materia y tipo de expediente. Tampoco son automáticamente redundantes sede de órgano y sede de expediente: primero debe aclararse si describen el mismo hecho.

## Cambios priorizados para V1

Orden de ejecución recomendado. **Sí** indica modificación necesaria para la solución propuesta. **Cond.** indica que depende de la decisión indicada o del esquema desplegado. **No** significa que no se justifica un cambio en ese componente para esa solución concreta.

| Orden / GAP | Cambio recomendado y aceptación | Excel | Backend | Base de Datos | UI | Proceso Operativo |
|---|---|---|---|---|---|---|
| 01 · G01,G24 | Contrato V1 único: hojas por nombre, encabezados y versión; descargas OOXML reales; lectura de los cinco libros de prueba sin confundir CATALOGOS con captura. | Sí | Sí | No | Sí | Sí |
| 02 · G02,G20 | Claves estables y tabla de equivalencias fuente–entidad–clave–ID. Repetir lote no duplica; homónimos no se fusionan automáticamente. | Sí | Sí | Sí | Cond.: consulta de equivalencias | Sí |
| 03 · G03,G18 | Maestro aprobado por ámbito; generar listas código/nombre/vigencia; resolver códigos a IDs y rechazar referencias ambiguas. | Sí | Sí | Sí: equivalencias; unicidad condicional | Cond.: edición/exportación | Sí |
| 04 · G04–G06 | usuario_clave_origen y referencias por clave; hojas de asignaciones multigerencia/materia. Preservar autor histórico y asignar responsable actual sin reactivar exusuarios. | Sí | Sí | Cond.: equivalencias y autoría adicional | Cond.: mostrar ambos responsables | Sí |
| 05 · G07 | Excluir ADMIN de captura general; política para Director Principal; validar extras y alcance como en operación ordinaria. | Sí | Sí | No | Sí | Sí |
| 06 · G13,G20,G25 | Carga sin fechas inventadas, truncamientos ni cierre por antigüedad; resultado por fila y conciliación real. | Sí: diccionario/errores | Sí | Cond.: detalle/estado de lote | Sí | Sí |
| 07 · G09–G10 | Agregar TIPOS_TERMINO y tipo_termino_codigo; conservar asunto. Guardar/editar/importar la FK correctamente y validar tabla destino. | Sí | Sí | Cond.: desplegar tabla/columnas faltantes | Sí | Sí |
| 08 · G11–G12 | Decidir modalidad temporal V1. Unificar semáforos por regla y calendario; preservar vencimiento histórico. Si se difiere cálculo, reflejar limitación en captura/UI y procedimiento. | Sí | Sí | Cond.: calendario/versionado | Sí | Sí |
| 09 · G14–G16 | Estado documental separado, motivo de faltante/cierre, manifiesto y carga posterior por clave; no aceptar constancia de faltante como acta/acuse. | Sí | Sí | Sí: estado/motivo o metadatos equivalentes | Sí | Sí |
| 10 · G08 | Alinear tipo_expediente_codigo: obligatorio para nuevos; histórico con valor homologado o Sin clasificar aprobado. Debe poder editarse tras migrar. | Sí | Sí | No: basta catálogo existente | Sí | Sí |
| 11 · G17–G18 | Conservar sala_lugar; definir sede/tipo/órgano y validar consistencia/longitudes. Agregar órgano de audiencia sólo si existe diferencia real o requisito histórico. | Sí | Sí | Cond.: FK/snapshot/sedes normalizadas | Sí | Sí |
| 12 · G19 | Captura estructurada de partes por calidad si se requieren consultas/reportes por rol. Si se difiere, preservar texto íntegro y documentar limitación. | Sí | Sí si se estructura | Sí si se estructura | Sí si se estructura | Sí |
| 13 · G23 | Mantener prioridad por registro y resolver equivalencia del DTO normal/urgent sin pérdida silenciosa de niveles. | Sí: definiciones | Sí | No | Cond.: unificar opciones | Sí |
| 14 · G24 | Corregir instrucciones, listas multivalor y rangos ampliables; probar más de 200 filas. Etiquetas de permisos comprensibles. | Sí | Sí: validar todas las filas | No | No | Sí |
| 15 · G26–G27 | Definir alcance histórico, corte, notificaciones, responsables de pendientes y conciliación de inicio de operación. | Sí: guía/anexos según alcance | Sí: controles según flujo | Cond.: historia adicional | Cond.: consulta/pendientes | Sí |
| 16 · G21 | Usar el #ID existente como referencia operativa; etiqueta “ID de audiencia”, copia fácil y, opcionalmente, formato AUD-000123 derivado. | No: ID lo genera destino | Cond.: formato/búsqueda | No si se deriva del ID | Sí | Sí |

En esta tabla cambiar registros de un catálogo existente no se contabiliza como cambio de esquema BD. No se propone añadir NOT NULL a tipo_expediente_id sin sanear primero los históricos y validar la política de negocio.

### Contenido mínimo recomendado de los cinco libros V1

1. **Catálogos:** conservar hojas actuales; incorporar TIPOS_TERMINO, códigos/nombres/ámbitos estables y versión del maestro. Añadir vocabularios documentales y motivos aprobados donde corresponda. No convertir roles/estados cerrados en listas libres del cliente.
2. **Usuarios:** usuario_clave_origen, datos de contacto y rol; asignaciones de gerencia/materia/extras en hojas relacionales o mecanismo multivalor validado. No repetir una cuenta por gerencia.
3. **Expedientes:** conservar clave_origen, numero_expediente, materia_codigo, tipo_expediente_codigo, órgano/sede, descripción y partes. Sustituir dependencia exclusiva del correo por clave de usuario. Alinear tipo obligatorio/excepción histórica.
4. **Audiencias:** conservar clave_origen, expediente_clave_origen, tipo, fecha/hora, recinto, modalidad, responsables, estado y desahogo. Añadir disponibilidad documental/manifiesto. Órgano propio sólo según decisión funcional. El ID destino se devuelve en la conciliación, no se pide inventarlo al abogado.
5. **Términos:** conservar asunto y las tres fechas; agregar tipo_termino_codigo, responsable por clave, motivo de cierre cuando aplique y disponibilidad del acuse. Campos de cálculo sólo cuando la modalidad V1 esté definida e implementada.

Regla transversal: adjuntar reporte de equivalencias y errores al finalizar cada lote. Las hojas auxiliares pueden mantenerse dentro de los cinco archivos; no es necesario multiplicar archivos para representar relaciones.

## ¿Qué funcionalidad se rompe si no hay Tipo de Expediente?

Hay que distinguir tres escenarios:

| Escenario | Consecuencia confirmada o sustentada por código |
|---|---|
| Un expediente migrado tiene tipo vacío | Modelo/dump permiten NULL. La aplicación puede persistirlo por rutas que no lo exijan. Al editar mediante el formulario que tiene required, el usuario debe seleccionar un tipo antes de guardar. |
| El catálogo de tipos está vacío | Alta y edición ordinarias no ofrecen una selección válida para satisfacer el campo obligatorio. Éste sí es un bloqueo de la operación por formularios mientras no se cargue el catálogo o se ajuste la regla. |
| Se elimina completamente el concepto del sistema | Se pierde clasificación y se deben adaptar formularios, controladores y FK. No basta retirar la columna del Excel. |

**No se encontró evidencia de que la ausencia de Tipo de Expediente rompa el cálculo de términos, la programación de audiencias, la carga de documentos o los permisos.** Las referencias revisadas lo usan como clasificación y dato de alta/edición; no prueban un motor de reglas dependiente del tipo. No corresponde justificar su obligatoriedad inventando efectos en esos módulos.

Conclusión para V1: conservarlo y alinear el contrato. Si el cliente no puede clasificar históricos, aprobar “Sin clasificar” con regularización o permitir ausencia de forma coherente en Excel/backend/UI. No asignar “Juicio” a todos sólo para pasar validaciones. Su clasificación general es **Importante**; catálogo vacío con formulario obligatorio es **Bloqueante para salida a producción**.

## ¿Cómo identifica un abogado una audiencia en su operación diaria?

Según la implementación revisada, cuenta con dos niveles:

- **Identidad inequívoca:** `#ID` generado por SIAJ, visible en la tabla. El repositorio incluye búsqueda por ID y filtro exacto por ID en las consultas revisadas.
- **Reconocimiento del asunto:** número de expediente, tipo de audiencia, fecha y hora; complementa con ubicación/modalidad, abogado y estado. También hay búsqueda por partes/descripción y otros atributos en las consultas revisadas.

Ejemplo de referencia operativa: **“Audiencia #123 · expediente 456/2024 · Conciliación · 20/09/2026 a las 10:30”**. El abogado ubica el asunto por datos de negocio y confirma el ID antes de subir acta, delegar o modificar. Este es un procedimiento recomendado a partir de las capacidades encontradas, no observación de uso real.

`clave_origen` sirve para enlazar la fuente histórica; no es hoy el identificador operativo persistido de la audiencia. No debe pedirse al abogado memorizarla para trabajar. El reporte de migración debe relacionarla con #ID.

Hay dos mejoras concretas de presentación: el tipo aparece tanto bajo “Asunto / Expediente” como en “Tipo”, en vez de aportar una descripción adicional; y la ubicación alterna sala y órgano, aunque son conceptos distintos. Mostrar **expediente + partes o descripción breve**, **tipo**, **órgano**, **recinto/modalidad**, **fecha/hora** e **ID** facilitaría distinguir audiencias similares.

Un folio `AUD-000123` puede ser sólo presentación del ID existente, sin otra columna BD. Si se requieren identificadores portables entre instalaciones, diseñar ese alcance aparte. Fecha/hora no son identidad estable: pueden cambiar por reprogramación y puede haber actuaciones coincidentes.

## Condiciones para liberar V1

- Contrato Excel/importador/destino probado con los cinco archivos y documentos representativos.
- Catálogos y equivalencias aprobados; reintento sin duplicar y referencias ambiguas rechazadas.
- Estados, fechas, responsables y documentos conciliados contra destino, sin transformaciones silenciosas.
- Alta/edición de expedientes y asociación de tipo de término verificadas después de migrar.
- Modalidad temporal y tratamiento de evidencia faltante aprobados e implementados para el alcance anunciado.
- Inventario de pendientes con responsable actual; corte y notificaciones de inicio definidos.

## Fuentes

El [informe funcional base](./Revision_funcional_layouts_SIAJ.md) contiene localizadores E01–E18 de los cinco Excel, entidades, importadores, pantallas y dump. Se reutilizó su inspección sin modificar los originales.

Verificación adicional para este GAP:

- `src/main/resources/templates/fragments/audiencias/tabla.html:13`: encabezado #ID; `:40`: valor a.id; `:48`: tipo/expediente; `:76`: alternativa sala/órgano.
- `src/main/java/com/juridico/sistema_juridico/repository/procesal/AudienciaRepository.java:34`: búsqueda por expediente, partes y otros atributos; `:49`: búsqueda por a.id; `:60`: filtro exacto por ID.
- `src/main/resources/templates/fragments/expedientes/modal-nuevo-exp.html:104` y `modales-detalle.html:143`: tipo requerido en formularios.
- `src/main/java/com/juridico/sistema_juridico/controller/ExpedientesController.java:409`: asignación condicional del tipo; `:471`: persistencia en otra rama.
- `src/main/java/com/juridico/sistema_juridico/Entity/expediente/Expediente.java:72`: relación a tipo; `db_schema_actual.sql:944`: FK nullable; `:2902`: restricción referencial.

No se afirma que se haya probado comportamiento interactivo en navegador/Excel ni cumplimiento contra producción. Las conclusiones de flujo proceden de inspección estática y las decisiones de diseño se identifican como propuestas.
