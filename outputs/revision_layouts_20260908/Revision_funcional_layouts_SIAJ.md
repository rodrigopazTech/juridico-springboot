# Revisión funcional de layouts de carga histórica SIAJ

Fecha: 8 de septiembre de 2026.

## Dictamen

**No recomiendo liberar los cinco archivos como layouts definitivos de migración todavía.** La estructura permite preparar información, pero existen diferencias entre el contrato Excel, las pantallas y los importadores que pueden producir pérdidas, asociaciones incorrectas y estados históricos falsos.

La revisión incluyó los cinco `Layout_*.xlsx` de `Layouts_Carga_Masiva_SIAJ`, sus hojas de captura, instrucciones, diccionarios y validaciones; entidades Java, controladores, servicios, plantillas HTML y el dump `db_schema_actual.sql`. No se modificaron los Excel ni el sistema. No se ejecutó una carga ni se consultó la base de datos desplegada: los hallazgos de implementación corresponden al código local, y las diferencias del dump deben verificarse en el ambiente destino. La presencia de una pantalla o columna no demuestra que el flujo completo funcione.

Correcciones relevantes a las premisas iniciales:

- `ADMIN` y `DIRECTOR_PRINCIPAL` aparecen en la columna **ROLES**, no como registros de usuarios en los Excel. La columna USUARIOS contiene un correo de ejemplo.
- Los órganos jurisdiccionales ya tienen una hoja **ORGANOS** en `Layout_Catalogos.xlsx`.
- **Tipos de Término sí existe en el código y en la pantalla de Catálogos**, pero falta en los layouts.
- `asunto` y tipo de término son conceptos distintos y la pantalla actual los muestra por separado.
- Las prioridades se comparten entre expedientes, términos y tareas/recordatorios. Audiencia no tiene una relación directa con ese catálogo en el modelo revisado.

## Matriz de análisis

Prioridades: **Crítica** = resolver antes de migrar; **Importante** = afecta operación o calidad; **Deseable** = mejora usabilidad/mantenimiento. En Recomendación se indican los componentes afectados: **L** layout/diccionario/guía, **C** catálogo, **BD** base de datos, **S** lógica/pantallas/importador. “BD condicional” significa que depende de la decisión funcional o del esquema desplegado. Las referencias E01–E18 se detallan al final.

