# SIAJ: layouts, plantillas y formatos de carga masiva

Fecha de análisis: 7 de septiembre de 2026. Versión de propuesta: 1.0.

## Alcance y criterio de evidencia

Se revisaron el dump `db_schema_actual.sql`, los modelos JPA actuales, DTO, controladores, servicios de importación, catálogos y scripts de despliegue locales. Se incluye el estado actual del directorio de trabajo, con cambios locales, no sólo el último commit. No se conectó a una base de datos ni se ejecutó una importación. Los ejemplos son ficticios. Los nombres e IDs vigentes del ambiente destino deben exportarse antes de distribuir las plantillas definitivas.

**Los layouts completos son contratos de captura propuestos, no una afirmación de que SIAJ ya tenga importadores para todas las entidades.** Las rutas históricas existentes tienen contratos separados. No renombrar un CSV a .xlsx: un Excel debe ser un archivo OOXML real.

En las tablas: **Sí (BD)** proviene de NOT NULL del dump; **Sí (JPA)** de modelo no confirmado en BD; **Sí (controlador)** de validación observada; **Sí (propuesta)** añade calidad de captura; **Condicional** requiere aplicar la regla indicada. Un valor opcional en BD no garantiza que la interfaz lo acepte. Longitud en caracteres; para UUID/enteros/fechas no aplica longitud de texto. TEXT no tiene límite declarado en el esquema; Excel limita cada celda a 32,767 caracteres, por lo que textos mayores requieren JSON/archivo y adaptación del importador.

## Instrucciones comunes para usuarios

1. Capturar una fila por registro y conservar encabezados exactos. No incluir subtotales, celdas combinadas, fórmulas, macros ni filas decorativas dentro de datos.
2. El libro entregado concentra las hojas de captura. Las hojas Resumen, Diccionario, Catalogos y Ejemplos son de consulta; no se importan. Los ejemplos están separados de la captura vacía.
3. Los archivos individuales sugeridos se obtienen copiando la hoja indicada a un libro nuevo como primera hoja. Para el Excel legado conservar exclusivamente sus siete columnas. El libro consolidado no debe enviarse completo al importador legado.
4. Fechas AAAA-MM-DD, horas HH:mm, booleanos true/false. Identificadores y números de expediente como texto para preservar ceros. Vacío representa ausencia, nunca la palabra NULL ni N/A.
5. En layouts propuestos, columnas *_id son referencias del ambiente destino: el equipo entrega catálogo con id y nombre. No reutilizar IDs de otra instalación. Para altas relacionadas en el mismo lote, cargar padres, recuperar IDs y después completar hijos.
6. Propuesta para automatizar cargas relacionadas: mantener tabla técnica de equivalencias (entidad, clave_origen, id_destino, lote); no inventar IDs ni usar nombres ambiguos. Este mecanismo todavía requiere implementación.
7. CSV alternativo sólo con adaptador: UTF-8, coma como separador, comillas dobles para textos con comas/saltos y escape doble de comillas. Los importadores XLSX existentes no leen CSV por cambiar la extensión.
8. Archivos adjuntos: manifiesto más carpeta de binarios, rutas relativas; validar existencia y nombre. Los campos internos de ruta, tamaño, MIME, IDs generados y auditoría los completa el servicio.
9. Rechazar duplicados, referencias inexistentes y catálogos inactivos antes de aplicar cambios (política propuesta). No sobrescribir registros por coincidencia parcial; separar altas de actualizaciones con clave inequívoca.

## Orden de carga propuesto

Gerencias → materias y catálogos básicos → usuarios → asignaciones de gerencias/materias y permisos → etapas/tipos de término → expedientes → contextos → audiencias/términos → carpetas (padres antes de hijos) → documentos/archivos → colaboradores/comentarios → recordatorios/eventos manuales → configuraciones/preferencias. El usuario creador de catálogos lo aporta la sesión administrativa para evitar ciclos. Los históricos derivados se generan por el flujo, o se concilian al final de una migración histórica específica.

## Matriz resumen

| Entidad / proceso | Archivo sugerido | Hoja de captura | Estado |
| --- | --- | --- | --- |
| Gerencias | SIAJ_gerencias_v1.xlsx | gerencias | Diseño propuesto; requiere importador |
| Materias | SIAJ_materias_v1.xlsx | materias | Diseño propuesto; requiere importador |
| Estados y sedes | SIAJ_cat_estados_v1.xlsx | cat_estados | Diseño propuesto; requiere importador |
| Prioridades | SIAJ_cat_prioridad_v1.xlsx | cat_prioridad | Diseño propuesto; requiere importador |
| Tipos de expediente | SIAJ_cat_tipo_expediente_v1.xlsx | cat_tipo_expediente | Diseño propuesto; requiere importador |
| Órganos jurisdiccionales | SIAJ_organos_jurisdiccionales_v1.xlsx | organos_jurisdiccionales | Diseño propuesto; requiere importador |
| Tipos de audiencia | SIAJ_tipos_audiencia_v1.xlsx | tipos_audiencia | Diseño propuesto; requiere importador |
| Tipos de término | SIAJ_cat_tipos_termino_v1.xlsx | cat_tipos_termino | JPA sin tabla en dump; validar despliegue |
| Etapas y subetapas procesales | SIAJ_cat_etapas_procesales_v1.xlsx | cat_etapas_procesales | JPA sin tabla en dump; validar despliegue |
| Usuarios | SIAJ_usuarios_v1.xlsx | usuarios | Diseño propuesto; requiere importador |
| Asignación de gerencias a usuarios | SIAJ_usuario_gerencias_v1.xlsx | usuario_gerencias | Diseño propuesto; requiere importador |
| Asignación de materias a usuarios | SIAJ_usuario_materias_v1.xlsx | usuario_materias | Diseño propuesto; requiere importador |
| Permisos adicionales | SIAJ_usuario_permisos_adicionales_v1.xlsx | usuario_permisos_adicionales | Diseño propuesto; requiere importador |
| Expedientes | SIAJ_expedientes_v1.xlsx | expedientes | Diseño propuesto; requiere importador |
| Contextos de audiencia | SIAJ_audiencia_contextos_v1.xlsx | audiencia_contextos | Diseño propuesto; requiere importador |
| Contextos de término | SIAJ_termino_contextos_v1.xlsx | termino_contextos | Diseño propuesto; requiere importador |
| Audiencias | SIAJ_audiencias_v1.xlsx | audiencias | Layout completo propuesto; API histórica parcial existente |
| Términos | SIAJ_terminos_v1.xlsx | terminos | Layout completo propuesto; API histórica parcial existente |
| Colaboradores de expediente | SIAJ_colaboradores_expediente_v1.xlsx | colaboradores_expediente | Diseño propuesto; requiere importador |
| Carpetas | SIAJ_carpetas_v1.xlsx | carpetas | Diseño propuesto; requiere importador |
| Documentos de expediente | SIAJ_documentos_v1.xlsx | documentos | Diseño propuesto; requiere importador |
| Archivos de término | SIAJ_termino_archivos_v1.xlsx | termino_archivos | Diseño propuesto; requiere importador |
| Comentarios y observaciones | SIAJ_comentarios_v1.xlsx | comentarios | Diseño propuesto; requiere importador |
| Tareas y recordatorios | SIAJ_recordatorios_v1.xlsx | recordatorios | Diseño propuesto; requiere importador |
| Eventos de calendario | SIAJ_eventos_calendario_v1.xlsx | eventos_calendario | Diseño propuesto; requiere importador |
| Configuraciones de alertas | SIAJ_configuraciones_alerta_v1.xlsx | configuraciones_alerta | Diseño propuesto; requiere importador |
| Configuraciones de semáforo | SIAJ_configuraciones_semaforo_v1.xlsx | configuraciones_semaforo | Diseño propuesto; requiere importador |
| Preferencias generales de notificación | SIAJ_preferencias_notificacion_v1.xlsx | preferencias_notificacion | Diseño propuesto; requiere importador |
| Preferencias por evento | SIAJ_preferencias_notificacion_evento_v1.xlsx | preferencias_notificacion_event | Diseño propuesto; requiere importador |
| Historial de etapas de expediente | SIAJ_expediente_etapa_periodos_v1.xlsx | expediente_etapa_periodos | Sólo migración histórica controlada |
| Audiencias desahogadas | SIAJ_audiencias_desahogadas_v1.xlsx | audiencias_desahogadas | Sólo migración histórica controlada |
| Términos presentados | SIAJ_terminos_presentados_v1.xlsx | terminos_presentados | Sólo migración histórica controlada |
| Términos: Excel legado | SIAJ_terminos_excel_legado_v1.xlsx | terminos_excel_legado | Servicio existente; ruta de carga no confirmada |
| Audiencias: API histórica | SIAJ_audiencias_api_historica_v1.json | audiencias_api_historica | API existente; homologación e inyección posteriores |
| Terminos: API histórica | SIAJ_terminos_api_historica_v1.json | terminos_api_historica | API existente; homologación e inyección posteriores |
| Actores | SIAJ_actores_levantamiento_v1.xlsx | actores | Levantamiento; no importable como entidad independiente |
| Demandados | SIAJ_demandados_levantamiento_v1.xlsx | demandados | Levantamiento; no importable como entidad independiente |
| Organismos | SIAJ_organismos_levantamiento_v1.xlsx | organismos | Levantamiento; no importable como entidad independiente |
| Salas | SIAJ_salas_levantamiento_v1.xlsx | salas | Levantamiento; no importable como entidad independiente |
| Juzgados | SIAJ_juzgados_levantamiento_v1.xlsx | juzgados | Levantamiento; no importable como entidad independiente |

# Entidad: Gerencias

## Descripción

Captura de gerencias para carga inicial o migración controlada.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_gerencias_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `gerencias` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| activo | Activo | Booleano | No | No aplica | true | true o false; no sustituir vacío por false sin regla. |
| descripcion | Descripcion | Texto | No | Sin límite declarado | Seguimiento del asunto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| nombre | Nombre | Texto | Sí (BD) | 200 | Valor del catálogo | Texto literal; conservar ceros iniciales en identificadores. |

## Dependencias

- Sin FK externa confirmada; consultar alcance funcional y catálogos.

## Reglas de negocio

- Nombre obligatorio y único en el dump. Cargar antes de materias y usuarios.

Llaves y restricciones observadas:

- `gerencias_pkey PRIMARY KEY (id)`.
- `uk5nv105r1e4j3r6hrfxxa4fxeg UNIQUE (nombre)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Campos fuera de captura ordinaria: `id`, `created_at`, `updated_at`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Materias

## Descripción

Captura de materias para carga inicial o migración controlada.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_materias_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `materias` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| activo | Activo | Booleano | No | No aplica | true | true o false; no sustituir vacío por false sin regla. |
| descripcion | Descripcion | Texto | No | Sin límite declarado | Seguimiento del asunto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| nombre | Nombre | Texto | Sí (BD) | 100 | Valor del catálogo | Texto literal; conservar ceros iniciales en identificadores. |
| gerencia_id | Referencia a Gerencias | Entero | Sí (BD) | No aplica | 1 | Debe existir en gerencias; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| usuario_id | Referencia a Usuarios | Entero | No | No aplica | 1 | Debe existir en usuarios; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |

## Dependencias

- usuario_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.
- gerencia_id → gerencias.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- La gerencia es obligatoria en BD. Homologar por nombre y gerencia; el dump conserva unicidad global del nombre: no asumir que puede repetirse entre gerencias.

Llaves y restricciones observadas:

- `materias_pkey PRIMARY KEY (id)`.
- `uk35p3esp62g4obf0xvgxkrgh2e UNIQUE (nombre, gerencia_id)`.
- `fk88y1xy72ng7x0aa9orpy942p7 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`.
- `fkkf13pj696ynk5lfcetip9d5oy FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Campos fuera de captura ordinaria: `id`, `created_at`, `updated_at`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Estados y sedes

## Descripción

Catálogo de estados/sedes utilizado para homologar ubicaciones de los asuntos.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_cat_estados_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `cat_estados` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| activo | Activo | Booleano | No | No aplica | true | true o false; no sustituir vacío por false sin regla. |
| nombre | Nombre | Texto | Sí (propuesta) | 255 | Valor del catálogo | Texto literal; conservar ceros iniciales en identificadores. |
| gerencia_id | Referencia a Gerencias | Entero | No | No aplica | 1 | Debe existir en gerencias; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| usuario_id | Referencia a Usuarios | Entero | No | No aplica | 1 | Debe existir en usuarios; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |

## Dependencias

- usuario_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.
- gerencia_id → gerencias.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Catálogo usado como sede; no existe FK desde expedientes.sede. Homologar el texto por catálogo vigente. Nombre no es NOT NULL en dump; se exige en el layout como regla propuesta.

Llaves y restricciones observadas:

- `cat_estados_pkey PRIMARY KEY (id)`.
- `uknst6hp4h4jox86rou3fpxfmd0 UNIQUE (nombre)`.
- `fks243iav8cbu2feurmh2qjaiy6 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`.
- `fksvk5hct1ehr5jejuecfr59pih FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Campos fuera de captura ordinaria: `id`, `created_at`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Prioridades

## Descripción

Captura de prioridades para carga inicial o migración controlada.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_cat_prioridad_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `cat_prioridad` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| activo | Activo | Booleano | No | No aplica | true | true o false; no sustituir vacío por false sin regla. |
| color | Color | Texto | No | 100 | bg-red-100 text-red-800 | Texto literal; conservar ceros iniciales en identificadores. |
| nombre | Nombre | Texto | Sí (BD) | 50 | Valor del catálogo | Texto literal; conservar ceros iniciales en identificadores. |

## Dependencias

- Sin FK externa confirmada; consultar alcance funcional y catálogos.

## Reglas de negocio

- ALTA, MEDIA y BAJA son semillas de DataInitializer; el catálogo en BD es editable. Exportar IDs vigentes, no asumir 1/2/3. El campo color contiene clases CSS en las semillas, no necesariamente hexadecimal.

Llaves y restricciones observadas:

- `cat_prioridad_pkey PRIMARY KEY (id)`.
- `ukp238eocvbwiipffxptyq0uq07 UNIQUE (nombre)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Campos fuera de captura ordinaria: `id`, `created_at`, `updated_at`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Tipos de expediente

## Descripción