| Tema | Situación Actual | Riesgo Detectado | Impacto | Recomendación | Prioridad |
|---|---|---|---|---|---|
| 1. Secuencia de los cinco archivos | Usuarios depende de gerencias/materias; expedientes de catálogos/usuarios; audiencias y términos de expedientes. La guía sólo detalla parte de la secuencia. E01–E05 | Captura prematura de referencias aún no aprobadas. | Retrabajo y registros sin vinculación. | Catálogos → Usuarios → Expedientes → Audiencias y Términos; estos dos últimos no dependen entre sí. Publicar guía y controles entre etapas. **L/S**. Dependencias: todos los módulos. | Importante |
| 2. Instrucciones de Catálogos | Dice capturar sólo en CAPTURA, pero ese libro tiene siete hojas de captura y ninguna llamada CAPTURA. E01 | Instrucción imposible de seguir. | Dudas y llenado de hojas equivocadas. | Enumerar hojas editables y ordenar SEDES antes de ORGANOS. **L**. | Importante |
| 3. Catálogos repetidos | Las cinco pestañas CATALOGOS contienen los mismos valores de ejemplo, no una exportación confirmada del destino. E01–E05 | Copias divergentes y ejemplos utilizados como datos reales. | Referencias rechazadas o asociaciones incorrectas. | Maestro aprobado, códigos estables y copias generadas automáticamente por versión/gerencia. Mantener copias locales de consulta. **L/C/S**. | Crítica |
| 4. Gobierno y ámbito de catálogos | Hay catálogos por gerencia, otros compartidos y restricciones globales de nombre en varios modelos. E06/E09/E12 | Cada gerencia puede crear equivalentes o nombres que colisionan globalmente. | Duplicidad, fallas de carga y fragmentación de reportes. | Cliente valida significado/propiedad; implementación homologa y administra códigos. Definir unicidad global o por ámbito antes de cargar. **L/C/S; BD si cambia la unicidad**. | Crítica |
| 5. Controles de Excel | Validaciones limitadas a filas 6–205, con rangos fijos; `showErrorMessage=false`. Varias listas sólo tienen un ejemplo. E01–E05 | Más de 200 registros quedan fuera de esos controles; escribir valores no listados no muestra rechazo configurado. | Falsa sensación de validación. | Rangos ampliables, listas actualizadas, mensajes de error y prevalidación completa en servidor. Excel es ayuda de captura. **L/S**. | Importante |
| 6. Roles administrativos | ADMIN y DIRECTOR_PRINCIPAL son roles válidos del sistema. E02/E10 | Exponerlos como opciones ordinarias puede provocar solicitudes de privilegios inapropiados. | Asignaciones indebidas y confusión sobre responsables. | Excluir ADMIN del layout operativo; definir si Director Principal es autoridad real del cliente y aprovisionarlo de forma controlada. No borrar roles ni cuentas técnicas de BD. **L/C/S**. | Crítica |
| 7. Permisos heredados | El servicio suma permisos base del rol y adicionales; elimina redundancias al guardar extras. E10 | Capturar todos como extras desvirtúa la herencia; DIRECTOR_PRINCIPAL no está en la matriz base de ese servicio. | Permisos difíciles de auditar y posible inconsistencia de acceso según la ruta. | Mostrar permisos heredados como consulta y capturar sólo extras aprobados. Revisar el tratamiento del rol principal en toda la autorización. **L/S**, sin cambio BD necesario para herencia. | Importante |
| 8. Nombres de permisos | Excel expone códigos `_SUPERVISION`; el enum ya tiene títulos/descripciones. E10 | Usuario interpreta supervisar como sólo consultar aunque algunas facultades incluyen gestión. | Consentimiento y asignación poco claros. | Mostrar nombre comprensible, acciones y ámbito; conservar código técnico estable. **L/C/S**. Dependencias: seguridad y todos los módulos operativos. | Importante |
| 9. Campos con varios valores | materias_codigos y permisos_adicionales piden listas con punto y coma, pero usan desplegables de selección simple. E02 | La ayuda de captura no permite construir directamente la lista documentada. | Omisiones, separadores incorrectos y errores de permisos. | Preferir hojas de asignaciones, una relación por fila, o validar cada elemento del texto y explicar el procedimiento. **L/S**. | Importante |
| 10. Identidad de usuarios | Correo es opcional en Usuarios; otros layouts sólo referencian responsables por correo. E02–E05 | Usuario válido sin correo no puede ser referenciado. | Responsables perdidos o correos ficticios. | Añadir usuario_clave_origen o código estable de usuario en todas las referencias; correo como contacto. **L/S/BD para equivalencias**. | Crítica |
| 11. Usuarios de varias gerencias e históricos | Usuario permite múltiples gerencias; Excel ofrece una. Referencias exigen usuarios activos. E02/E03/E18 | Duplicar una persona por gerencia o reactivar excolaboradores para conservar historia. | Identidades duplicadas, alcance incorrecto y pérdida de atribución. | Una identidad, asignaciones múltiples y separación entre responsable actual y autor histórico inactivo. **L/S; BD condicional**. | Importante |
| 12. clave_origen | Diccionario exige estabilidad y unicidad por archivo/gerencia; no aparece como campo en entidades operativas ni como contrato resuelto en `src/main`. E03–E06/E08 | Reintentos sin identidad persistente; colisiones entre fuentes. | Duplicados y documentos/hijos vinculados al registro equivocado. | Equivalencias persistentes por fuente, entidad y clave, con ámbito acordado; validar también contra cargas previas. **L/S/BD**. | Crítica |
| 13. Número de expediente | Número oficial textual y UUID interno generado son distintos; el número tiene índice, no unicidad global. E06 | Fusionar expedientes sólo por número. | Mezcla de partes, actuaciones y responsables. | Conservar número oficial, clave de origen y UUID. Homologar con órgano/gerencia y revisión de coincidencias ambiguas. **L/S/BD para equivalencias**. | Crítica |
| 14. materia_codigo | Catálogos separa código LABORAL y nombre Laboral; Materia tiene nombre e ID, sin código en JPA. E01/E03/E06 | Suponer que el código se guarda directamente o que el nombre es identidad. | Referencias no resueltas y errores por cambios de nombre. | Mantener materia_codigo en Excel y mapear a ID; nombre visible auxiliar. **L/C/S; BD si se persiste código o equivalencia**. | Importante |
| 15. Tipo de expediente | Está en alta y edición y se persiste; HTML lo exige, Excel lo declara opcional. E03/E06 | Registro migrado aceptado, pero difícil de editar sin completar otro dato. | Inconsistencia operativa. | Conservar como clasificación; alinear obligatoriedad o aprobar “Sin clasificar” y regularización. No inventar automatismos por tipo. **L/C/S**. | Importante |
| 16. Partes procesales | Un campo TEXT `partes`; el ejemplo etiqueta Actor/Demandado pero no hay relaciones estructuradas en Expediente. E03/E06 | Etiquetas inconsistentes y cardinalidad no controlada. | No se garantizan filtros, reportes o vinculación por calidad procesal. | Hoja PARTES con expediente, persona y calidad procesal, múltiples filas; mantener original. **L/C/S/BD**. | Importante |
| 17. sala_lugar y órgano | Audiencia guarda recinto libre; órgano se relaciona con expediente, no directamente con audiencia. E04/E06/E07 | Sustituir recinto por autoridad pierde información; heredar órgano actual puede alterar la interpretación histórica. | Ubicación confusa y trazabilidad incompleta. | Conservar sala_lugar; agregar órgano de audiencia sólo si puede diferir o debe congelarse históricamente. **L/S; BD para relación/snapshot adicional**. | Importante |
| 18. Catálogo de órganos | ORGANOS ya captura código, nombre, sede, tipo y gerencia. E01 | Crear otro catálogo paralelo innecesario. | Duplicidad y mantenimiento inconsistente. | Usar la hoja existente; homologar antes de expedientes. Separar archivo sólo por responsabilidad o volumen. **L/C/S**. | Importante |
| 19. Sede, tipo y órgano | Órgano es autoridad; sede y tipo son texto en modelo. Expediente también guarda sede como texto. E06/E09 | Valores contradictorios, longitudes incompatibles o sedes entendidas como oficinas internas. | Filtros imprecisos y errores de ubicación. | Definir sede jurisdiccional, catálogo de tipos y relaciones explícitas si se requiere integridad. Validar longitudes destino. **L/C/S/BD**. | Importante |
| 20. Actas y carga posterior | Excel exige acta para CON_ACTA y fecha para CONCLUIDA; existe subida posterior. El método de conclusión revisado no exige acta. E04/E07 | Estado documental mezclado con estado procesal; reglas distintas por ruta. | Audiencia aparentemente acreditada sin documento o imposibilidad de reflejar historia. | Estado documental independiente y excepción histórica aprobada; cargar binarios después por clave. **L/S/BD**. | Crítica |
| 21. PDF de sustitución | Un archivo indicado como acta puede satisfacer formalmente el campo sin acreditar la actuación. E04/E07 | “Documento no disponible” termina registrado como acta real. | Evidencia engañosa, indicadores y validaciones incorrectos. | No usarlo como acta/acuse. Si se conserva constancia de faltante, tipificarla separadamente y mantener pendiente/no entregado. **L/C/S; BD para estado/tipo**. | Crítica |
| 22. Entrega documental masiva | Excel contiene nombres relativos; una ruta no transmite binarios. E04/E05/E08 | Archivos huérfanos, faltantes, duplicados o asignados a otra actuación. | Expediente digital incompleto y retrabajo. | Manifiesto, paquete por lote, hash, inventario y conciliación; digitalización con control de calidad para físicos. **L/S; BD para trazabilidad documental**. | Importante |
| 23. Asunto y tipo de término | Pantalla separa tipo y actuación; Excel sólo tiene asunto. E05/E11 | Renombrar asunto a tipo perdería la descripción sin resolver la clasificación. | Históricos sin regla ni reportes por tipo. | Conservar asunto como “Actuación / descripción” y añadir tipo_termino_codigo. **L/C/S**. | Crítica |
| 24. Catálogo de Tipos de Término | Existe entidad `tipos_termino`, servicio CRUD y pestaña; no aparece en los cinco Excel ni su tabla en el dump revisado. E11/E17 | Captura incompleta y despliegue no verificado. | Carga sin clasificación o errores si faltan tabla/columnas. | Añadir hoja TIPOS_TERMINO, exportar catálogo vigente y verificar esquema desplegado. **L/C/S; BD según despliegue**. | Crítica |
| 25. Guardado del tipo de término | HTML envía tipoTerminoId; entidad usa tipoTerminoRef. El controlador no recibe/resuelve aquel parámetro. E11 | La selección no queda vinculada por el flujo revisado. | Apariencia de configuración sin aplicación al registro. | Resolver ID, validar vigencia/ámbito y persistir relación en alta, edición e importación. **S**. | Crítica |
| 26. Reglas fijas/dinámicas | Tipo tiene días y umbrales; registro tiene bandera dinámica. No se encontró motor que use esa bandera para calcular vencimientos. E11 | Confundir controles/modelo con funcionalidad implementada. | Fechas y semáforos que no reflejan la regla elegida. | Definir fecha base, calendario, modalidad, regla y versiones; implementar cálculo verificable. **L/C/S/BD para versionado/calendario**. | Crítica |
| 27. Semáforos de audiencia | TipoAudiencia tiene umbrales, pero Audiencia.getSemaforoColor usa constantes 1/3 días. Hay configuración adicional por sistema/gerencia. E07/E12 | Configuración guardada que no gobierna el cálculo de ese método. | Alertas visuales inconsistentes. | Unificar resolución de reglas por tipo con herencia documentada y usarla en vistas/alertas. **C/S; L para reglas migrables**. | Crítica |
| 28. Tres fechas de término | Ingreso y presentación son campos distintos del vencimiento; alta ordinaria fija ingreso a hoy. E05/E11 | Usar fecha de carga como ingreso histórico o presentación como vencimiento. | Pérdida de cronología y medición incorrecta de cumplimiento. | Preservar las tres fechas con definiciones y validación por estado; auditoría de migración aparte. **L/S**. | Crítica |
| 29. Prioridades por entidad | CatPrioridad es común; expedientes, términos y recordatorios tienen FK propia. Audiencia no. DTO de recordatorios admite normal/urgent. E06/E13 | Presuponer herencia universal o equivalencia de tres niveles con dos. | Urgencia alterada y filtros inconsistentes. | Un catálogo compartido, valores independientes por registro y equivalencia explícita del DTO; no derivar semáforo de prioridad sin regla. **L/C/S**. | Importante |
| 30. Identificación de audiencias | ID interno entero; lista muestra tipo, expediente, fecha/hora; clave_origen no es identificador operativo persistido. E07/E14 | Varias audiencias similares o reprogramadas son difíciles de distinguir. | Selección incorrecta de acta, delegación o actualización. | Folio visible estable, búsqueda por folio y referencia de origen en consulta de migración. **L/S; BD si el folio se almacena**. | Importante |
| 31. Compatibilidad real del importador | Los libros empiezan por CATALOGOS y encabezados de captura en fila 5; lector legado usa primera hoja y omite sólo fila 1. Servicio Excel de audiencias es un esqueleto. E08/E15 | Procesar catálogos como registros o anunciar éxito sin importar. | Carga fallida o corrupta. | Implementar contrato versionado por nombre de hoja/encabezado; probar los cinco libros completos. **S/L**. | Crítica |
| 32. Descargas desde el sistema | AdminImportController genera texto separado por comas con extensión .xlsx, distinto de los Excel reales revisados. E15 | Dos fuentes de plantillas incompatibles. | Excel no abre correctamente o usuario captura otro contrato. | Generar OOXML real con la misma versión oficial y retirar descargas obsoletas. **S**. | Crítica |
| 33. Transformaciones silenciosas | Ruta histórica usa hoy/00:00 para fechas/horas inválidas, trunca textos y determina conclusión por antigüedad. E08 | Cambia hechos históricos sin autorización. | Vencimientos falsos, pendientes ocultos y pérdida de texto. | Rechazar errores por fila; preservar estados, fechas y texto; registrar transformaciones aprobadas. **S/L**. | Crítica |
| 34. Duplicados y conciliación | Ruta histórica busca por número y otras coincidencias parciales; puede marcar PROCESADO después de capturar error. E08 | Omisiones/fusiones y éxito que no acredita persistencia. | Totales engañosos y migración incompleta. | Identidad por clave estable, reintento idempotente y conteos reconciliados con destino; separar error/omitido/importado. **S/BD**. | Crítica |
| 35. Alcance de la migración | Cinco libros no representan asuntos únicos/contextos, historial de etapas, múltiples adjuntos o todas las entidades. E03–E07/E11 | Prometer “migración histórica completa” con sólo estado final. | Pérdida de historia o expedientes ficticios para satisfacer referencias. | Acordar inventario de datos incluidos/excluidos y anexos necesarios. **L/S; BD según historia requerida**. | Importante |

## Secuencia funcional y gobierno de datos

### Orden recomendado de llenado y carga

1. **Layout_Catalogos.xlsx.** Primero GERENCIAS; después MATERIAS y SEDES; luego ORGANOS, TIPOS_EXPEDIENTE, TIPOS_AUDIENCIA y PRIORIDADES. Incorporar TIPOS_TERMINO antes de términos. PRIORIDADES puede aprobarse globalmente desde el inicio. ORGANOS depende de SEDES cuando la sede se informa.
2. **Layout_Usuarios.xlsx.** Definir identidad, rol, gerencias, materias y extras. Resolver una única identidad para personal de varias gerencias y cuentas sin correo.
3. **Layout_Expedientes.xlsx.** Asignar claves estables y homologar número, materia, órgano, tipo, partes y responsable.
4. **Layout_Audiencias.xlsx.** Referenciar expedientes ya identificados y usuarios aprobados.
5. **Layout_Terminos.xlsx.** Referenciar los mismos maestros y su tipo de término. Puede prepararse/cargarse en paralelo con Audiencias una vez resueltos los expedientes.

Preparar borradores en paralelo es posible; liberar o cargar hijos requiere padres validados. Tras Usuarios se deben regenerar las listas de usuarios de los libros operativos. Para evitar ciclos, el creador técnico de catálogos procede de una cuenta de implementación existente, no de una persona pendiente de importar.