Captura de tipos de expediente para carga inicial o migración controlada.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_cat_tipo_expediente_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `cat_tipo_expediente` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| activo | Activo | Booleano | No | No aplica | true | true o false; no sustituir vacío por false sin regla. |
| descripcion | Descripcion | Texto | No | Sin límite declarado | Seguimiento del asunto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| nombre | Nombre | Texto | Sí (BD) | 100 | Valor del catálogo | Texto literal; conservar ceros iniciales en identificadores. |
| gerencia_id | Referencia a Gerencias | Entero | No | No aplica | 1 | Debe existir en gerencias; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| usuario_id | Referencia a Usuarios | Entero | No | No aplica | 1 | Debe existir en usuarios; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |

## Dependencias

- usuario_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.
- gerencia_id → gerencias.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Nombre único en dump. Confirmar ámbito global o por gerencia antes de crear duplicados.

Llaves y restricciones observadas:

- `cat_tipo_expediente_pkey PRIMARY KEY (id)`.
- `uk9cslxj82xdh4mfg9baolqj7xu UNIQUE (nombre)`.
- `fk72utcny5h8l8oeddw7p2ducwb FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`.
- `fkk792qaf01pxxgna4hd5yakesh FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Campos fuera de captura ordinaria: `id`, `created_at`, `updated_at`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Órganos jurisdiccionales

## Descripción

Catálogo de autoridades ante las que se tramita el expediente.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_organos_jurisdiccionales_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `organos_jurisdiccionales` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| activo | Activo | Booleano | No | No aplica | true | true o false; no sustituir vacío por false sin regla. |
| nombre | Nombre | Texto | Sí (BD) | 300 | Valor del catálogo | Texto literal; conservar ceros iniciales en identificadores. |
| sede | Sede | Texto | No | 100 | Ciudad de México | Texto literal; conservar ceros iniciales en identificadores. |
| tipo | Tipo | Texto | No | 50 | Texto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| gerencia_id | Referencia a Gerencias | Entero | No | No aplica | 1 | Debe existir en gerencias; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| usuario_id | Referencia a Usuarios | Entero | No | No aplica | 1 | Debe existir en usuarios; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |

## Dependencias

- usuario_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.
- gerencia_id → gerencias.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Representa autoridades jurisdiccionales, incluidos juzgados y salas cuando corresponda. tipo es texto sin enum/CHECK en el dump. No equiparar automáticamente organismos administrativos con órganos jurisdiccionales.

Llaves y restricciones observadas:

- `organos_jurisdiccionales_pkey PRIMARY KEY (id)`.
- `ukmc16d9k9cfsrevrlbjdt9cctb UNIQUE (nombre)`.
- `fk139ctye6aj13npxjakubnphj5 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`.
- `fkp3dc7r1182p73giiehvc1a3kw FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Campos fuera de captura ordinaria: `id`, `created_at`, `updated_at`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Tipos de audiencia

## Descripción

Captura de tipos de audiencia para carga inicial o migración controlada.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_tipos_audiencia_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `tipos_audiencia` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| activo | Activo | Booleano | No | No aplica | true | true o false; no sustituir vacío por false sin regla. |
| descripcion | Descripcion | Texto | No | Sin límite declarado | Seguimiento del asunto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| nombre | Nombre | Texto | Sí (BD) | 100 | Valor del catálogo | Texto literal; conservar ceros iniciales en identificadores. |
| gerencia_id | Referencia a Gerencias | Entero | No | No aplica | 1 | Debe existir en gerencias; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| usuario_id | Referencia a Usuarios | Entero | No | No aplica | 1 | Debe existir en usuarios; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |

## Dependencias

- usuario_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.
- gerencia_id → gerencias.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Unicidad (nombre, gerencia_id) en dump. Respetar ámbito de gerencia y visibilidad por usuario; exportar catálogo activo.

Llaves y restricciones observadas:

- `tipos_audiencia_pkey PRIMARY KEY (id)`.
- `ukhwdw390hju3jpv9r2pvbn7eat UNIQUE (nombre)`.
- `fk1kml027csdf02fbsgarvpaacn FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`.
- `fklhw4n232t69p6wb8mkmubvkl6 FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Campos fuera de captura ordinaria: `id`, `created_at`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Tipos de término

## Descripción

Captura de tipos de término para carga inicial o migración controlada.

Estado: **JPA sin tabla en dump; validar despliegue**.

## Archivo sugerido

`SIAJ_cat_tipos_termino_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `cat_tipos_termino` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| nombre | Nombre | Texto | Sí (JPA) | 100 | Valor del catálogo | Texto literal; conservar ceros iniciales en identificadores. |
| descripcion | Descripcion | Texto | No | Sin límite declarado | Seguimiento del asunto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| dias_predeterminados | Dias predeterminados | Entero | Sí (JPA) | No aplica | 15 | Sin decimales; IDs del ambiente destino. |
| tipo_dias | Tipo dias | Texto | Sí (JPA) | 20 | HABILES | Permitidos: HABILES, NATURALES. Texto literal; conservar ceros iniciales en identificadores. |
| semaforo_rojo_dias | Semaforo rojo dias | Entero | No | No aplica | 3 | Sin decimales; IDs del ambiente destino. |
| semaforo_amarillo_dias | Semaforo amarillo dias | Entero | No | No aplica | 7 | Sin decimales; IDs del ambiente destino. |
| activo | Activo | Booleano | No | No aplica | true | true o false; no sustituir vacío por false sin regla. |
| materia_id | Materia id | Entero | No | No aplica | 1 | Sin decimales; IDs del ambiente destino. |
| gerencia_id | Gerencia id | Entero | No | No aplica | 1 | Sin decimales; IDs del ambiente destino. |

## Dependencias

- materia_id → materias.id. Cargar previamente o resolver en el mismo proceso controlado.
- gerencia_id → gerencias.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Modelo actual no presente en dump. Nombre obligatorio y único; validación del servicio rechaza duplicados. HABILES/NATURALES provienen del modelo, no de un CHECK del dump. Valores predeterminados: 15 días, rojo 3, amarillo 7, activo true. Validar calendario inhábil y cálculo antes de habilitar carga.

Llaves y restricciones observadas:

- `PK id INTEGER generado (JPA)`.
- `UNIQUE nombre (JPA)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Evidencia: `Entity/catalogo/TipoTermino.java`.

# Entidad: Etapas y subetapas procesales

## Descripción

Captura de etapas y subetapas procesales para carga inicial o migración controlada.

Estado: **JPA sin tabla en dump; validar despliegue**.

## Archivo sugerido

`SIAJ_cat_etapas_procesales_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `cat_etapas_procesales` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| nombre | Nombre | Texto | Sí (JPA) | 120 | Valor del catálogo | Texto literal; conservar ceros iniciales en identificadores. |
| clave | Clave | Texto | No | 80 | Texto de ejemplo | Se genera desde nombre si se omite. Texto literal; conservar ceros iniciales en identificadores. |
| orden | Orden | Entero | Sí (JPA) | No aplica | 1 | Sin decimales; IDs del ambiente destino. |
| descripcion | Descripcion | Texto | No | Sin límite declarado | Seguimiento del asunto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| activo | Activo | Booleano | No | No aplica | true | true o false; no sustituir vacío por false sin regla. |
| cierra_expediente | Cierra expediente | Booleano | No | No aplica | false | true o false; no sustituir vacío por false sin regla. |
| etapa_legacy | Etapa legacy | Texto | No | 30 | TRAMITE | Permitidos: TRAMITE, LAUDO, FIRME, CONCLUIDO. Texto literal; conservar ceros iniciales en identificadores. |
| materia_id | Materia id | Entero | Sí (JPA) | No aplica | 1 | Sin decimales; IDs del ambiente destino. |
| parent_id | Etapa superior | Entero | No | No aplica | 1 | Sin decimales; IDs del ambiente destino. |

## Dependencias

- materia_id → materias.id. Cargar previamente o resolver en el mismo proceso controlado.
- parent_id → cat_etapas_procesales.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Modelo no presente en dump. Padre de la misma materia, máximo dos niveles; no autorreferencia. Una etapa con hijos no se convierte en subetapa (CatalogosController). clave se normaliza desde nombre si se omite.

Llaves y restricciones observadas:

- `PK id INTEGER generado (JPA)`.
- `UNIQUE (materia_id, nombre, parent_id) (JPA); verificar semántica de NULL para raíces`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Evidencia: `Entity/catalogo/EtapaCatalogo.java`.

# Entidad: Usuarios

## Descripción

Cuentas, identidad laboral, roles y alcance de acceso al sistema.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_usuarios_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `usuarios` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| activo | Activo | Booleano | No | No aplica | true | true o false; no sustituir vacío por false sin regla. |
| email | Correo de acceso o contacto | Texto | Condicional: ver discrepancia BD/JPA | 100 | ana.perez@example.org | Texto literal; conservar ceros iniciales en identificadores. |
| nombre_completo | Nombre completo | Texto | Sí (BD) | 200 | Ana Pérez López | Texto literal; conservar ceros iniciales en identificadores. |
| rol | Rol | Texto | Sí (BD) | 50 | ADMIN | Permitidos: ADMIN, DIRECTOR_PRINCIPAL, DIRECTOR, SUBDIRECTOR, GERENTE, JEFE_DEPTO, ABOGADO. Texto literal; conservar ceros iniciales en identificadores. |
| username | Nombre de acceso local | Texto | No | 80 | ana.perez | Texto literal; conservar ceros iniciales en identificadores. |
| numero_empleado | Numero empleado | Texto | No | 40 | 000123 | Texto literal; conservar ceros iniciales en identificadores. |
| puesto | Puesto | Texto | No | 160 | Abogada | Texto literal; conservar ceros iniciales en identificadores. |
| telefono_institucional | Telefono institucional | Texto | No | 120 | 5550000000 ext. 100 | Texto literal; conservar ceros iniciales en identificadores. |
| telefono_movil | Telefono movil | Texto | No | 80 | 5550000000 | Texto literal; conservar ceros iniciales en identificadores. |
| ubicacion_oficina | Ubicacion oficina | Texto | No | 180 | Oficina 1 | Texto literal; conservar ceros iniciales en identificadores. |

## Dependencias

- creado_por_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.
- gerencia_id → gerencias.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- email es obligatorio en dump, pero opcional en JPA y migración 03-allow-local-only-users.sql. Confirmar aplicación de esa migración.
- Username se normaliza/genera por UsuarioIdentityService; username y número de empleado son únicos ignorando mayúsculas. Número de empleado se recorta y pasa a mayúsculas; email a minúsculas.
- Alta mediante UsuarioService, respetando jerarquía del operador y alcance de gerencias/materias. Contraseñas, hashes y vínculos Microsoft no se solicitan en Excel; provisionar credencial/activación mediante el flujo autorizado.
- El modelo usa usuario_gerencias; gerencia_id legado del dump requiere reconciliación.

Llaves y restricciones observadas:

- `ukkfsp0s1tflm1cwlj8idhqsad0 UNIQUE (email)`.
- `ukr2mnk6pyydgygq94im0nmaps5 UNIQUE (microsoft_object_id)`.
- `usuarios_pkey PRIMARY KEY (id)`.
- `fkhy8fli5s85u3iut9tvcjggx77 FOREIGN KEY (creado_por_id) REFERENCES public.usuarios(id)`.
- `fkrel3m11jpih4a77lseyy3h3ad FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Campos fuera de captura ordinaria: `id`, `created_at`, `force_password_change`, `password_hash`, `password_updated_at`, `ultimo_acceso`, `updated_at`, `gerencia_id`, `creado_por_id`, `microsoft_business_phones`, `microsoft_display_name`, `microsoft_job_title`, `microsoft_last_sync_at`, `microsoft_linked_at`, `microsoft_login_enabled`, `microsoft_mobile_phone`, `microsoft_object_id`, `microsoft_office_location`, `microsoft_user_principal_name`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Asignación de gerencias a usuarios

## Descripción

Captura de asignación de gerencias a usuarios para carga inicial o migración controlada.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_usuario_gerencias_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `usuario_gerencias` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| usuario_id | Referencia a Usuarios | Entero | Sí (BD) | No aplica | 1 | Debe existir en usuarios; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| gerencia_id | Referencia a Gerencias | Entero | Sí (BD) | No aplica | 1 | Debe existir en gerencias; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |

## Dependencias

- gerencia_id → gerencias.id. Cargar previamente o resolver en el mismo proceso controlado.
- usuario_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Una fila por usuario y gerencia, sin duplicar el par. No enviar listas separadas por comas en una celda.

Llaves y restricciones observadas:

- `usuario_gerencias_pkey PRIMARY KEY (usuario_id, gerencia_id)`.
- `fkai89eqag1l6q65v4f2w54b2gd FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id)`.
- `fkk2iku7uhxeu1b8r5j2qo8kl82 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Asignación de materias a usuarios

## Descripción

Captura de asignación de materias a usuarios para carga inicial o migración controlada.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_usuario_materias_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `usuario_materias` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| usuario_id | Referencia a Usuarios | Entero | Sí (BD) | No aplica | 1 | Debe existir en usuarios; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| materia_id | Referencia a Materias | Entero | Sí (BD) | No aplica | 1 | Debe existir en materias; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |

## Dependencias

- usuario_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.
- materia_id → materias.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Una fila por usuario y materia; materia coherente con gerencias asignadas. Verificar alcance en servicios de autorización.

Llaves y restricciones observadas:

- `usuario_materias_pkey PRIMARY KEY (usuario_id, materia_id)`.
- `fkblixcl3jed4e5u1r4cbuxo233 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`.
- `fkgmypxwp3xdvi5ckk7xsd6flna FOREIGN KEY (materia_id) REFERENCES public.materias(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Permisos adicionales

## Descripción

Captura de permisos adicionales para carga inicial o migración controlada.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_usuario_permisos_adicionales_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `usuario_permisos_adicionales` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| usuario_id | Referencia a Usuarios | Entero | Sí (BD) | No aplica | 1 | Debe existir en usuarios; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| permiso | Permiso | Texto | Sí (BD) | 80 | DASHBOARD_VER | Permitidos: DASHBOARD_VER, AGENDA_VER, CATALOGOS_VER, USUARIOS_VER, ADMIN_PANEL, EXPEDIENTES_VER, EXPEDIENTES_SUPERVISION, TERMINOS_VER, TERMINOS_SUPERVISION, AUDIENCIAS_VER, AUDIENCIAS_SUPERVISION, DOCUMENTOS_SUPERVISION, ALERTAS_CONFIGURAR. Texto literal; conservar ceros iniciales en identificadores. |

## Dependencias

- usuario_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Permisos de PermisoSistema; sólo asignación administrativa. No usar la carga para eludir jerarquías.

Llaves y restricciones observadas:

- `usuario_permisos_adicionales_pkey PRIMARY KEY (usuario_id, permiso)`.
- `fkjml13xi25qa1awrohharg2hc3 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Expedientes