Antes de distribuir: congelar gerencias, materias y relaciones, sedes, órganos y tipos de órgano, tipos de expediente/audiencia/término, prioridades, etapas y estados admitidos, roles y permisos permitidos. Para reglas temporales, acordar calendarios y significado de fecha base. Las listas de roles/estados/booleanos son vocabularios del sistema: el cliente no debe inventar valores incompatibles.

La guía debe incluir secuencia, campos obligatorios/condicionales, identificación estable, ejemplos válidos e inválidos, reglas de adjuntos, límite de filas, procedimiento de corrección y responsable de aprobación. La restricción “una gerencia por archivo” debe tener excepción explícita para maestros compartidos y usuarios transversales.

### Catálogo maestro

La duplicación de **copias de consulta** es aceptable para trabajar sin conexiones externas. Lo incorrecto es que cada copia se convierta en una fuente editable independiente. Propongo maestro versionado con código, nombre, activo, ámbito y equivalencia al ID destino; desde él se generan automáticamente sólo las listas pertinentes para cada libro.

El cliente aporta nombres oficiales, significado, vigencia y responsables, y aprueba la homologación. Implementación identifica restricciones, elimina ambigüedades, administra códigos y publica la versión. Los códigos no cambian cuando se corrige el nombre. No se requieren fórmulas enlazadas entre archivos: una generación controlada evita vínculos rotos y versiones no sincronizadas.

## Roles, permisos e identidad

La UI debería mostrar los permisos del rol automáticamente como heredados, sin obligar al cliente a escribirlos ni almacenarlos otra vez como extras. Los adicionales son únicamente facultades fuera de la base, sujetos a aprobación y al ámbito autorizado. El modelo actual suma permisos; no representa revocaciones individuales de permisos heredados.

Propuesta de etiquetas, manteniendo los códigos:

| Código | Nombre visible propuesto |
|---|---|
| EXPEDIENTES_SUPERVISION | Supervisar expedientes del ámbito autorizado |
| TERMINOS_SUPERVISION | Revisar términos de otros equipos autorizados |
| AUDIENCIAS_SUPERVISION | Gestionar audiencias del ámbito autorizado |
| DOCUMENTOS_SUPERVISION | Revisar y administrar documentos del ámbito autorizado |

Cada etiqueta debe acompañarse de acciones concretas y gerencias/materias alcanzadas. “Ver todos” no sería correcto sin comprobar alcance global. Cambiar etiquetas no requiere renombrar permisos en BD.

Retirar ADMIN de las opciones operativas no elimina su existencia técnica. DIRECTOR_PRINCIPAL no debe eliminarse sólo por el nombre: puede representar a una autoridad real del cliente. Debe decidirse quién lo ocupa y aprovisionarlo fuera de la captura general. El servicio de permisos base omite ese rol, mientras otras rutas lo reconocen por rol; esto requiere conciliación funcional, no permite concluir que carezca de todo acceso.

## Identificadores y expedientes

**clave_origen** identifica un registro en la fuente de migración. **numero_expediente** es la referencia oficial del asunto. **id** es el UUID generado por SIAJ. Deben conservarse los tres conceptos, aunque clave_origen puede almacenarse en una tabla de equivalencias y no en el expediente.

Si la fuente ya tiene ID estable, conservarlo. Si no, asignar una vez una clave como `DESP01-EXP-000001`, `DESP01-AUD-000001` y `DESP01-TER-000001`. Los prefijos ayudan a leer, pero no sustituyen restricciones de unicidad. No generar claves con el número de fila, nombres de personas, fechas reprogramables o la gerencia actual que pudiera cambiar. La clave asignada debe regresar al archivo maestro y mantenerse en toda recarga.

Propuesta técnica: equivalencia `(fuente, entidad, clave_origen) → id_destino`, con restricción única; si las claves originales sólo son únicas por gerencia, incluir el ámbito de origen inmutable. Guardar lote y huella del registro como auditoría, sin incluir el lote en la identidad única, para que otro lote no vuelva a darlo de alta. Validar duplicados dentro del archivo, entre archivos y contra equivalencias previas. Separar alta, actualización y conflicto; no sobrescribir por parecido.