## Descripción

Registro central del asunto jurídico y sus relaciones organizacionales, responsables y etapa.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_expedientes_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `expedientes` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| descripcion | Descripcion | Texto | Sí (BD) | Sin límite declarado | Seguimiento del asunto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| etapa_procesal | Etapa procesal | Texto | Sí (BD) | 30 | TRAMITE | Permitidos: TRAMITE, LAUDO, FIRME, CONCLUIDO. Texto literal; conservar ceros iniciales en identificadores. |
| numero | Numero | Texto | Sí (BD) | 50 | EXP-001/2026 | Texto literal; conservar ceros iniciales en identificadores. |
| organo_jurisdiccional_texto | Organo jurisdiccional texto | Texto | No | Sin límite declarado | Texto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| partes | Identificación textual de las partes procesales | Texto | No | Sin límite declarado | Actor: Persona A; Demandado: Entidad B | Texto literal; conservar ceros iniciales en identificadores. |
| sede | Sede | Texto | No | 150 | Ciudad de México | Texto literal; conservar ceros iniciales en identificadores. |
| abogado_responsable_id | Referencia a Usuarios | Entero | No | No aplica | 1 | Debe existir en usuarios; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| gerencia_id | Referencia a Gerencias | Entero | Sí (BD) | No aplica | 1 | Debe existir en gerencias; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| materia_id | Referencia a Materias | Entero | Sí (BD) | No aplica | 1 | Debe existir en materias; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| organo_jurisdiccional_id | Referencia a Órganos jurisdiccionales | Entero | No | No aplica | 1 | Debe existir en organos_jurisdiccionales; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| prioridad_id | Referencia a Prioridades | Entero | No | No aplica | 1 | Debe existir en cat_prioridad; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| tipo_expediente_id | Referencia a Tipos de expediente | Entero | No | No aplica | 1 | Debe existir en cat_tipo_expediente; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| etapa_catalogo_id | Etapa configurable de la materia | Entero | No | No aplica | 1 | Sólo JPA; no existe en el dump. Etapa de la materia del expediente. Sin decimales; IDs del ambiente destino. |

## Dependencias

- organo_jurisdiccional_id → organos_jurisdiccionales.id. Cargar previamente o resolver en el mismo proceso controlado.
- materia_id → materias.id. Cargar previamente o resolver en el mismo proceso controlado.
- gerencia_id → gerencias.id. Cargar previamente o resolver en el mismo proceso controlado.
- prioridad_id → cat_prioridad.id. Cargar previamente o resolver en el mismo proceso controlado.
- abogado_responsable_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.
- tipo_expediente_id → cat_tipo_expediente.id. Cargar previamente o resolver en el mismo proceso controlado.
- etapa_catalogo_id → cat_etapas_procesales.id (JPA). Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Número obligatorio, máximo 50. No hay UNIQUE(numero) en el dump. No usar sólo el número para conciliar expedientes homónimos.
- Materia y gerencia son NOT NULL en dump y DTO; JPA no declara la misma obligatoriedad. Exigir ambas y coherencia entre ellas.
- Descripción obligatoria; etapa enum obligatorio con valor inicial TRAMITE en JPA. partes es texto libre sin estructura normalizada de actores/demandados.
- El asistente deriva expedientes de audiencias y términos; este layout independiente necesita importador. es_temporal corresponde a manejo interno, no a captura ordinaria.

Llaves y restricciones observadas:

- `expedientes_pkey PRIMARY KEY (id)`.
- `fk4ko1kuyjwubjuk8pqc6uj7nkl FOREIGN KEY (organo_jurisdiccional_id) REFERENCES public.organos_jurisdiccionales(id)`.
- `fk58skvtk8tcu7wlxc6q6bd9kbt FOREIGN KEY (materia_id) REFERENCES public.materias(id)`.
- `fkbichuxp9f2p9t665qhevw9hv FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id)`.
- `fkjd2xfb8uvwn3bfcsxc8vy3clj FOREIGN KEY (prioridad_id) REFERENCES public.cat_prioridad(id)`.
- `fkof8a43swfcvf2dv5mo6q5thd6 FOREIGN KEY (abogado_responsable_id) REFERENCES public.usuarios(id)`.
- `fkq1uvxy2ws1qxvxrgqovf9tq1s FOREIGN KEY (tipo_expediente_id) REFERENCES public.cat_tipo_expediente(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Campos fuera de captura ordinaria: `id`, `abogado_responsable_nombre`, `created_at`, `updated_at`, `es_temporal`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Contextos de audiencia

## Descripción

Captura de contextos de audiencia para carga inicial o migración controlada.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_audiencia_contextos_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `audiencia_contextos` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| descripcion | Descripcion | Texto | Sí (BD) | Sin límite declarado | Seguimiento del asunto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| numero_expediente_snapshot | Numero expediente snapshot | Texto | No | 50 | Texto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| organo_jurisdiccional_texto | Organo jurisdiccional texto | Texto | No | Sin límite declarado | Texto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| origen | Origen | Texto | Sí (BD) | 30 | EXPEDIENTE | Permitidos: EXPEDIENTE, ASUNTO_UNICO. Texto literal; conservar ceros iniciales en identificadores. |
| partes | Identificación textual de las partes procesales | Texto | No | Sin límite declarado | Actor: Persona A; Demandado: Entidad B | Texto literal; conservar ceros iniciales en identificadores. |
| sede | Sede | Texto | No | 150 | Ciudad de México | Texto literal; conservar ceros iniciales en identificadores. |
| gerencia_id | Referencia a Gerencias | Entero | Sí (BD) | No aplica | 1 | Debe existir en gerencias; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| materia_id | Referencia a Materias | Entero | Sí (BD) | No aplica | 1 | Debe existir en materias; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| organo_jurisdiccional_id | Referencia a Órganos jurisdiccionales | Entero | No | No aplica | 1 | Debe existir en organos_jurisdiccionales; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |

## Dependencias

- gerencia_id → gerencias.id. Cargar previamente o resolver en el mismo proceso controlado.
- organo_jurisdiccional_id → organos_jurisdiccionales.id. Cargar previamente o resolver en el mismo proceso controlado.
- materia_id → materias.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Captura complementaria de materia, gerencia, descripción y partes. No compartir contexto entre audiencias: UNIQUE contexto_id en audiencias. Puede generarse como snapshot desde expediente.

Llaves y restricciones observadas:

- `audiencia_contextos_pkey PRIMARY KEY (id)`.
- `fkbdu4j8d2ejho98oi08369endx FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id)`.
- `fkp1paxjyvpxulyqnukqmd0d13c FOREIGN KEY (organo_jurisdiccional_id) REFERENCES public.organos_jurisdiccionales(id)`.
- `fktfcon5wnhwo0ph22a2wt5y6im FOREIGN KEY (materia_id) REFERENCES public.materias(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Campos fuera de captura ordinaria: `id`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Contextos de término

## Descripción

Captura de contextos de término para carga inicial o migración controlada.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_termino_contextos_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `termino_contextos` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| descripcion | Descripcion | Texto | Sí (BD) | Sin límite declarado | Seguimiento del asunto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| numero_expediente_snapshot | Numero expediente snapshot | Texto | No | 50 | Texto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| organo_jurisdiccional_texto | Organo jurisdiccional texto | Texto | No | Sin límite declarado | Texto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| origen | Origen | Texto | Sí (BD) | 30 | EXPEDIENTE | Permitidos: EXPEDIENTE, ASUNTO_UNICO. Texto literal; conservar ceros iniciales en identificadores. |
| partes | Identificación textual de las partes procesales | Texto | No | Sin límite declarado | Actor: Persona A; Demandado: Entidad B | Texto literal; conservar ceros iniciales en identificadores. |
| sede | Sede | Texto | No | 150 | Ciudad de México | Texto literal; conservar ceros iniciales en identificadores. |
| gerencia_id | Referencia a Gerencias | Entero | Sí (BD) | No aplica | 1 | Debe existir en gerencias; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| materia_id | Referencia a Materias | Entero | Sí (BD) | No aplica | 1 | Debe existir en materias; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| organo_jurisdiccional_id | Referencia a Órganos jurisdiccionales | Entero | No | No aplica | 1 | Debe existir en organos_jurisdiccionales; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |

## Dependencias

- materia_id → materias.id. Cargar previamente o resolver en el mismo proceso controlado.
- organo_jurisdiccional_id → organos_jurisdiccionales.id. Cargar previamente o resolver en el mismo proceso controlado.
- gerencia_id → gerencias.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Captura complementaria para origen EXPEDIENTE o ASUNTO_UNICO. No compartir contexto entre términos: UNIQUE contexto_id en terminos.

Llaves y restricciones observadas:

- `termino_contextos_pkey PRIMARY KEY (id)`.
- `fk6bqlx8xoy5y68c306ugj3x9it FOREIGN KEY (materia_id) REFERENCES public.materias(id)`.
- `fkhcuyhk60prwmu8wdx5lq7dw8t FOREIGN KEY (organo_jurisdiccional_id) REFERENCES public.organos_jurisdiccionales(id)`.
- `fkiyg3nqpnf53flv1ovxvp94t5h FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Campos fuera de captura ordinaria: `id`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Audiencias

## Descripción

Programación y seguimiento de comparecencias vinculadas a un expediente.

Estado: **Layout completo propuesto; API histórica parcial existente**.

## Archivo sugerido

`SIAJ_audiencias_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `audiencias` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| acta_documento | Acta documento | Texto | No | 500 | Texto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| es_virtual | Indica audiencia por videoconferencia | Booleano | No | No aplica | true | true o false; no sustituir vacío por false sin regla. |
| estatus_audiencia | Estatus audiencia | Texto | No | 30 | PENDIENTE | Permitidos: PENDIENTE, CON_ACTA, CONCLUIDA. Texto literal; conservar ceros iniciales en identificadores. |
| fecha_audiencia | Fecha audiencia | Fecha | Sí (BD) | No aplica | 2026-09-15 | AAAA-MM-DD, fecha real; no fórmulas. |
| fecha_desahogo | Fecha desahogo | Fecha | No | No aplica | 2026-09-15 | AAAA-MM-DD, fecha real; no fórmulas. |
| hora_audiencia | Hora audiencia | Hora | Sí (BD) | No aplica | 09:30 | HH:mm, reloj de 24 horas. |
| observaciones | Observaciones | Texto | No | Sin límite declarado | Texto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| sala_lugar | Sala lugar | Texto | No | Sin límite declarado | Sala 1 | Texto literal; conservar ceros iniciales en identificadores. |
| url_reunion | Url reunion | Texto | No | Sin límite declarado | https://example.org/reunion/123 | Texto literal; conservar ceros iniciales en identificadores. |
| abogado_comparece_id | Referencia a Usuarios | Entero | No | No aplica | 1 | Debe existir en usuarios; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| expediente_id | Referencia a Expedientes | UUID | Sí (BD) | No aplica | 00000000-0000-4000-8000-000000000001 | Debe existir en expedientes; consultar hoja Catalogos. |
| tipo_audiencia_id | Referencia a Tipos de audiencia | Entero | Sí (controlador) | No aplica | 1 | Debe existir en tipos_audiencia; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| contexto_id | Referencia a Contextos de audiencia | Entero 64 bits | No | No aplica | 1 | Debe existir en audiencia_contextos; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| usuario_delegado_id | Referencia a Usuarios | Entero | No | No aplica | 1 | Debe existir en usuarios; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |

## Dependencias

- expediente_id → expedientes.id. Cargar previamente o resolver en el mismo proceso controlado.
- tipo_audiencia_id → tipos_audiencia.id. Cargar previamente o resolver en el mismo proceso controlado.
- usuario_delegado_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.
- contexto_id → audiencia_contextos.id. Cargar previamente o resolver en el mismo proceso controlado.
- abogado_comparece_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Fecha y hora obligatorias en BD; tipo requerido por controlador aunque nullable en BD. expediente_id NOT NULL en dump.
- Estado: PENDIENTE, CON_ACTA, CONCLUIDA. Para estados con acta/desahogo, validar evidencia mediante el flujo correspondiente.
- Contexto es único por audiencia. El esquema admite contexto ASUNTO_UNICO pero no permite expediente_id nulo: no habilitar audiencia independiente sin resolver esa contradicción.
- Propuesta de calidad: si es_virtual=true exigir URL de reunión; validar que compareciente y delegado tengan acceso al expediente.

Llaves y restricciones observadas:

- `audiencias_pkey PRIMARY KEY (id)`.
- `uk6yco257iom67jk0pilkinvda3 UNIQUE (contexto_id)`.
- `fk4csl8qte7scamolg89kofnal3 FOREIGN KEY (expediente_id) REFERENCES public.expedientes(id)`.
- `fk7wchckhcv7ce6x99xekj5e9dx FOREIGN KEY (tipo_audiencia_id) REFERENCES public.tipos_audiencia(id)`.
- `fkgsv5padg0wab4wkm8no4mfx59 FOREIGN KEY (usuario_delegado_id) REFERENCES public.usuarios(id)`.
- `fkgy6f4qct07n42s9vecon4e792 FOREIGN KEY (contexto_id) REFERENCES public.audiencia_contextos(id)`.
- `fkpkiu3klwkc0lr9f9ebbpwsqc1 FOREIGN KEY (abogado_comparece_id) REFERENCES public.usuarios(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- Confirmar la ruta y ejecutar primero una carga de ensayo con conciliación de resultados.
- Campos fuera de captura ordinaria: `id`, `created_at`, `updated_at`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Términos

## Descripción

Control de actuaciones y vencimientos, responsables y estado de revisión/presentación.

Estado: **Layout completo propuesto; API histórica parcial existente**.

## Archivo sugerido

`SIAJ_terminos_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `terminos` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| acuse | Acuse | Texto | No | 500 | Texto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| acuse_nombre | Acuse nombre | Texto | No | 500 | Texto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| asunto | Asunto | Texto | Sí (controlador) | Sin límite declarado | Presentar contestación | Texto literal; conservar ceros iniciales en identificadores. |
| estatus_termino | Estatus termino | Texto | Sí (BD) | 30 | PROYECTISTA | Permitidos: PROYECTISTA, REVISION, GERENCIA, DIRECCION, LIBERADO, PRESENTADO, CONCLUIDO. Texto literal; conservar ceros iniciales en identificadores. |
| fecha_ingreso | Fecha de ingreso de la actuación | Fecha | No | No aplica | 2026-09-15 | AAAA-MM-DD, fecha real; no fórmulas. |
| fecha_presentacion | Fecha presentacion | Fecha | No | No aplica | 2026-09-15 | AAAA-MM-DD, fecha real; no fórmulas. |
| fecha_vencimiento | Fecha límite registrada para la actuación | Fecha | Sí (BD) | No aplica | 2026-09-15 | AAAA-MM-DD, fecha real; no fórmulas. |
| observaciones | Observaciones | Texto | No | Sin límite declarado | Texto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| abogado_responsable_id | Referencia a Usuarios | Entero | No | No aplica | 1 | Debe existir en usuarios; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| expediente_id | Referencia a Expedientes | UUID | Condicional: origen del asunto | No aplica | 00000000-0000-4000-8000-000000000001 | Debe existir en expedientes; consultar hoja Catalogos. |
| prioridad_id | Referencia a Prioridades | Entero | No | No aplica | 1 | Debe existir en cat_prioridad; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| contexto_id | Referencia a Contextos de término | Entero 64 bits | Condicional: origen del asunto | No aplica | 1 | Debe existir en termino_contextos; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| tipo_termino_id | Tipo configurable para plazo | Entero | No | No aplica | 1 | Sólo JPA; no existe en el dump. Validar despliegue y cálculo antes de importar. Sin decimales; IDs del ambiente destino. |
| es_tiempo_dinamico | Es tiempo dinamico | Booleano | No | No aplica | false | Sólo JPA; no existe en el dump. Validar despliegue y cálculo antes de importar. true o false; no sustituir vacío por false sin regla. |
| tipo_dias_calculado | Tipo dias calculado | Texto | No | 20 | HABILES | Sólo JPA; no existe en el dump. Validar despliegue y cálculo antes de importar. Permitidos: HABILES, NATURALES. Texto literal; conservar ceros iniciales en identificadores. |
| desglose_dias_texto | Desglose dias texto | Texto | No | Sin límite declarado | Texto de ejemplo | Sólo JPA; no existe en el dump. Validar despliegue y cálculo antes de importar. Texto literal; conservar ceros iniciales en identificadores. |

## Dependencias

- contexto_id → termino_contextos.id. Cargar previamente o resolver en el mismo proceso controlado.
- prioridad_id → cat_prioridad.id. Cargar previamente o resolver en el mismo proceso controlado.
- expediente_id → expedientes.id. Cargar previamente o resolver en el mismo proceso controlado.
- abogado_responsable_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.
- tipo_termino_id → cat_tipos_termino.id (JPA). Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Asunto obligatorio en controlador aunque nullable en BD. Vencimiento obligatorio.
- Puede vincularse a expediente o a contexto de ASUNTO_UNICO. Para asunto único, controlador exige materia, gerencia y descripción. No exigir siempre expediente a partir del DTO legado asuntoId Integer.
- Estados: PROYECTISTA, REVISION, GERENCIA, DIRECCION, LIBERADO, PRESENTADO, CONCLUIDO. Conservar transición y permisos del flujo. PRESENTADO debe acompañarse de presentación y acuse validados.
- prioridad_id es relación usada por JPA. Las columnas prioridad y archivo_word del dump no están mapeadas en Termino actual; no cargarlas directamente.
- Cálculo dinámico y tipo de término son añadidos JPA: validar esquema desplegado, tipo de días y calendario; no inferir vencimiento legal automáticamente desde esta documentación.

Llaves y restricciones observadas:

- `terminos_pkey PRIMARY KEY (id)`.
- `ukjcdu631eit28n3wdtg3y1elgb UNIQUE (contexto_id)`.
- `fk2dw1fq3hmoqxkj4o25awskock FOREIGN KEY (contexto_id) REFERENCES public.termino_contextos(id)`.
- `fk4gjcyyda149hmwyj6v7fjcot2 FOREIGN KEY (prioridad_id) REFERENCES public.cat_prioridad(id)`.
- `fkcoe42fxbdjqjek2ks0ghkmowd FOREIGN KEY (expediente_id) REFERENCES public.expedientes(id)`.
- `fko59hv5s9ul7bqg35p4oanxlkr FOREIGN KEY (abogado_responsable_id) REFERENCES public.usuarios(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- Confirmar la ruta y ejecutar primero una carga de ensayo con conciliación de resultados.
- Campos fuera de captura ordinaria: `id`, `created_at`, `updated_at`, `fecha_creacion`, `archivo_word`, `prioridad`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Colaboradores de expediente

## Descripción

Captura de colaboradores de expediente para carga inicial o migración controlada.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_colaboradores_expediente_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `colaboradores_expediente` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| fecha_expiracion | Fecha expiracion | Fecha y hora | Sí (BD) | No aplica | 2026-09-15T09:30:00 | AAAA-MM-DDTHH:mm:ss; confirmar zona del servidor. |
| motivo | Motivo | Texto | No | Sin límite declarado | Texto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| permiso_nivel | Permiso nivel | Texto | No | 255 | Pendiente de catálogo | Texto literal; conservar ceros iniciales en identificadores. |
| expediente_id | Referencia a Expedientes | UUID | Sí (BD) | No aplica | 00000000-0000-4000-8000-000000000001 | Debe existir en expedientes; consultar hoja Catalogos. |
| usuario_id | Referencia a Usuarios | Entero | Sí (BD) | No aplica | 1 | Debe existir en usuarios; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |

## Dependencias

- usuario_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.
- expediente_id → expedientes.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Expediente, usuario y fecha de expiración obligatorios. permiso_nivel es texto sin CHECK; exportar valores usados por el flujo antes de habilitarlo. Propuesta: impedir duplicación de colaboración activa del mismo usuario/expediente.

Llaves y restricciones observadas:

- `colaboradores_expediente_pkey PRIMARY KEY (id)`.
- `fkfmyo4j16g3shpn1jqc846sril FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`.
- `fkmajsh6pilw2o7d2lusf1yfewt FOREIGN KEY (expediente_id) REFERENCES public.expedientes(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Campos fuera de captura ordinaria: `id`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Carpetas

## Descripción

Captura de carpetas para carga inicial o migración controlada.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_carpetas_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `carpetas` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| expediente_id | Expediente id | UUID | Sí (BD) | No aplica | 00000000-0000-4000-8000-000000000001 | Validar definición funcional antes de importar. |
| nombre | Nombre | Texto | Sí (BD) | 255 | Valor del catálogo | Texto literal; conservar ceros iniciales en identificadores. |
| orden | Orden | Entero | Sí (BD) | No aplica | 1 | Sin decimales; IDs del ambiente destino. |
| carpeta_padre_id | Referencia a Carpetas | UUID | No | No aplica | 00000000-0000-4000-8000-000000000001 | Debe existir en carpetas; consultar hoja Catalogos. |

## Dependencias

- usuario_creador_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.
- carpeta_padre_id → carpetas.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Padre previamente cargado, mismo expediente y sin ciclos (regla de importación propuesta). Carpeta de sistema, orden técnico y fecha de creación se generan mediante servicio.

Llaves y restricciones observadas:

- `carpetas_pkey PRIMARY KEY (id)`.
- `fk7oriagmf0utemgy3ub8iy7myt FOREIGN KEY (usuario_creador_id) REFERENCES public.usuarios(id)`.
- `fkel65g9qwjoep94aixiveex7fm FOREIGN KEY (carpeta_padre_id) REFERENCES public.carpetas(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Campos fuera de captura ordinaria: `id`, `es_sistema`, `fecha_creacion`, `usuario_creador_id`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Documentos de expediente

## Descripción

Manifiesto para incorporar archivos al repositorio del expediente.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_documentos_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `documentos` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| descripcion | Descripcion | Texto | No | Sin límite declarado | Seguimiento del asunto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| expediente_id | Expediente id | UUID | Sí (BD) | No aplica | 00000000-0000-4000-8000-000000000001 | Validar definición funcional antes de importar. |
| nombre_original | Nombre original | Texto | Sí (BD) | 255 | acuse-001.pdf | Texto literal; conservar ceros iniciales en identificadores. |
| carpeta_id | Referencia a Carpetas | UUID | No | No aplica | 00000000-0000-4000-8000-000000000001 | Debe existir en carpetas; consultar hoja Catalogos. |
| archivo_relativo | Archivo a incorporar | Texto | Sí (propuesta) | 255 (propuesta) | documentos/acuse-001.pdf | Archivo real dentro del paquete; sin rutas absolutas ni ../. El importador almacena y genera metadatos. |

## Dependencias

- usuario_eliminacion_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.
- carpeta_id → carpetas.id. Cargar previamente o resolver en el mismo proceso controlado.
- usuario_subida_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Entregar manifiesto más archivos reales. Nombre/ruta/tamaño internos se generan al almacenar; el texto de una ruta no sube el archivo.
- FileStorageService limita documentos a 150 MiB, imágenes a 30 MiB y audio/video a 1 GiB. Verificar además límite HTTP/proxy de destino.
- Carpeta debe corresponder al expediente. No importar flags de papelera ni rutas absolutas del equipo de origen como rutas del servidor.

Llaves y restricciones observadas:

- `documentos_pkey PRIMARY KEY (id)`.
- `fkapnjaa6i0jmcmp0adsrclumti FOREIGN KEY (usuario_eliminacion_id) REFERENCES public.usuarios(id)`.
- `fki77q0aw3d7cix0fty95p5halt FOREIGN KEY (carpeta_id) REFERENCES public.carpetas(id)`.
- `fkou5fwjxrh4strb85eehbd3535 FOREIGN KEY (usuario_subida_id) REFERENCES public.usuarios(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Campos fuera de captura ordinaria: `id`, `created_at`, `fecha_subida`, `nombre_archivo`, `ruta_archivo`, `tamanio_bytes`, `tipo_mime`, `updated_at`, `usuario_subida_id`, `carpeta_original_id`, `carpeta_original_ruta`, `eliminado`, `eliminado_definitivo`, `fecha_eliminacion`, `fecha_eliminacion_definitiva`, `usuario_eliminacion_id`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Archivos de término

## Descripción

Captura de archivos de término para carga inicial o migración controlada.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_termino_archivos_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `termino_archivos` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| etapa | Etapa | Texto | Sí (BD) | 30 | PROYECTISTA | Permitidos: PROYECTISTA, REVISION, GERENCIA, DIRECCION, LIBERADO, PRESENTADO, CONCLUIDO. Texto literal; conservar ceros iniciales en identificadores. |
| nombre_original | Nombre original | Texto | Sí (BD) | 255 | acuse-001.pdf | Texto literal; conservar ceros iniciales en identificadores. |
| tipo_documento | Tipo documento | Texto | Sí (BD) | 20 | BORRADOR | Permitidos: BORRADOR, ACUSE. Texto literal; conservar ceros iniciales en identificadores. |
| termino_id | Referencia a Términos | Entero | Sí (BD) | No aplica | 1 | Debe existir en terminos; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| archivo_relativo | Archivo a incorporar | Texto | Sí (propuesta) | 255 (propuesta) | documentos/acuse-001.pdf | Archivo real dentro del paquete; sin rutas absolutas ni ../. El importador almacena y genera metadatos. |

## Dependencias

- termino_id → terminos.id. Cargar previamente o resolver en el mismo proceso controlado.
- usuario_subida_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Manifiesto y binarios. BORRADOR/ACUSE son valores del enum. Etapa usa estados de término. Validar permisos y flujo al subir acuse.
- Ruta, tamaño, MIME y autor se registran por el servicio de almacenamiento; preservar metadatos históricos sólo mediante migración controlada.

Llaves y restricciones observadas:

- `termino_archivos_pkey PRIMARY KEY (id)`.
- `fkhgdafpg9sqy4788ma60xfixbx FOREIGN KEY (termino_id) REFERENCES public.terminos(id)`.
- `fklrxl92lqj7h66ktvsk2bdw68u FOREIGN KEY (usuario_subida_id) REFERENCES public.usuarios(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Campos fuera de captura ordinaria: `id`, `created_at`, `ruta_archivo`, `tamanio_bytes`, `tipo_mime`, `usuario_subida_id`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Comentarios y observaciones

## Descripción

Captura de comentarios y observaciones para carga inicial o migración controlada.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_comentarios_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `comentarios` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| comentario | Comentario | Texto | Sí (BD) | Sin límite declarado | Texto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| entidad_id | Entidad id | Texto | Sí (BD) | 50 | 00000000-0000-4000-8000-000000000001 | Texto literal; conservar ceros iniciales en identificadores. |
| entidad_tipo | Entidad tipo | Texto | Sí (BD) | 30 | EXPEDIENTE | Texto literal; conservar ceros iniciales en identificadores. |
| etapa_procesal | Etapa procesal | Texto | No | 30 | TRAMITE | Permitidos: TRAMITE, LAUDO, FIRME, CONCLUIDO. Texto literal; conservar ceros iniciales en identificadores. |
| usuario_id | Referencia a Usuarios | Entero | No | No aplica | 1 | Debe existir en usuarios; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |

## Dependencias

- usuario_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- entidad_id es texto polimórfico sin FK: validar manualmente tipo y existencia de entidad. Usuario y fecha deben conservarse sólo para migración histórica autorizada.
- etapa_procesal acepta sólo enum de expediente; no confundir con estados de término.

Llaves y restricciones observadas:

- `comentarios_pkey PRIMARY KEY (id)`.
- `fkdts62yj83qe3k748cgcjvm48r FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Campos fuera de captura ordinaria: `id`, `created_at`, `usuario_nombre`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Tareas y recordatorios

## Descripción

Captura de tareas y recordatorios para carga inicial o migración controlada.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_recordatorios_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `recordatorios` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| completado | Completado | Booleano | No | No aplica | true | true o false; no sustituir vacío por false sin regla. |
| detalles | Detalles | Texto | No | Sin límite declarado | Texto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| fecha_recordatorio | Fecha recordatorio | Fecha | Sí (BD) | No aplica | 2026-09-15 | AAAA-MM-DD, fecha real; no fórmulas. |
| hora_recordatorio | Hora recordatorio | Hora | Sí (BD) | No aplica | 09:30 | HH:mm, reloj de 24 horas. |
| titulo | Titulo | Texto | Sí (BD) | 200 | Revisar escrito | Texto literal; conservar ceros iniciales en identificadores. |
| prioridad_id | Referencia a Prioridades | Entero | No | No aplica | 1 | Debe existir en cat_prioridad; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| usuario_id | Referencia a Usuarios | Entero | Sí (BD) | No aplica | 1 | Debe existir en usuarios; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |

## Dependencias

- prioridad_id → cat_prioridad.id. Cargar previamente o resolver en el mismo proceso controlado.
- usuario_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Título, fecha, hora y usuario obligatorios. DTO usa prioridad normal/urgent, mientras BD usa prioridad_id: definir equivalencia explícita, no asumir identidad de códigos.

Llaves y restricciones observadas:

- `recordatorios_pkey PRIMARY KEY (id)`.
- `fk476hi0yoplmls066xtrc5otb FOREIGN KEY (prioridad_id) REFERENCES public.cat_prioridad(id)`.
- `fkc52iceqg5fu1wn3jrijliai38 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Campos fuera de captura ordinaria: `id`, `created_at`, `updated_at`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Eventos de calendario

## Descripción

Captura de eventos de calendario para carga inicial o migración controlada.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_eventos_calendario_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `eventos_calendario` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| categoria | Categoria | Texto | Sí (BD) | 30 | Pendiente de catálogo | Texto literal; conservar ceros iniciales en identificadores. |
| color | Color | Texto | No | 7 | bg-red-100 text-red-800 | Texto literal; conservar ceros iniciales en identificadores. |
| descripcion | Descripcion | Texto | No | Sin límite declarado | Seguimiento del asunto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| entidad_id | Entidad id | Entero | No | No aplica | 00000000-0000-4000-8000-000000000001 | Sin decimales; IDs del ambiente destino. |
| entidad_tipo | Entidad tipo | Texto | No | 30 | EXPEDIENTE | Texto literal; conservar ceros iniciales en identificadores. |
| fecha_fin | Fecha fin | Fecha y hora | No | No aplica | 2026-09-15T09:30:00 | AAAA-MM-DDTHH:mm:ss; confirmar zona del servidor. |
| fecha_inicio | Fecha inicio | Fecha y hora | Sí (BD) | No aplica | 2026-09-15T09:30:00 | AAAA-MM-DDTHH:mm:ss; confirmar zona del servidor. |
| titulo | Titulo | Texto | Sí (BD) | 200 | Revisar escrito | Texto literal; conservar ceros iniciales en identificadores. |
| todo_el_dia | Todo el dia | Booleano | No | No aplica | true | true o false; no sustituir vacío por false sin regla. |
| usuario_id | Referencia a Usuarios | Entero | No | No aplica | 1 | Debe existir en usuarios; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |

## Dependencias

- usuario_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- categoría es texto sin CHECK. Confirmar valores del servicio antes de importar. No duplicar eventos derivados de audiencias/términos.
- Propuesta: fecha_fin >= fecha_inicio. entidad_id es Integer: no admite directamente UUID de expediente.

Llaves y restricciones observadas:

- `eventos_calendario_pkey PRIMARY KEY (id)`.
- `fkkerk9013d75pe0retvvpoali4 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Campos fuera de captura ordinaria: `id`, `created_at`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Configuraciones de alertas

## Descripción

Captura de configuraciones de alertas para carga inicial o migración controlada.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_configuraciones_alerta_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `configuraciones_alerta` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| activa | Activa | Booleano | Sí (BD) | No aplica | true | true o false; no sustituir vacío por false sin regla. |
| asunto_id | Asunto id | Texto | No | 100 | Texto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| asunto_tipo | Asunto tipo | Texto | No | 30 | Texto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| dias_anticipacion | Dias anticipacion | Entero | Sí (BD) | No aplica | 1 | Sin decimales; IDs del ambiente destino. |
| nivel | Nivel | Texto | Sí (BD) | 30 | SISTEMA | Permitidos: SISTEMA, GERENCIA, USUARIO, ASUNTO. Texto literal; conservar ceros iniciales en identificadores. |
| obligatoria | Obligatoria | Booleano | Sí (BD) | No aplica | true | true o false; no sustituir vacío por false sin regla. |
| tipo_evento | Tipo evento | Texto | Sí (BD) | 30 | TERMINO | Permitidos: TERMINO, AUDIENCIA. Texto literal; conservar ceros iniciales en identificadores. |
| gerencia_id | Referencia a Gerencias | Entero | No | No aplica | 1 | Debe existir en gerencias; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |
| usuario_id | Referencia a Usuarios | Entero | No | No aplica | 1 | Debe existir en usuarios; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |

## Dependencias

- gerencia_id → gerencias.id. Cargar previamente o resolver en el mismo proceso controlado.
- usuario_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Una fila por día de anticipación. Servicio admite días 0..365. Tipo TERMINO o AUDIENCIA.
- Nivel SISTEMA, GERENCIA, USUARIO o ASUNTO; identificar gerencia/usuario/asunto según nivel. Definir precedencia mediante servicio, no insertar reglas contradictorias.

Llaves y restricciones observadas:

- `configuraciones_alerta_pkey PRIMARY KEY (id)`.
- `fkjuixqrp11exyu0hxda62mjaly FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id)`.
- `fkp2ofjofsj1ohidlux3q1jylxr FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Campos fuera de captura ordinaria: `id`, `created_at`, `updated_at`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Configuraciones de semáforo

## Descripción

Captura de configuraciones de semáforo para carga inicial o migración controlada.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_configuraciones_semaforo_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `configuraciones_semaforo` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| amarillo_hasta | Amarillo hasta | Entero | Sí (BD) | No aplica | 1 | Sin decimales; IDs del ambiente destino. |
| nivel | Nivel | Texto | Sí (BD) | 30 | SISTEMA | Permitidos: SISTEMA, GERENCIA. Texto literal; conservar ceros iniciales en identificadores. |
| rojo_hasta | Rojo hasta | Entero | Sí (BD) | No aplica | 1 | Sin decimales; IDs del ambiente destino. |
| tipo_evento | Tipo evento | Texto | Sí (BD) | 30 | TERMINO | Permitidos: TERMINO, AUDIENCIA. Texto literal; conservar ceros iniciales en identificadores. |
| gerencia_id | Referencia a Gerencias | Entero | No | No aplica | 1 | Debe existir en gerencias; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |

## Dependencias

- gerencia_id → gerencias.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Servicio sólo admite SISTEMA y GERENCIA aunque CHECK incluye USUARIO y ASUNTO. Gerencia requerida para nivel GERENCIA.
- rojo_hasta >= 0 y amarillo_hasta > rojo_hasta. Persistir usando servicio para conservar unicidad lógica por tipo/nivel/gerencia.

Llaves y restricciones observadas:

- `configuraciones_semaforo_pkey PRIMARY KEY (id)`.
- `fkpncb84gi9iflcalxgkyf6chdk FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Campos fuera de captura ordinaria: `id`, `created_at`, `updated_at`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Preferencias generales de notificación

## Descripción

Captura de preferencias generales de notificación para carga inicial o migración controlada.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_preferencias_notificacion_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `preferencias_notificacion` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| notificar_nueva_audiencia | Notificar nueva audiencia | Booleano | No | No aplica | true | true o false; no sustituir vacío por false sin regla. |
| notificar_nuevo_termino | Notificar nuevo termino | Booleano | No | No aplica | true | true o false; no sustituir vacío por false sin regla. |
| notificar_reasignacion | Notificar reasignacion | Booleano | No | No aplica | true | true o false; no sustituir vacío por false sin regla. |
| notificar_termino_vencer | Notificar termino vencer | Booleano | No | No aplica | true | true o false; no sustituir vacío por false sin regla. |
| usuario_id | Referencia a Usuarios | Entero | Sí (BD) | No aplica | 1 | Debe existir en usuarios; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |

## Dependencias

- usuario_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Una preferencia por usuario según UNIQUE. Modelo general previo al esquema por evento: confirmar cuál prevalece.

Llaves y restricciones observadas:

- `preferencias_notificacion_pkey PRIMARY KEY (id)`.
- `uke1fpwe1mmm24nh837d91ymwnh UNIQUE (usuario_id)`.
- `fk401s19rwxg52aeg4lnbgdno3d FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Campos fuera de captura ordinaria: `id`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Preferencias por evento

## Descripción

Captura de preferencias por evento para carga inicial o migración controlada.

Estado: **Diseño propuesto; requiere importador**.

## Archivo sugerido

`SIAJ_preferencias_notificacion_evento_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `preferencias_notificacion_event` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| evento | Evento | Texto | Sí (BD) | 80 | EXPEDIENTE_NUEVO | Permitidos: EXPEDIENTE_NUEVO, EXPEDIENTE_REASIGNADO, EXPEDIENTE_OBSERVACION_NUEVA, TERMINO_ASIGNADO, TERMINO_NUEVO, TERMINO_CAMBIO_ETAPA, TERMINO_VALIDADO_CONCLUIDO, TERMINO_RECORDATORIO, AUDIENCIA_NUEVA, AUDIENCIA_COMPARECENCIA_ASIGNADA, AUDIENCIA_DELEGADA, AUDIENCIA_RECORDATORIO, AUDIENCIA_ACTA_DISPONIBLE, AUDIENCIA_VALIDADA_CONCLUIDA, SEGURIDAD_PASSWORD_POR_VENCER, TAREA_RECORDATORIO. Texto literal; conservar ceros iniciales en identificadores. |
| habilitada | Habilitada | Booleano | Sí (BD) | No aplica | true | true o false; no sustituir vacío por false sin regla. |
| usuario_id | Referencia a Usuarios | Entero | Sí (BD) | No aplica | 1 | Debe existir en usuarios; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |

## Dependencias

- usuario_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Par (usuario_id, evento) único. Validar contra enum y CHECK; no cambiar notificaciones de seguridad obligatorias por un importador genérico sin comprobar servicio.

Llaves y restricciones observadas:

- `preferencias_notificacion_evento_pkey PRIMARY KEY (id)`.
- `uk_pref_notif_evento_usuario UNIQUE (usuario_id, evento)`.
- `fkq824i3pvktkmtaa0m7l1g1ry0 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Campos fuera de captura ordinaria: `id`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Historial de etapas de expediente

## Descripción

Captura de historial de etapas de expediente para carga inicial o migración controlada.

Estado: **Sólo migración histórica controlada**.

## Archivo sugerido

`SIAJ_expediente_etapa_periodos_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `expediente_etapa_periodos` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| expediente_id | Expediente id | UUID | Sí (JPA) | No aplica | 00000000-0000-4000-8000-000000000001 | Validar definición funcional antes de importar. |
| etapa_procesal | Etapa procesal | Texto | Sí (JPA) | 30 | TRAMITE | Permitidos: TRAMITE, LAUDO, FIRME, CONCLUIDO. Texto literal; conservar ceros iniciales en identificadores. |
| etapa_catalogo_id | Etapa configurable de la materia | Entero | No | No aplica | 1 | Sin decimales; IDs del ambiente destino. |
| fecha_inicio | Fecha inicio | Fecha y hora | Sí (JPA) | No aplica | 2026-09-15T09:30:00 | AAAA-MM-DDTHH:mm:ss; confirmar zona del servidor. |
| fecha_fin | Fecha fin | Fecha y hora | No | No aplica | 2026-09-15T09:30:00 | AAAA-MM-DDTHH:mm:ss; confirmar zona del servidor. |
| usuario_inicio_id | Usuario inicio id | Entero | No | No aplica | 1 | Sin decimales; IDs del ambiente destino. |
| usuario_fin_id | Usuario fin id | Entero | No | No aplica | 1 | Sin decimales; IDs del ambiente destino. |
| motivo_cambio | Motivo cambio | Texto | No | Sin límite declarado | Texto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |

## Dependencias

- expediente_id → expedientes.id. Cargar previamente o resolver en el mismo proceso controlado.
- etapa_catalogo_id → cat_etapas_procesales.id. Cargar previamente o resolver en el mismo proceso controlado.
- usuario_inicio_id / usuario_fin_id → usuarios.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Histórico técnico generado por cambios de etapa. Sólo migración histórica, no alta ordinaria. Propuesta: fin >= inicio, sin períodos superpuestos y un período abierto por expediente.

Llaves y restricciones observadas:

- `PK id BIGINT generado (JPA)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Evidencia: `Entity/expediente/ExpedienteEtapaPeriodo.java`.

# Entidad: Audiencias desahogadas

## Descripción

Captura de audiencias desahogadas para carga inicial o migración controlada.

Estado: **Sólo migración histórica controlada**.

## Archivo sugerido

`SIAJ_audiencias_desahogadas_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `audiencias_desahogadas` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| expediente_numero | Expediente numero | Texto | No | 50 | Texto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| fecha_audiencia | Fecha audiencia | Fecha | Sí (BD) | No aplica | 2026-09-15 | AAAA-MM-DD, fecha real; no fórmulas. |
| fecha_desahogo | Fecha desahogo | Fecha | Sí (BD) | No aplica | 2026-09-15 | AAAA-MM-DD, fecha real; no fórmulas. |
| hora_audiencia | Hora audiencia | Hora | Sí (BD) | No aplica | 09:30 | HH:mm, reloj de 24 horas. |
| audiencia_id | Referencia a Audiencias | Entero | Sí (BD) | No aplica | 1 | Debe existir en audiencias; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |

## Dependencias

- audiencia_id → audiencias.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Tabla de sincronización/resultado. Generar mediante flujo de desahogo; no carga paralela de usuario. Migración histórica debe conciliar audiencia y fechas. No hay UNIQUE audiencia_id en dump.

Llaves y restricciones observadas:

- `audiencias_desahogadas_pkey PRIMARY KEY (id)`.
- `fkq40pjs1rrg3mj5qdcl74hawwa FOREIGN KEY (audiencia_id) REFERENCES public.audiencias(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Campos fuera de captura ordinaria: `id`, `sincronizado_at`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Términos presentados

## Descripción

Captura de términos presentados para carga inicial o migración controlada.

Estado: **Sólo migración histórica controlada**.

## Archivo sugerido

`SIAJ_terminos_presentados_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `terminos_presentados` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| acuse_documento | Acuse documento | Texto | No | 500 | Texto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| expediente_numero | Expediente numero | Texto | No | 50 | Texto de ejemplo | Texto literal; conservar ceros iniciales en identificadores. |
| fecha_presentacion | Fecha presentacion | Fecha | Sí (BD) | No aplica | 2026-09-15 | AAAA-MM-DD, fecha real; no fórmulas. |
| termino_id | Referencia a Términos | Entero | Sí (BD) | No aplica | 1 | Debe existir en terminos; consultar hoja Catalogos. Sin decimales; IDs del ambiente destino. |

## Dependencias

- termino_id → terminos.id. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Tabla de sincronización/resultado. Generar mediante flujo de presentación; no carga paralela de usuario. Migración histórica debe conciliar término, fecha y acuse. No hay UNIQUE termino_id en dump.

Llaves y restricciones observadas:

- `terminos_presentados_pkey PRIMARY KEY (id)`.
- `fk1brocrvr9fxbfek662npjjn5l FOREIGN KEY (termino_id) REFERENCES public.terminos(id)`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Campos fuera de captura ordinaria: `id`, `sincronizado_at`. Son generados, derivados, legados o exclusivos de migración técnica; ver inventario.
- Evidencia: `db_schema_actual.sql` y servicios/controladores indicados en las reglas.

# Entidad: Términos: Excel legado

## Descripción

Contrato de siete columnas leído por TerminoImportService; servicio existente sin invocación desde controlador encontrada.

Estado: **Servicio existente; ruta de carga no confirmada**.

## Archivo sugerido

`SIAJ_terminos_excel_legado_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `terminos_excel_legado` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| NumeroExpediente | NumeroExpediente | Texto | Sí | 50 | EXP-001/2026 | Columna posicional; conservar este orden. Primera hoja y primera fila de encabezados. |
| Materia | Materia | Texto | Recomendado | 100 | Laboral | Columna posicional; conservar este orden. Primera hoja y primera fila de encabezados. |
| OrganoJurisdiccional | OrganoJurisdiccional | Texto | No | Sin límite declarado | Tribunal de ejemplo | Columna posicional; conservar este orden. Primera hoja y primera fila de encabezados. |
| EmailAbogadoResponsable | EmailAbogadoResponsable | Texto | Recomendado | 100 | ana.perez@example.org | Columna posicional; conservar este orden. Primera hoja y primera fila de encabezados. |
| Actuacion | Actuacion | Texto | Sí (propuesta) | Sin límite declarado | Presentar contestación | Columna posicional; conservar este orden. Primera hoja y primera fila de encabezados. |
| FechaVencimiento | FechaVencimiento | Fecha | Sí | No aplica | 2026-09-15 | Columna posicional; conservar este orden. Primera hoja y primera fila de encabezados. |
| Prioridad | Prioridad | Texto | Recomendado | 50 | MEDIA | Columna posicional; conservar este orden. Primera hoja y primera fila de encabezados. |

## Dependencias

- Usuarios por email. Cargar previamente o resolver en el mismo proceso controlado.
- Gerencias. Cargar previamente o resolver en el mismo proceso controlado.
- Materias. Cargar previamente o resolver en el mismo proceso controlado.
- Prioridades. Cargar previamente o resolver en el mismo proceso controlado.
- Expedientes por número. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- XLSX auténtico; primera hoja, encabezado fila 1 y datos desde fila 2. Filas sin número se omiten.
- Fecha nativa Excel o texto AAAA-MM-DD/dd/MM/aaaa; preferir ISO. Número de expediente siempre texto.
- Si expediente no existe, crea expediente en TRAMITE con descripción automática; toma gerencia del abogado o la primera disponible y materia por nombre o la primera disponible. Esto puede asignar incorrectamente: exigir homologación previa.
- Email inexistente deja abogado nulo. Prioridad desconocida cae en MEDIA o null. Crea un nuevo término por fila sin deduplicación; no reimportar el mismo archivo.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- Confirmar la ruta y ejecutar primero una carga de ensayo con conciliación de resultados.
- Evidencia: `service/procesal/TerminoImportService.java`.

# Entidad: Audiencias: API histórica

## Descripción

Contrato recomendado dentro de las claves leídas por la API y el asistente. La hoja Excel requiere transformación a JSON, no se carga directamente.

Estado: **API existente; homologación e inyección posteriores**.

## Archivo sugerido

`SIAJ_audiencias_api_historica_v1.json`. Formato: JSON UTF-8; hoja Excel sólo para captura y conversión.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| numeroExpediente | numeroExpediente | Texto | Sí (propuesta) | 50 | EXP-001/2026 | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| gerencia | gerencia | Texto | Sí o global del lote | 200 | Gerencia Laboral y Penal | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| materia | materia | Texto | Sí (propuesta) | 100 | Laboral | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| descripcion | descripcion | Texto | Recomendado | Sin límite declarado | Asunto de ejemplo | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| partes | partes | Texto | Recomendado | Sin límite declarado | Actor: Persona A; Demandado: Entidad B | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| sede | sede | Texto | No | 150 | Ciudad de México | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| organoJurisdiccional | organoJurisdiccional | Texto | No | 300 | Tribunal de ejemplo | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| abogadoResponsable | abogadoResponsable | Texto | Recomendado | 200 | Ana Pérez López | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| tipoAudiencia | tipoAudiencia | Texto | Sí (propuesta) | 100 | Conciliación | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| fechaAudiencia | fechaAudiencia | Fecha | Sí (propuesta) | No aplica | 2026-09-15 | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente. Formato ISO AAAA-MM-DD obligatorio en la propuesta. |
| horaAudiencia | horaAudiencia | Hora | Sí (propuesta) | No aplica | 09:30 | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente. Formato HH:mm. |
| sala | sala | Texto | No | 490 | Sala 1 | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| abogadoCompareciente | abogadoCompareciente | Texto | No | 200 | Ana Pérez López | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| esEnLinea | esEnLinea | Booleano | No | No aplica | false | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| enlaceReunion | enlaceReunion | Texto | Condicional: virtual | 490 | https://example.org/reunion/123 | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| actaDocumento | actaDocumento | Texto | No | 490 | acta-001.pdf | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| observaciones | observaciones | Texto | No | Sin límite declarado | Observación de ejemplo | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |

## Dependencias

- Catálogos homologados en lote. Cargar previamente o resolver en el mismo proceso controlado.
- Gerencia global si falta en filas. Cargar previamente o resolver en el mismo proceso controlado.
- Usuarios. Cargar previamente o resolver en el mismo proceso controlado.
- Expedientes derivados. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- POST /api/migration/lote/audiencias admite lista de objetos; X-API-KEY configurada. Recepción no equivale a persistencia final.
- El asistente homologa catálogos, revisa expedientes, audiencias y términos antes de inyectar.
- Fechas vacías o inválidas se sustituyen por HOY y hora inválida por 00:00; prevalidarlas y rechazar errores antes de enviar.
- La inyección asigna CONCLUIDO/CONCLUIDA si la fecha es anterior a HOY; en otro caso PROYECTISTA/PENDIENTE. No conserva un estado histórico enviado arbitrariamente.
- Deduplica términos por expediente+asunto+vencimiento y audiencias por expediente+fecha+hora; esto no prueba identidad de negocio y puede omitir eventos distintos.
- Campos de archivos son referencias textuales; no transfieren binarios. Longitudes 490 reflejan truncamiento del importador en campos señalados.
- Para términos usar asunto, sin enviar simultáneamente prestacion/actuacion; la vista e inyección no resuelven esos alias de la misma manera. archivoWord se asigna a acuseNombre en el importador, no al documento de borrador.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- Confirmar la ruta y ejecutar primero una carga de ensayo con conciliación de resultados.
- Evidencia: `service/procesal/LoteMigracionService.java`.

# Entidad: Terminos: API histórica

## Descripción

Contrato recomendado dentro de las claves leídas por la API y el asistente. La hoja Excel requiere transformación a JSON, no se carga directamente.

Estado: **API existente; homologación e inyección posteriores**.

## Archivo sugerido

`SIAJ_terminos_api_historica_v1.json`. Formato: JSON UTF-8; hoja Excel sólo para captura y conversión.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| numeroExpediente | numeroExpediente | Texto | Sí (propuesta) | 50 | EXP-001/2026 | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| gerencia | gerencia | Texto | Sí o global del lote | 200 | Gerencia Laboral y Penal | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| materia | materia | Texto | Sí (propuesta) | 100 | Laboral | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| descripcion | descripcion | Texto | Recomendado | Sin límite declarado | Asunto de ejemplo | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| partes | partes | Texto | Recomendado | Sin límite declarado | Actor: Persona A; Demandado: Entidad B | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| sede | sede | Texto | No | 150 | Ciudad de México | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| organoJurisdiccional | organoJurisdiccional | Texto | No | 300 | Tribunal de ejemplo | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| abogadoResponsable | abogadoResponsable | Texto | Recomendado | 200 | Ana Pérez López | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| asunto | asunto | Texto | Sí (propuesta) | 490 | Presentar contestación | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| fechaVencimiento | fechaVencimiento | Fecha | Sí (propuesta) | No aplica | 2026-09-15 | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente. Formato ISO AAAA-MM-DD obligatorio en la propuesta. |
| fechaIngreso | fechaIngreso | Fecha | Recomendado | No aplica | 2026-09-07 | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente. Formato ISO AAAA-MM-DD obligatorio en la propuesta. |
| prioridad | prioridad | Texto | Recomendado | 50 | MEDIA | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| acuse | acuse | Texto | No | 490 | acuse-001.pdf | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| acuseNombre | acuseNombre | Texto | No | 490 | Acuse de recibo | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |
| observaciones | observaciones | Texto | No | Sin límite declarado | Observación de ejemplo | Clave JSON exacta. Nombres de catálogos/usuarios homologados en asistente.  |

## Dependencias

- Catálogos homologados en lote. Cargar previamente o resolver en el mismo proceso controlado.
- Gerencia global si falta en filas. Cargar previamente o resolver en el mismo proceso controlado.
- Usuarios. Cargar previamente o resolver en el mismo proceso controlado.
- Expedientes derivados. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- POST /api/migration/lote/terminos admite lista de objetos; X-API-KEY configurada. Recepción no equivale a persistencia final.
- El asistente homologa catálogos, revisa expedientes, audiencias y términos antes de inyectar.
- Fechas vacías o inválidas se sustituyen por HOY y hora inválida por 00:00; prevalidarlas y rechazar errores antes de enviar.
- La inyección asigna CONCLUIDO/CONCLUIDA si la fecha es anterior a HOY; en otro caso PROYECTISTA/PENDIENTE. No conserva un estado histórico enviado arbitrariamente.
- Deduplica términos por expediente+asunto+vencimiento y audiencias por expediente+fecha+hora; esto no prueba identidad de negocio y puede omitir eventos distintos.
- Campos de archivos son referencias textuales; no transfieren binarios. Longitudes 490 reflejan truncamiento del importador en campos señalados.
- Para términos usar asunto, sin enviar simultáneamente prestacion/actuacion; la vista e inyección no resuelven esos alias de la misma manera. archivoWord se asigna a acuseNombre en el importador, no al documento de borrador.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- Confirmar la ruta y ejecutar primero una carga de ensayo con conciliación de resultados.
- Evidencia: `service/procesal/LoteMigracionService.java`.

# Entidad: Actores

## Descripción

Levantamiento preliminar, sin tabla independiente encontrada. Destino posible: expedientes.partes.

Estado: **Levantamiento; no importable como entidad independiente**.

## Archivo sugerido

`SIAJ_actores_levantamiento_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `actores` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| expediente_id | Expediente id | UUID | Sí (propuesta) | No aplica | 00000000-0000-4000-8000-000000000001 | Validar definición funcional antes de importar. |
| nombre_actor | Nombre actor | Texto | Sí (propuesta) | Por definir | Texto de ejemplo | Validar definición funcional antes de importar. |

## Dependencias

- Expedientes u órganos jurisdiccionales según significado. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- No importar esta hoja directamente. Validar modelo y correspondencia funcional.
- Para actores y demandados, el texto partes permite múltiples nombres pero pierde identificación estructurada; no inventar RFC, CURP, domicilios, personalidad o llaves únicas.
- Para salas/juzgados, usar el layout de órganos si representan autoridad; sala_lugar si se trata del recinto. JUZGADO es ejemplo propuesto, no valor de catálogo confirmado.

Llaves y restricciones observadas:

- `No existe PK ni unicidad específica confirmada para esta entidad`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Evidencia: `Inventario SQL y entidades JPA`.

# Entidad: Demandados

## Descripción

Levantamiento preliminar, sin tabla independiente encontrada. Destino posible: expedientes.partes.

Estado: **Levantamiento; no importable como entidad independiente**.

## Archivo sugerido

`SIAJ_demandados_levantamiento_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `demandados` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| expediente_id | Expediente id | UUID | Sí (propuesta) | No aplica | 00000000-0000-4000-8000-000000000001 | Validar definición funcional antes de importar. |
| nombre_demandado | Nombre demandado | Texto | Sí (propuesta) | Por definir | Texto de ejemplo | Validar definición funcional antes de importar. |

## Dependencias

- Expedientes u órganos jurisdiccionales según significado. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- No importar esta hoja directamente. Validar modelo y correspondencia funcional.
- Para actores y demandados, el texto partes permite múltiples nombres pero pierde identificación estructurada; no inventar RFC, CURP, domicilios, personalidad o llaves únicas.
- Para salas/juzgados, usar el layout de órganos si representan autoridad; sala_lugar si se trata del recinto. JUZGADO es ejemplo propuesto, no valor de catálogo confirmado.

Llaves y restricciones observadas:

- `No existe PK ni unicidad específica confirmada para esta entidad`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Evidencia: `Inventario SQL y entidades JPA`.

# Entidad: Organismos

## Descripción

Levantamiento preliminar, sin tabla independiente encontrada. Destino posible: Sin destino inequívoco.

Estado: **Levantamiento; no importable como entidad independiente**.

## Archivo sugerido

`SIAJ_organismos_levantamiento_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `organismos` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| nombre_organismo | Nombre organismo | Texto | Sí (propuesta) | Por definir | Texto de ejemplo | Validar definición funcional antes de importar. |
| funcion_en_asunto | Funcion en asunto | Texto | Sí (propuesta) | Por definir | Texto de ejemplo | Validar definición funcional antes de importar. |

## Dependencias

- Expedientes u órganos jurisdiccionales según significado. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- Confirmar si se trata de parte demandada, autoridad jurisdiccional o unidad administrativa. Definir atributos, claves, relaciones y catálogo antes de implementar.

Llaves y restricciones observadas:

- `No existe PK ni unicidad específica confirmada para esta entidad`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Evidencia: `Inventario SQL y entidades JPA`.

# Entidad: Salas

## Descripción

Levantamiento preliminar, sin tabla independiente encontrada. Destino posible: audiencias.sala_lugar o organos_jurisdiccionales, según significado.

Estado: **Levantamiento; no importable como entidad independiente**.

## Archivo sugerido

`SIAJ_salas_levantamiento_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `salas` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| expediente_id | Expediente id | UUID | Sí (propuesta) | No aplica | 00000000-0000-4000-8000-000000000001 | Validar definición funcional antes de importar. |
| sala | Sala | Texto | Sí (propuesta) | Sin límite SQL; 490 en migración | Texto de ejemplo | Validar definición funcional antes de importar. |

## Dependencias

- Expedientes u órganos jurisdiccionales según significado. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- No importar esta hoja directamente. Validar modelo y correspondencia funcional.
- Para actores y demandados, el texto partes permite múltiples nombres pero pierde identificación estructurada; no inventar RFC, CURP, domicilios, personalidad o llaves únicas.
- Para salas/juzgados, usar el layout de órganos si representan autoridad; sala_lugar si se trata del recinto. JUZGADO es ejemplo propuesto, no valor de catálogo confirmado.

Llaves y restricciones observadas:

- `No existe PK ni unicidad específica confirmada para esta entidad`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Evidencia: `Inventario SQL y entidades JPA`.

# Entidad: Juzgados

## Descripción

Levantamiento preliminar, sin tabla independiente encontrada. Destino posible: organos_jurisdiccionales.

Estado: **Levantamiento; no importable como entidad independiente**.

## Archivo sugerido

`SIAJ_juzgados_levantamiento_v1.xlsx`. Formato: Excel XLSX; CSV sólo con adaptador. También disponible como hoja `juzgados` del libro consolidado.

## Estructura del layout

| Campo | Descripción | Tipo de dato | Obligatorio | Longitud máxima | Ejemplo | Validaciones |
| --- | --- | --- | --- | --- | --- | --- |
| nombre | Nombre | Texto | Sí (propuesta) | 300 | Valor del catálogo | Validar definición funcional antes de importar. |
| gerencia_id | Gerencia id | Entero | No | No aplica | 1 | Validar definición funcional antes de importar. |
| sede | Sede | Texto | No | 100 | Ciudad de México | Validar definición funcional antes de importar. |
| tipo | Tipo | Texto | No | 50 | JUZGADO | Validar definición funcional antes de importar. |

## Dependencias

- Expedientes u órganos jurisdiccionales según significado. Cargar previamente o resolver en el mismo proceso controlado.

## Reglas de negocio

- No importar esta hoja directamente. Validar modelo y correspondencia funcional.
- Para actores y demandados, el texto partes permite múltiples nombres pero pierde identificación estructurada; no inventar RFC, CURP, domicilios, personalidad o llaves únicas.
- Para salas/juzgados, usar el layout de órganos si representan autoridad; sala_lugar si se trata del recinto. JUZGADO es ejemplo propuesto, no valor de catálogo confirmado.

Llaves y restricciones observadas:

- `No existe PK ni unicidad específica confirmada para esta entidad`.

## Observaciones para el usuario

- Consultar hoja Catalogos y reglas comunes. Valores de ejemplo no son registros existentes.
- No se entrega un importador ejecutable; este contrato requiere validación y adaptación técnica.
- Evidencia: `Inventario SQL y entidades JPA`.

# Catálogos y valores permitidos

Los valores enumerados son códigos exactos. Para catálogos editables la hoja describe qué exportar; no contiene IDs ficticios presentados como válidos.

| Entidad | Campo | Valores o fuente | Carga previa / observación |
| --- | --- | --- | --- |
| materias | gerencia_id | Exportar id, nombre, activo y ámbito de gerencias | Cargar previamente; IDs reales pendientes del ambiente destino |
| materias | usuario_id | Exportar id, nombre, activo y ámbito de usuarios | Cargar previamente; IDs reales pendientes del ambiente destino |
| cat_estados | gerencia_id | Exportar id, nombre, activo y ámbito de gerencias | Cargar previamente; IDs reales pendientes del ambiente destino |
| cat_estados | usuario_id | Exportar id, nombre, activo y ámbito de usuarios | Cargar previamente; IDs reales pendientes del ambiente destino |
| cat_tipo_expediente | gerencia_id | Exportar id, nombre, activo y ámbito de gerencias | Cargar previamente; IDs reales pendientes del ambiente destino |
| cat_tipo_expediente | usuario_id | Exportar id, nombre, activo y ámbito de usuarios | Cargar previamente; IDs reales pendientes del ambiente destino |
| organos_jurisdiccionales | gerencia_id | Exportar id, nombre, activo y ámbito de gerencias | Cargar previamente; IDs reales pendientes del ambiente destino |
| organos_jurisdiccionales | usuario_id | Exportar id, nombre, activo y ámbito de usuarios | Cargar previamente; IDs reales pendientes del ambiente destino |
| tipos_audiencia | gerencia_id | Exportar id, nombre, activo y ámbito de gerencias | Cargar previamente; IDs reales pendientes del ambiente destino |
| tipos_audiencia | usuario_id | Exportar id, nombre, activo y ámbito de usuarios | Cargar previamente; IDs reales pendientes del ambiente destino |
| cat_tipos_termino | tipo_dias | HABILES, NATURALES | Valores cerrados según CHECK/modelo/servicio indicado; ver regla de entidad |
| cat_etapas_procesales | etapa_legacy | TRAMITE, LAUDO, FIRME, CONCLUIDO | Valores cerrados según CHECK/modelo/servicio indicado; ver regla de entidad |
| usuarios | rol | ADMIN, DIRECTOR_PRINCIPAL, DIRECTOR, SUBDIRECTOR, GERENTE, JEFE_DEPTO, ABOGADO | Valores cerrados según CHECK/modelo/servicio indicado; ver regla de entidad |
| usuario_gerencias | usuario_id | Exportar id, nombre, activo y ámbito de usuarios | Cargar previamente; IDs reales pendientes del ambiente destino |
| usuario_gerencias | gerencia_id | Exportar id, nombre, activo y ámbito de gerencias | Cargar previamente; IDs reales pendientes del ambiente destino |
| usuario_materias | usuario_id | Exportar id, nombre, activo y ámbito de usuarios | Cargar previamente; IDs reales pendientes del ambiente destino |
| usuario_materias | materia_id | Exportar id, nombre, activo y ámbito de materias | Cargar previamente; IDs reales pendientes del ambiente destino |
| usuario_permisos_adicionales | usuario_id | Exportar id, nombre, activo y ámbito de usuarios | Cargar previamente; IDs reales pendientes del ambiente destino |
| usuario_permisos_adicionales | permiso | DASHBOARD_VER, AGENDA_VER, CATALOGOS_VER, USUARIOS_VER, ADMIN_PANEL, EXPEDIENTES_VER, EXPEDIENTES_SUPERVISION, TERMINOS_VER, TERMINOS_SUPERVISION, AUDIENCIAS_VER, AUDIENCIAS_SUPERVISION, DOCUMENTOS_SUPERVISION, ALERTAS_CONFIGURAR | Valores cerrados según CHECK/modelo/servicio indicado; ver regla de entidad |
| expedientes | etapa_procesal | TRAMITE, LAUDO, FIRME, CONCLUIDO | Valores cerrados según CHECK/modelo/servicio indicado; ver regla de entidad |
| expedientes | abogado_responsable_id | Exportar id, nombre, activo y ámbito de usuarios | Cargar previamente; IDs reales pendientes del ambiente destino |
| expedientes | gerencia_id | Exportar id, nombre, activo y ámbito de gerencias | Cargar previamente; IDs reales pendientes del ambiente destino |
| expedientes | materia_id | Exportar id, nombre, activo y ámbito de materias | Cargar previamente; IDs reales pendientes del ambiente destino |
| expedientes | organo_jurisdiccional_id | Exportar id, nombre, activo y ámbito de organos_jurisdiccionales | Cargar previamente; IDs reales pendientes del ambiente destino |
| expedientes | prioridad_id | Exportar id, nombre, activo y ámbito de cat_prioridad | Cargar previamente; IDs reales pendientes del ambiente destino |
| expedientes | tipo_expediente_id | Exportar id, nombre, activo y ámbito de cat_tipo_expediente | Cargar previamente; IDs reales pendientes del ambiente destino |
| audiencia_contextos | origen | EXPEDIENTE, ASUNTO_UNICO | Valores cerrados según CHECK/modelo/servicio indicado; ver regla de entidad |
| audiencia_contextos | gerencia_id | Exportar id, nombre, activo y ámbito de gerencias | Cargar previamente; IDs reales pendientes del ambiente destino |
| audiencia_contextos | materia_id | Exportar id, nombre, activo y ámbito de materias | Cargar previamente; IDs reales pendientes del ambiente destino |
| audiencia_contextos | organo_jurisdiccional_id | Exportar id, nombre, activo y ámbito de organos_jurisdiccionales | Cargar previamente; IDs reales pendientes del ambiente destino |
| termino_contextos | origen | EXPEDIENTE, ASUNTO_UNICO | Valores cerrados según CHECK/modelo/servicio indicado; ver regla de entidad |
| termino_contextos | gerencia_id | Exportar id, nombre, activo y ámbito de gerencias | Cargar previamente; IDs reales pendientes del ambiente destino |
| termino_contextos | materia_id | Exportar id, nombre, activo y ámbito de materias | Cargar previamente; IDs reales pendientes del ambiente destino |
| termino_contextos | organo_jurisdiccional_id | Exportar id, nombre, activo y ámbito de organos_jurisdiccionales | Cargar previamente; IDs reales pendientes del ambiente destino |
| audiencias | estatus_audiencia | PENDIENTE, CON_ACTA, CONCLUIDA | Valores cerrados según CHECK/modelo/servicio indicado; ver regla de entidad |
| audiencias | abogado_comparece_id | Exportar id, nombre, activo y ámbito de usuarios | Cargar previamente; IDs reales pendientes del ambiente destino |
| audiencias | expediente_id | Exportar id, nombre, activo y ámbito de expedientes | Cargar previamente; IDs reales pendientes del ambiente destino |
| audiencias | tipo_audiencia_id | Exportar id, nombre, activo y ámbito de tipos_audiencia | Cargar previamente; IDs reales pendientes del ambiente destino |
| audiencias | contexto_id | Exportar id, nombre, activo y ámbito de audiencia_contextos | Cargar previamente; IDs reales pendientes del ambiente destino |
| audiencias | usuario_delegado_id | Exportar id, nombre, activo y ámbito de usuarios | Cargar previamente; IDs reales pendientes del ambiente destino |
| terminos | estatus_termino | PROYECTISTA, REVISION, GERENCIA, DIRECCION, LIBERADO, PRESENTADO, CONCLUIDO | Valores cerrados según CHECK/modelo/servicio indicado; ver regla de entidad |
| terminos | abogado_responsable_id | Exportar id, nombre, activo y ámbito de usuarios | Cargar previamente; IDs reales pendientes del ambiente destino |
| terminos | expediente_id | Exportar id, nombre, activo y ámbito de expedientes | Cargar previamente; IDs reales pendientes del ambiente destino |
| terminos | prioridad_id | Exportar id, nombre, activo y ámbito de cat_prioridad | Cargar previamente; IDs reales pendientes del ambiente destino |
| terminos | contexto_id | Exportar id, nombre, activo y ámbito de termino_contextos | Cargar previamente; IDs reales pendientes del ambiente destino |
| terminos | tipo_dias_calculado | HABILES, NATURALES | Valores cerrados según CHECK/modelo/servicio indicado; ver regla de entidad |
| colaboradores_expediente | expediente_id | Exportar id, nombre, activo y ámbito de expedientes | Cargar previamente; IDs reales pendientes del ambiente destino |
| colaboradores_expediente | usuario_id | Exportar id, nombre, activo y ámbito de usuarios | Cargar previamente; IDs reales pendientes del ambiente destino |
| carpetas | carpeta_padre_id | Exportar id, nombre, activo y ámbito de carpetas | Cargar previamente; IDs reales pendientes del ambiente destino |
| documentos | carpeta_id | Exportar id, nombre, activo y ámbito de carpetas | Cargar previamente; IDs reales pendientes del ambiente destino |
| termino_archivos | etapa | PROYECTISTA, REVISION, GERENCIA, DIRECCION, LIBERADO, PRESENTADO, CONCLUIDO | Valores cerrados según CHECK/modelo/servicio indicado; ver regla de entidad |
| termino_archivos | tipo_documento | BORRADOR, ACUSE | Valores cerrados según CHECK/modelo/servicio indicado; ver regla de entidad |
| termino_archivos | termino_id | Exportar id, nombre, activo y ámbito de terminos | Cargar previamente; IDs reales pendientes del ambiente destino |
| comentarios | etapa_procesal | TRAMITE, LAUDO, FIRME, CONCLUIDO | Valores cerrados según CHECK/modelo/servicio indicado; ver regla de entidad |
| comentarios | usuario_id | Exportar id, nombre, activo y ámbito de usuarios | Cargar previamente; IDs reales pendientes del ambiente destino |
| recordatorios | prioridad_id | Exportar id, nombre, activo y ámbito de cat_prioridad | Cargar previamente; IDs reales pendientes del ambiente destino |
| recordatorios | usuario_id | Exportar id, nombre, activo y ámbito de usuarios | Cargar previamente; IDs reales pendientes del ambiente destino |
| eventos_calendario | usuario_id | Exportar id, nombre, activo y ámbito de usuarios | Cargar previamente; IDs reales pendientes del ambiente destino |
| configuraciones_alerta | nivel | SISTEMA, GERENCIA, USUARIO, ASUNTO | Valores cerrados según CHECK/modelo/servicio indicado; ver regla de entidad |
| configuraciones_alerta | tipo_evento | TERMINO, AUDIENCIA | Valores cerrados según CHECK/modelo/servicio indicado; ver regla de entidad |
| configuraciones_alerta | gerencia_id | Exportar id, nombre, activo y ámbito de gerencias | Cargar previamente; IDs reales pendientes del ambiente destino |
| configuraciones_alerta | usuario_id | Exportar id, nombre, activo y ámbito de usuarios | Cargar previamente; IDs reales pendientes del ambiente destino |
| configuraciones_semaforo | nivel | SISTEMA, GERENCIA | Valores cerrados según CHECK/modelo/servicio indicado; ver regla de entidad |
| configuraciones_semaforo | tipo_evento | TERMINO, AUDIENCIA | Valores cerrados según CHECK/modelo/servicio indicado; ver regla de entidad |
| configuraciones_semaforo | gerencia_id | Exportar id, nombre, activo y ámbito de gerencias | Cargar previamente; IDs reales pendientes del ambiente destino |
| preferencias_notificacion | usuario_id | Exportar id, nombre, activo y ámbito de usuarios | Cargar previamente; IDs reales pendientes del ambiente destino |
| preferencias_notificacion_evento | evento | EXPEDIENTE_NUEVO, EXPEDIENTE_REASIGNADO, EXPEDIENTE_OBSERVACION_NUEVA, TERMINO_ASIGNADO, TERMINO_NUEVO, TERMINO_CAMBIO_ETAPA, TERMINO_VALIDADO_CONCLUIDO, TERMINO_RECORDATORIO, AUDIENCIA_NUEVA, AUDIENCIA_COMPARECENCIA_ASIGNADA, AUDIENCIA_DELEGADA, AUDIENCIA_RECORDATORIO, AUDIENCIA_ACTA_DISPONIBLE, AUDIENCIA_VALIDADA_CONCLUIDA, SEGURIDAD_PASSWORD_POR_VENCER, TAREA_RECORDATORIO | Valores cerrados según CHECK/modelo/servicio indicado; ver regla de entidad |
| preferencias_notificacion_evento | usuario_id | Exportar id, nombre, activo y ámbito de usuarios | Cargar previamente; IDs reales pendientes del ambiente destino |
| expediente_etapa_periodos | etapa_procesal | TRAMITE, LAUDO, FIRME, CONCLUIDO | Valores cerrados según CHECK/modelo/servicio indicado; ver regla de entidad |
| audiencias_desahogadas | audiencia_id | Exportar id, nombre, activo y ámbito de audiencias | Cargar previamente; IDs reales pendientes del ambiente destino |
| terminos_presentados | termino_id | Exportar id, nombre, activo y ámbito de terminos | Cargar previamente; IDs reales pendientes del ambiente destino |
| cat_prioridad | nombre | ALTA, MEDIA, BAJA | Semillas DataInitializer; no lista inmutable de BD |
| materias | nombre | Civil, Mercantil, Fiscal, Administrativo, Laboral, Penal, Amparo, Transparencia | Semillas DataInitializer; confirmar catálogo vigente |
| cat_tipos_termino | tipo_dias | HABILES, NATURALES | Convención JPA; falta confirmar calendario inhábil |
| recordatorios | prioridad DTO | normal, urgent | No equivalen automáticamente a IDs de cat_prioridad |
| organos_jurisdiccionales | tipo | Pendiente de catálogo vigente | Texto libre; JUZGADO/SALA no son enum confirmado |
| colaboradores_expediente | permiso_nivel | Pendiente de valores vigentes | Texto libre sin CHECK |
| eventos_calendario | categoria | Pendiente de valores vigentes | Texto libre sin CHECK |

# Matriz de entidades técnicas y excluidas

| Tabla / entidad | Archivo | Justificación |
| --- | --- | --- |
| actividad_expedientes | Sin archivo de carga de usuario | Auditoría/trazabilidad generada por operaciones; sólo conservación histórica técnica, sin edición por usuarios. |
| actividad_usuarios | Sin archivo de carga de usuario | Auditoría/trazabilidad generada por operaciones; sólo conservación histórica técnica, sin edición por usuarios. |
| auditoria_eventos | Sin archivo de carga de usuario | Auditoría/trazabilidad generada por operaciones; sólo conservación histórica técnica, sin edición por usuarios. |
| auditoria_sesiones | Sin archivo de carga de usuario | Auditoría/trazabilidad generada por operaciones; sólo conservación histórica técnica, sin edición por usuarios. |
| bitacora_datos | Sin archivo de carga de usuario | Auditoría/trazabilidad generada por operaciones; sólo conservación histórica técnica, sin edición por usuarios. |
| bk_auth_gerencias_20260727_131223_usuario_gerencias | Sin archivo de carga de usuario | Respaldo histórico: no restaurar como datos maestros ni incluir en carga ordinaria. |
| bk_auth_gerencias_20260727_131223_usuario_materias | Sin archivo de carga de usuario | Respaldo histórico: no restaurar como datos maestros ni incluir en carga ordinaria. |
| bk_auth_gerencias_20260727_131223_usuarios | Sin archivo de carga de usuario | Respaldo histórico: no restaurar como datos maestros ni incluir en carga ordinaria. |
| catalogos_migracion | Sin archivo de carga de usuario | Staging y homologación generados por asistente/API; no insertar mediante plantilla de usuario. |
| documentos_expediente | Sin archivo de carga de usuario | Tabla presente en dump sin entidad Documento correspondiente (actual usa documentos). Validar si es legado antes de migrar; no duplicar archivos. |
| lotes_migracion | Sin archivo de carga de usuario | Staging y homologación generados por asistente/API; no insertar mediante plantilla de usuario. |
| notificaciones | Sin archivo de carga de usuario | Uso técnico interno; no carga de usuario final. |
| password_reset_tokens | Sin archivo de carga de usuario | Tokens/credenciales/suscripciones de seguridad; se generan o registran en sesión, no se migran por Excel. |
| push_subscriptions | Sin archivo de carga de usuario | Tokens/credenciales/suscripciones de seguridad; se generan o registran en sesión, no se migran por Excel. |
| registros_migracion | Sin archivo de carga de usuario | Staging y homologación generados por asistente/API; no insertar mediante plantilla de usuario. |
| manual_recovery_codes (JPA/script) | Sin archivo de carga de usuario | Códigos de recuperación de seguridad; no distribuir ni importar en plantilla. |

# Información faltante y validaciones pendientes

| ID | Elemento | Información faltante o acción | Responsable |
| --- | --- | --- | --- |
| G01 | Esquema destino | SQL/JPA difieren: email, multi-gerencia, tipos de término, etapas y períodos, columnas dinámicas de término. DBA debe entregar DDL vigente y versión desplegada. | DBA / técnico |
| G02 | Plantillas descargables | AdminImportController devuelve bytes de encabezados separados por comas con extensión .xlsx; generar OOXML real y probar lector. | Técnico |
| G03 | Rutas Excel | ImportService de audiencias sólo itera filas y no guarda. TerminoImportService tiene lógica, pero no se encontró controlador que lo invoque. Confirmar o implementar ruta. | Técnico |
| G04 | Calidad de migración | parseDate usa HOY y parseTime 00:00 ante error; rechazar entrada inválida y registrar error por fila. | Técnico / funcional |
| G05 | Estados históricos | No concluir por antigüedad de fecha: acordar conservación de estado, desahogo, presentación y evidencia documental; modificar el importador. | Funcional / técnico |
| G06 | Conciliación | La inyección captura excepciones por registro y después marca PROCESADO; el estado del lote no garantiza persistencia. Conciliar cantidades y claves contra destino. | Técnico / datos |
| G07 | Duplicados | Número de expediente no es único en BD. Definir clave de negocio y política de coincidencia por materia/gerencia/órgano/partes; no fusionar homónimos. | Funcional / datos |
| G08 | Entidades no normalizadas | Actores, demandados y organismos carecen de estructura inequívoca; definir identificación, cardinalidad, atributos y reglas. Salas y juzgados requieren precisar autoridad vs recinto. | Funcional |
| G09 | Catálogos vigentes | No se dispone de datos del ambiente destino. Exportar id, nombre, activo, gerencia/materia y usuario propietario; resolver valores obsoletos y duplicados. | Administrador / datos |
| G10 | Asuntos únicos | Audiencia exige expediente en dump/controlador. Validar cómo se crean expedientes temporales y contextos; no importar audiencias huérfanas. | Técnico / funcional |
| G11 | Plazos y calendario | Definir calendario de inhábiles, tipo de días, fecha base y zona horaria. No existe evidencia suficiente para garantizar cálculo de vencimientos jurídicos. | Funcional |
| G12 | Almacenamiento | Validar raíces, límites HTTP/proxy, extensiones y flujo de acta/acuse. Referencias JSON no suben binarios. | Técnico |
| G13 | Semántica histórica | Definir autores/fechas originales, actualizaciones vs altas, reintentos, lotes atómicos y reversión. Propuesta de clave_origen requiere implementación. | Funcional / técnico |
| G14 | Campos incompatibles | DTO asuntoId Integer no equivale al expediente UUID; prioridad/archivo_word del dump y archivoWord del JSON no equivalen al modelo de archivos actual. | Técnico |
| G15 | Configuración | Validar valores de permiso_nivel, categoría de evento, entidad_tipo, equivalencia normal/urgent y precedencia de preferencias general/por evento. | Funcional / técnico |

# Protocolo propuesto de aceptación de carga

1. Congelar versión de plantilla y exportación de catálogos del destino.
2. Validar encabezados, tipos, longitudes, obligatorios, fechas reales y referencias antes de persistir. No truncar ni sustituir datos sin error explícito.
3. Reportar por fila: archivo, hoja, número de fila, clave de origen, campo, valor rechazado, código, mensaje y acción. Este reporte es un requisito propuesto, no una función confirmada.
4. Ensayar altas, repetición del lote, homónimos, referencia ausente, catálogo inactivo, fecha inválida, asunto único, usuario sin correo y documento ausente.
5. Conciliar recibidos = aceptados + rechazados + omitidos justificados. Verificar IDs, relaciones, estados y adjuntos realmente persistidos, no sólo el indicador PROCESADO.
6. Emitir acta de carga con lote, versión, responsable, totales y pendientes; conservar equivalencias y fuente original. No se ha ejecutado este protocolo contra una instancia desde este análisis.

# Fuentes locales y trazabilidad

Los máximos y NOT NULL provienen del dump, salvo campos expresamente marcados JPA o propuesta. Las reglas de aplicación se sustentan en los archivos siguientes; no constituyen asesoría sobre el cómputo jurídico de plazos.

- [db_schema_actual.sql](C:/Users/Rodrigo.DESKTOP-2MLHEH3/Desktop/Proyectos_Desarrollo/juridico-springboot/db_schema_actual.sql)
- [src/main/java/com/juridico/sistema_juridico/controller/AdminImportController.java](C:/Users/Rodrigo.DESKTOP-2MLHEH3/Desktop/Proyectos_Desarrollo/juridico-springboot/src/main/java/com/juridico/sistema_juridico/controller/AdminImportController.java)
- [src/main/java/com/juridico/sistema_juridico/controller/api/MigrationApiController.java](C:/Users/Rodrigo.DESKTOP-2MLHEH3/Desktop/Proyectos_Desarrollo/juridico-springboot/src/main/java/com/juridico/sistema_juridico/controller/api/MigrationApiController.java)
- [src/main/java/com/juridico/sistema_juridico/service/procesal/LoteMigracionService.java](C:/Users/Rodrigo.DESKTOP-2MLHEH3/Desktop/Proyectos_Desarrollo/juridico-springboot/src/main/java/com/juridico/sistema_juridico/service/procesal/LoteMigracionService.java)
- [src/main/java/com/juridico/sistema_juridico/service/procesal/TerminoImportService.java](C:/Users/Rodrigo.DESKTOP-2MLHEH3/Desktop/Proyectos_Desarrollo/juridico-springboot/src/main/java/com/juridico/sistema_juridico/service/procesal/TerminoImportService.java)
- [src/main/java/com/juridico/sistema_juridico/service/ImportService.java](C:/Users/Rodrigo.DESKTOP-2MLHEH3/Desktop/Proyectos_Desarrollo/juridico-springboot/src/main/java/com/juridico/sistema_juridico/service/ImportService.java)
- [src/main/java/com/juridico/sistema_juridico/controller/AudienciasController.java](C:/Users/Rodrigo.DESKTOP-2MLHEH3/Desktop/Proyectos_Desarrollo/juridico-springboot/src/main/java/com/juridico/sistema_juridico/controller/AudienciasController.java)
- [src/main/java/com/juridico/sistema_juridico/controller/TerminosController.java](C:/Users/Rodrigo.DESKTOP-2MLHEH3/Desktop/Proyectos_Desarrollo/juridico-springboot/src/main/java/com/juridico/sistema_juridico/controller/TerminosController.java)
- [src/main/java/com/juridico/sistema_juridico/controller/CatalogosController.java](C:/Users/Rodrigo.DESKTOP-2MLHEH3/Desktop/Proyectos_Desarrollo/juridico-springboot/src/main/java/com/juridico/sistema_juridico/controller/CatalogosController.java)
- [src/main/java/com/juridico/sistema_juridico/service/UsuarioService.java](C:/Users/Rodrigo.DESKTOP-2MLHEH3/Desktop/Proyectos_Desarrollo/juridico-springboot/src/main/java/com/juridico/sistema_juridico/service/UsuarioService.java)
- [src/main/java/com/juridico/sistema_juridico/service/configuracion/ConfiguracionAlertaService.java](C:/Users/Rodrigo.DESKTOP-2MLHEH3/Desktop/Proyectos_Desarrollo/juridico-springboot/src/main/java/com/juridico/sistema_juridico/service/configuracion/ConfiguracionAlertaService.java)
- [src/main/java/com/juridico/sistema_juridico/service/configuracion/ConfiguracionSemaforoService.java](C:/Users/Rodrigo.DESKTOP-2MLHEH3/Desktop/Proyectos_Desarrollo/juridico-springboot/src/main/java/com/juridico/sistema_juridico/service/configuracion/ConfiguracionSemaforoService.java)
- [src/main/java/com/juridico/sistema_juridico/service/FileStorageService.java](C:/Users/Rodrigo.DESKTOP-2MLHEH3/Desktop/Proyectos_Desarrollo/juridico-springboot/src/main/java/com/juridico/sistema_juridico/service/FileStorageService.java)
- [src/main/java/com/juridico/sistema_juridico/config/DataInitializer.java](C:/Users/Rodrigo.DESKTOP-2MLHEH3/Desktop/Proyectos_Desarrollo/juridico-springboot/src/main/java/com/juridico/sistema_juridico/config/DataInitializer.java)
- [deploy/database/03-allow-local-only-users.sql](C:/Users/Rodrigo.DESKTOP-2MLHEH3/Desktop/Proyectos_Desarrollo/juridico-springboot/deploy/database/03-allow-local-only-users.sql)