El sistema puede generar el UUID y un folio operativo. El número oficial histórico debe conservarse: no debe inventarse. Para registros sin número, acordar cómo representar esa ausencia; `S/N` no es una clave única. Dos órganos pueden tener el expediente `123/2024`, por lo que el número aislado no identifica suficientemente el asunto.

**Materia:** conservar `materia_codigo` porque el contrato separa código y nombre. En la presentación puede decir “Materia” y mostrar “LABORAL — Laboral”. Renombrarlo a `nombre_materia` sólo tendría sentido si se cambia explícitamente el contrato para transportar nombres y se define su ámbito; esa opción es menos estable. Hoy se requiere resolver el código a la entidad Materia.

**Tipo de expediente:** aporta clasificación independiente de materia y etapa. Está solicitado en alta/edición y tiene persistencia real. No se encontró evidencia para afirmar que por sí solo active flujos o reglas especiales. Conservarlo y acordar si es obligatorio en históricos; su utilidad concreta debe reflejarse en consulta/reportes y en un catálogo que no replique la materia.

**Partes:** distinguir Actor/Demandado aporta valor, pero el catálogo de calidad procesal debe permitir terceros y denominaciones aplicables a otras materias. No todas las partes caben en esa pareja. Propuesta: personas/organizaciones y relación expediente-parte-calidad, con múltiples participantes por calidad. Sin esa estructura pueden conservarse textos legibles, pero no garantizar búsquedas por rol, conteos, identificación repetida de una persona o generación fiable de documentos. No afirmar que esas funciones ya existan sólo por añadir dos columnas.

## Audiencias, órganos y documentos

### Ubicación e identificación

No reemplazar sala_lugar con el catálogo de órganos: “Juzgado Primero” es la autoridad y “Sala 2, planta baja” es el recinto. Modalidad virtual y enlace son otros atributos. El código y el dump revisados no tienen FK directa Audiencia–Órgano; hay relación a través del expediente. Si la audiencia puede celebrarse ante una autoridad distinta, agregar `organo_codigo` propio. Si debe conservarse la autoridad al momento del evento, almacenar una relación histórica o snapshot que no cambie al actualizar el expediente.

Sede representa la ubicación territorial acordada; tipo clasifica la autoridad —por ejemplo JUZGADO, TRIBUNAL o JUNTA—; órgano identifica la institución concreta. La sede no debe mezclarse con `ubicacion_oficina` del usuario. Propuesta normalizada: sede 1:N órganos; tipo de órgano 1:N órganos; expediente N:1 órgano. Si una autoridad se comparte entre gerencias, modelar asignaciones de alcance en lugar de duplicarla sólo por propiedad administrativa. Unicidad de órgano debe considerar la sede/identificación oficial y no depender únicamente de un nombre abreviado.

Hay una brecha de longitudes: el layout admite sede_codigo hasta 255 caracteres, mientras el destino textual revisado permite 100 para sede de órgano y 150 para sede de expediente. Si el código sólo sirve para buscar el nombre, validar también la longitud del valor finalmente persistido; no truncar para que quepa.

Para distinguir una audiencia, mostrar un folio estable como `AUD-000123` junto con expediente, tipo, fecha/hora y órgano. Puede derivarse del ID interno si es suficiente para el alcance de la instalación. Mantenerlo aunque se reprograme; conservar relación entre evento original, reprogramaciones y actas cuando el negocio lo requiera. La clave de origen sirve para conciliación y búsqueda histórica, pero actualmente no es una referencia operativa persistida del módulo.

### Ausencia y recepción posterior de documentos

Si el cliente no entrega un acta, no debe falsearse el estado procesal ni inventarse evidencia. Separar estado de audiencia de disponibilidad documental: `PENDIENTE_ENTREGA`, `NO_DISPONIBLE`, `NO_COMPARTIDO`, `RECIBIDO` o `NO_APLICA`, con motivo, responsable y fecha. Es una propuesta que requiere implementación. Un registro puede estar históricamente concluido y tener evidencia no entregada, si el cliente aprueba esa política; la interfaz debe hacerlo explícito.

Existe flujo de subida posterior de acta, pero **una carga masiva diferida necesita manifiesto, resolución de claves y tratamiento de excepciones**. Bajo el contrato Excel actual no puede usarse CON_ACTA sin acta_archivo. Debe modificarse el contrato si se admitirá información procesal con evidencia pendiente. Lo mismo aplica a PRESENTADO sin acuse.

Un PDF que diga “documento no disponible” no debe ocupar el lugar lógico de acta o acuse: provocaría una falsa confirmación documental. Puede conservarse como constancia administrativa independiente, sin cambiar disponibilidad a RECIBIDO ni satisfacer la regla de evidencia.

Para grandes volúmenes digitales: paquete por lote/gerencia, repositorio de recepción autorizado y manifiesto con clave de entidad, tipo documental, nombre relativo, fecha original conocida, disponibilidad, hash y estado de validación. Verificar existencia, formato, integridad, duplicidad y asociación. Conciliar filas con archivos recibidos y registrar rechazos. No enviar rutas locales como si fueran documentos.

Para físicos: inventario y etiquetado por expediente/actuación; digitalización con responsable, revisión de legibilidad, páginas completas y orientación; OCR cuando aporte búsqueda; entrega de binarios y manifiesto; registro de custodia/devolución acordado con el cliente. El OCR no sustituye la imagen original. Priorizar expedientes activos y conservar originales según la política institucional que corresponda.

## Tipos de término y reglas temporales

El comportamiento esperado se cumple **parcialmente**:

| Comportamiento esperado | Evidencia actual | Dictamen |
|---|---|---|
| Crear tipos de término | Pantalla Catálogos, servicio crear/actualizar/activar y entidad tipos_termino. | Existe en código; verificar despliegue y permisos. |
| Asociar tipo a término | FK tipo_termino_id en JPA y selección en HTML. | Integración incompleta: tipoTerminoId no se resuelve a tipoTerminoRef en el guardado revisado. |
| Regla temporal por tipo | días predeterminados, tipo de días y umbrales rojo/amarillo. | Modelo parcial; falta en Excel. |
| Regla fija o dinámica | Radio en UI y bandera esTiempoDinamico en término. | No se encontró cálculo que use modalidad para obtener vencimiento; edición tampoco copia explícitamente esa configuración. |
| Semáforo de término por tipo | getSemaforoColor prioriza umbrales del tipo y calcula días naturales restantes. | Funciona a nivel de método si existe relación; no demuestra cálculo de días hábiles ni regla dinámica. |
| Semáforo de audiencia por tipo | TipoAudiencia posee umbrales, pero método de audiencia usa 1 y 3 constantes. | Brecha entre configuración y cálculo revisado. |
| Reglas coherentes con gerencia/sistema | ConfiguracionSemaforoService resuelve gerencia → sistema → valores por defecto. | Convive con umbrales por tipo; debe explicarse y unificarse la precedencia. |

No renombrar `asunto` a “Tipo de término”. Añadir `tipo_termino_codigo`; conservar descripción concreta: tipo “Contestación”, asunto “Preparar y presentar contestación del expediente 123/2024”.

Propuesta: cada tipo define modalidad permitida, duración/regla, fecha base, calendario, umbrales y vigencia. Una regla fija puede ser un número de días desde un evento base; una dinámica puede depender de una fecha señalada o evento cambiante. La palabra “dinámica” debe precisarse con el cliente: actualmente no permite inferir una fórmula única. La fecha de vencimiento y el semáforo son resultados relacionados, pero diferentes: calcular un vencimiento no equivale a configurar cuándo se vuelve amarillo.

Usar precedencia explícita: regla versionada del tipo; excepción autorizada del registro cuando proceda; valores generales sólo como respaldo documentado. Guardar regla/versiones y motivo de excepción para que cambiar un catálogo no altere silenciosamente la interpretación histórica. Validar umbrales no negativos y amarillo mayor que rojo; definir límites inclusivos, estado vencido y semáforo apagado para actuaciones terminadas. Unificar el resultado entre listados, agenda, dashboard y notificaciones.

No recalcular ni sobrescribir automáticamente los vencimientos históricos al importar. Si se calcula una fecha de contraste, conservar ambas y presentar diferencias para revisión. Calendarios inhábiles, suspensiones, fecha de notificación y zona horaria requieren definición funcional del cliente; este análisis no determina reglas jurídicas de cómputo.

## Fechas de términos y prioridades

### Definiciones de las tres fechas

| Campo | Definición funcional propuesta | Regla de captura |
|---|---|---|
| fecha_ingreso | Fecha en que la actuación se recibió o incorporó al control del despacho en origen. | Preservar original si se conoce. No sustituir por fecha de migración ni asumir que es la notificación que inicia el plazo. |
| fecha_vencimiento | Fecha límite registrada para cumplir la actuación. | Obligatoria en el contrato actual; conservar fuente y cambios autorizados. |
| fecha_presentacion | Fecha efectiva en que se presentó la actuación. | Vacía mientras no se haya presentado; obligatoria cuando el estado implique presentación, salvo excepción histórica explícita. |

No son redundantes. La comparación presentación–vencimiento permite distinguir presentación anticipada, en fecha o posterior. Una presentación tardía debe preservarse y señalarse, no corregirse para pasar una validación. Ingreso posterior al vencimiento puede reflejar recepción tardía: requiere revisión, no rechazo universal. `created_at`/fecha de carga son auditoría técnica distinta.

Ejemplos ilustrativos, sin prescribir cómputo legal:

| Caso | Ingreso | Vencimiento | Presentación | Interpretación |
|---|---|---|---|---|
| Presentación anticipada | 2024-06-21 | 2024-07-05 | 2024-07-04 | Cumplió antes de la fecha límite registrada. |
| Presentación en fecha | 2024-06-21 | 2024-07-05 | 2024-07-05 | Coinciden, pero siguen siendo hechos distintos. |
| Presentación posterior | 2024-06-21 | 2024-07-05 | 2024-07-08 | Registrar diferencia y motivo; no alterar vencimiento. |
| Pendiente vencido | 2024-06-21 | 2024-07-05 | Vacía | Continúa pendiente a la fecha de corte; una fecha pasada no demuestra conclusión. |

Agregar motivo de conclusión si se admite CONCLUIDO sin presentación. El diccionario actual menciona “CONCLUIDO por presentación”, pero no tiene un campo para distinguir ese motivo. Si se necesita precisión intradía, evaluar hora/zona para vencimiento y presentación, hoy representados como LocalDate.

### Uso de prioridades

| Entidad | Uso verificado | Criterio recomendado |
|---|---|---|
| Expediente | FK prioridad_id. | Importancia general del asunto. |
| Término | FK prioridad_id independiente. | Prioridad de esa actuación; puede diferir de su expediente. |
| Tarea/recordatorio | FK prioridad_id y selector de catálogo en formulario. | Prioridad de esa tarea. Revisar contrato normal/urgent del DTO. |
| Audiencia | No hay prioridad propia en entidad ni en layout. | No afirmar que la tenga por compartir pestaña CATALOGOS. Añadir sólo si existe necesidad distinta del semáforo temporal. |

Existe un catálogo común, **no un único valor global heredado por todas las entidades**. Si se desea sugerir la prioridad del expediente al crear hijos, tratarla como valor inicial explícito, no como propagación retroactiva automática. No equiparar Alta con Rojo: prioridad expresa importancia y semáforo expresa proximidad temporal.

## Mejoras recomendadas

### Críticas: antes de la migración

1. Implementar y probar el contrato de los cinco libros: hojas por nombre, encabezados, códigos, claves, estados y adjuntos. Unificar las plantillas descargables.
2. Publicar catálogo maestro aprobado, eliminar datos de ejemplo del paquete productivo y resolver ámbitos de unicidad.
3. Implementar identidad y equivalencias persistentes para entidades y usuarios sin correo; reintentos sin duplicar y control de coincidencias ambiguas.
4. Eliminar sustituciones de fechas/horas, truncamientos y conclusión automática por antigüedad. Preservar estados y fechas originales.
5. Separar importado, rechazado y omitido; conciliar contra registros realmente persistidos y detener una carga con referencias inválidas.
6. Incorporar Tipos de Término a los layouts, resolver su guardado y comprobar tablas/columnas del destino.
7. Resolver reglas temporales y semáforos por tipo, o acordar explícitamente un alcance de primera migración que conserve fechas sin prometer cálculo automático. No liberar la funcionalidad como terminada mientras exista la brecha.
8. Establecer política de documentos faltantes, carga posterior y estados documentales; prohibir sustitutos registrados como evidencia real.
9. Controlar asignación de roles administrativos y extras desde la carga con la misma autorización de la aplicación.

### Importantes: operación y calidad

1. Guía completa de secuencia, corrección de instrucciones de Catálogos y controles para volúmenes superiores a 200 filas.
2. Hojas relacionales para materias/permisos/gerencias de usuario, más tratamiento de responsables históricos inactivos.
3. Alinear obligatoriedad de tipo de expediente y otros campos entre captura, importación y edición.
4. Estructurar partes procesales y calidad de participación; preservar textos originales.
5. Acordar sede, tipo, órgano, autoridad histórica de audiencia y representación de órganos compartidos.
6. Folio visible de audiencia y tratamiento de reprogramaciones.
7. Recepción documental con manifiesto, validación, conciliación y digitalización controlada.
8. Alinear prioridades entre formularios y DTO; documentar independencia respecto del semáforo.
9. Delimitar historia de etapas, asuntos únicos, varios adjuntos y datos que los cinco layouts no representan.

### Deseables: usabilidad y mantenimiento

1. Mostrar código y nombre juntos, etiquetas de permisos comprensibles y vista de permisos heredados.
2. Generar automáticamente plantillas por gerencia y versión con sólo catálogos pertinentes.
3. Autocompletar datos derivados —por ejemplo sede del órgano cuando corresponda— con advertencia ante contradicciones.
4. Folios y búsqueda por referencia histórica; tablero de pendientes de homologación/documentación.
5. OCR y detección asistida de posibles duplicados, con revisión humana de coincidencias ambiguas.

## Dependencias y preguntas que deben resolverse

| Decisión pendiente | Responsable funcional/técnico | Módulos dependientes |
|---|---|---|
| ¿Se migra únicamente estado actual o toda la cronología, cambios de responsable y versiones documentales? | Cliente, implementación y datos | Expedientes, auditoría, históricos, reportes |
| ¿Qué versión de aplicación y esquema estará en destino? ¿Existen tipos_termino, tipo_termino_id y columnas temporales? | Desarrollo/DBA | Catálogos, términos, despliegue |
| ¿Cuál es la identidad única de cada fuente y cómo se resuelven números de expediente repetidos? | Dueño de datos y migración | Todos los vínculos y adjuntos |
| ¿Qué catálogos son globales, por gerencia o por materia? ¿Quién aprueba altas/cambios? | Cliente e implementación | Catálogos, permisos y reportes |
| ¿Director Principal corresponde a una autoridad del cliente? ¿Quién puede otorgar roles y extras? | Administrador funcional | Usuarios y autorización |
| ¿Cómo se identifican usuarios sin correo, multigerencia o que ya no laboran? | Cliente y usuarios | Responsables, auditoría y notificaciones |
| ¿Tipo de expediente es obligatorio? ¿Se permite Sin clasificar temporalmente? | Dueño de expedientes | Captura, edición, reportes |
| ¿Qué calidades procesales y cuántas partes por expediente deben conservarse? | Usuarios de cada materia | Expedientes, búsqueda y documentos |
| ¿Sede significa estado, ciudad, distrito judicial o recinto? ¿Puede una audiencia tener otro órgano? | Operación jurídica | Órganos, expedientes y agenda |
| ¿Qué significa regla dinámica? ¿Cuál es fecha base, calendario, tratamiento de suspensiones y zona horaria? | Responsable funcional de plazos | Términos, audiencias, semáforos y alertas |
| ¿Un cambio de regla afecta abiertos existentes o sólo nuevos registros? | Responsable funcional | Versionado, alertas e históricos |
| ¿Se permite concluir o marcar presentado sin evidencia? ¿Qué motivo, aprobación y regularización exige? | Cliente y gestión documental | Audiencias, términos, documentos e indicadores |
| ¿Cómo distinguir conclusión por presentación de otros motivos? | Dueño del proceso | Términos, fechas y reportes |
| ¿Los históricos sin enlace virtual, sin hora o sin responsable se aceptan como excepciones o se excluyen? | Cliente e implementación | Agenda y validaciones |
| ¿Se soportan asuntos únicos sin expediente formal y registros cancelados/reprogramados? | Operación y desarrollo | Contextos, expedientes y audiencias |
| ¿Cuántos registros/archivos se cargarán, quién digitaliza y cuál es el repositorio de recepción? | Cliente e infraestructura | Importación, almacenamiento y documentos |
| ¿Qué notificaciones se suprimen durante la migración y cuándo inicia la operación ordinaria? | Operación y desarrollo | Correo, agenda, recordatorios y alertas |
| ¿Qué política de actualización, reversión y aceptación de rechazos parciales se aplicará? | Dueño de datos y desarrollo | Lotes, auditoría y recuperación |

### Criterios de aceptación propuestos

- Ensayo en ambiente controlado con los cinco libros reales y un paquete documental representativo.
- Pruebas de claves duplicadas/reintentos, homónimos, usuarios sin correo/inactivos, catálogos inexistentes, más de 200 filas, fechas inválidas, actuaciones vencidas pendientes y documentos no entregados.
- Verificación de alta y edición posterior del registro migrado, persistencia del tipo de término y coherencia de semáforos entre vistas.
- Reporte por archivo, hoja, fila, clave y campo, con error y corrección esperada; ningún valor inventado o truncado silenciosamente.
- Conciliación: recibidos = importados + rechazados + omitidos justificados. Verificar equivalencias, relaciones, estados, fechas y binarios en destino.
- Acta de aceptación con versión de aplicación/layout/catálogos, lote, responsables y pendientes. No considerar COMPLETADO o PROCESADO como prueba suficiente por sí solos.

## Evidencia y localizadores

Las referencias a pantallas provienen de HTML/código, no de una sesión ejecutada en navegador. Las validaciones Excel se inspeccionaron como propiedades del archivo; no se probó su comportamiento interactivo en Microsoft Excel.

- **E01:** `Layout_Catalogos.xlsx`: CATALOGOS A1:N14; GERENCIAS A5:D5; MATERIAS A5:E5; PRIORIDADES A5:D5; TIPOS_EXPEDIENTE A5:E5; ORGANOS A5:F5; TIPOS_AUDIENCIA A5:E5; SEDES A5:D5; INSTRUCCIONES A4:B13; DICCIONARIO A1:H59.
- **E02:** `Layout_Usuarios.xlsx`: CATALOGOS A1:N14; CAPTURA A5:M5 y validaciones 6:205; INSTRUCCIONES A4:B11; DICCIONARIO A2:H14.
- **E03:** `Layout_Expedientes.xlsx`: CAPTURA A5:L5; DICCIONARIO A2:H13; CATALOGOS A1:N14 y validaciones 6:205.
- **E04:** `Layout_Audiencias.xlsx`: CAPTURA A5:O5; DICCIONARIO A2:H16; CATALOGOS A1:N14 y validaciones 6:205.
- **E05:** `Layout_Terminos.xlsx`: CAPTURA A5:L5; DICCIONARIO A2:H13; CATALOGOS A1:N14 y validaciones 6:205.
- **E06:** `src/main/java/com/juridico/sistema_juridico/Entity/expediente/Expediente.java` y `Entity/catalogo/Materia.java`: identidades, relaciones, partes, sede, prioridad y tipo.
- **E07:** `Entity/procesal/Audiencia.java`; `service/procesal/AudienciaService.java`, métodos subir/concluir; `controller/AudienciasController.java:726`: carga posterior de acta.
- **E08:** `service/procesal/LoteMigracionService.java:1516`, búsqueda por número/partes; `:1644`, creación de términos/audiencias, estados, duplicados y truncamientos; `:1760`, errores y PROCESADO; `:2273`, parseDate/parseTime.
- **E09:** `Entity/catalogo/OrganoJurisdiccional.java`: sede, tipo, nombre único y gerencia.
- **E10:** `Entity/enums/RolUsuario.java`; `Entity/enums/PermisoSistema.java`; `service/PermisoUsuarioService.java`; `service/UsuarioService.java`: roles, permisos base/extras y administración.
- **E11:** `Entity/catalogo/TipoTermino.java`; `Entity/procesal/Termino.java:81` y `:161`; `service/catalogo/TipoTerminoService.java`; `controller/CatalogosController.java:901`; `controller/TerminosController.java:382`; `src/main/resources/templates/fragments/terminos/modal-nuevo-termino.html:123`; `src/main/resources/templates/views/catalogos/index.html:337`.
- **E12:** `Entity/catalogo/TipoAudiencia.java`; `service/configuracion/ConfiguracionSemaforoService.java`; `Entity/procesal/Audiencia.java`, getSemaforoColor.
- **E13:** `Entity/usuario/Recordatorio.java`; `controller/TareasController.java:46`; `dto/request/recordatorios/RecordatorioRequest.java:19`; `src/main/resources/templates/fragments/tareas/modal-recordatorio.html:43`.
- **E14:** `src/main/resources/templates/fragments/audiencias/tabla.html:48` y siguientes: tipo, expediente y fecha; identificadores usados por acciones del registro.
- **E15:** `controller/AdminImportController.java`, descargarPlantillaTerminos/descargarPlantillaAudiencias/generarPlantilla; `service/ImportService.java`; `service/procesal/TerminoImportService.java`, método importar.
- **E16:** `src/main/resources/templates/fragments/expedientes/modal-nuevo-exp.html:104`; `fragments/expedientes/modales-detalle.html:143`; `controller/ExpedientesController.java:411` y `:471`: tipo de expediente requerido en HTML y persistido.
- **E17:** `db_schema_actual.sql:363`, audiencias con expediente_id NOT NULL y sin órgano directo; tabla expedientes y relaciones a órgano; no se encontró CREATE TABLE para tipos_termino. El modelo JPA permite expediente nullable en audiencia, por lo que se requiere verificar el destino.
- **E18:** `Entity/usuario/Usuario.java:101`, relación usuario_gerencias; referencias de responsables en E02–E05.

Los archivos Java indicados con ruta abreviada están bajo `src/main/java/com/juridico/sistema_juridico/`. Los Excel están bajo `Layouts_Carga_Masiva_SIAJ/`. El análisis previo del 7 de septiembre se utilizó sólo como antecedente y los hallazgos centrales se contrastaron con las fuentes actuales.
