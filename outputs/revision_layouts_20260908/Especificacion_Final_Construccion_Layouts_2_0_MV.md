# Especificación final de construcción — Layouts V2 Mínimos Viables para Migración

Fecha: 9 de septiembre de 2026  
Contrato: `2.0-MV`  
Estado: especificación de construcción

## 1. Alcance vinculante

Esta especificación implementa exactamente los cinco archivos aprobados por el contrato `2.0-MV`:

1. `Layout_Catalogos_V2_MV.xlsx`
2. `Layout_Usuarios_V2_MV.xlsx`
3. `Layout_Expedientes_V2_MV.xlsx`
4. `Layout_Audiencias_V2_MV.xlsx`
5. `Layout_Terminos_V2_MV.xlsx`

El importador debe leer las hojas por nombre, validar el libro completo antes de persistir registros y devolver un resultado por cada fila de datos. Las columnas, nombres de hojas y valores controlados definidos aquí son sensibles a mayúsculas, minúsculas y acentos tal como se muestran.

Los ejemplos con códigos como `GER_001`, `MAT_001` o `ORG_001` forman un conjunto ilustrativo autoconsistente. No son valores adicionales obligatorios: en producción deben sustituirse por los códigos aprobados y publicados en la versión de catálogo indicada en `CONTROL`.

## 2. Convenciones de construcción

### 2.1 Estructura física común

- Formato: OOXML `.xlsx`.
- Fila 1 de cada hoja importable: encabezados exactos, en el orden especificado.
- Filas 2 en adelante: datos.
- No se permiten encabezados duplicados, adicionales, renombrados ni omitidos.
- `CONTROL` contiene una sola fila de valores: encabezados en la fila 1 y datos en la fila 2.
- Las hojas de captura se implementan como tablas ampliables de Excel.
- `INSTRUCCIONES`, `DICCIONARIO` y `CATALOGOS_REFERENCIA` son informativas y no se importan.
- `CATALOGOS_REFERENCIA` está protegida contra edición y contiene los valores autorizados correspondientes a `catalogo_version`.
- Las listas controladas se configuran mediante validación de datos de Excel tomando sus valores de `CATALOGOS_REFERENCIA`.
- Las celdas de códigos, claves, teléfonos, número de empleado, nombres de archivo y rutas se formatean como texto.
- Las fechas son valores reales de Excel y se muestran como `aaaa-mm-dd`.
- Las horas son valores reales de Excel y se muestran como `hh:mm`.
- Los booleanos sólo aceptan `true` o `false`.
- Una fila completamente vacía después de la última fila con datos no se procesa.
- Una fila parcialmente capturada se valida como registro y puede resultar rechazada.

### 2.2 Normalización permitida

El importador puede eliminar espacios al inicio y al final de cada valor. No puede corregir códigos, inferir coincidencias por nombre, sustituir datos inválidos, truncar textos, asignar la fecha de ejecución, asignar `00:00`, elegir la primera materia o prioridad disponible ni inventar responsables o estados.

### 2.3 Formato uniforme de mensajes

Los errores de fila deben devolverse con el formato:

`[<archivo>][<hoja>][fila <n>][<columna>] <mensaje>`

Los errores de libro o de hoja deben devolverse con el formato:

`[<archivo>][<hoja>] <mensaje>`

Mensajes comunes exactos:

| Condición | Mensaje |
|---|---|
| Hoja ausente | `Falta la hoja obligatoria '<hoja>'.` |
| Hoja repetida | `La hoja '<hoja>' está duplicada.` |
| Encabezado ausente | `Falta la columna obligatoria '<columna>'.` |
| Encabezado adicional | `La columna '<columna>' no pertenece al contrato 2.0-MV.` |
| Orden incorrecto | `La columna '<columna>' no se encuentra en la posición definida por el contrato 2.0-MV.` |
| Valor obligatorio vacío | `El valor es obligatorio.` |
| Longitud excedida | `La longitud máxima permitida es <n> caracteres.` |
| Tipo inválido | `El valor debe ser de tipo <tipo>.` |
| Valor fuera de lista | `El valor '<valor>' no pertenece a los valores permitidos.` |
| Código inexistente | `El código '<valor>' no existe en la versión de catálogo '<catalogo_version>'.` |
| Código inactivo | `El código '<valor>' está inactivo.` |
| Duplicado en libro | `El valor '<valor>' está duplicado en la hoja.` |
| Unicidad en destino | `El valor '<valor>' ya existe y viola la regla de unicidad del destino.` |
| Relación incompatible | `El valor '<valor>' no es compatible con <referencia>.` |
| Archivo inexistente | `El archivo '<ruta>' no existe en la entrega del lote.` |
| URL inválida | `El valor debe ser una URL válida.` |

## 3. Hoja CONTROL común a los cinco archivos

### 3.1 Columnas exactas

| Pos. | Columna | Tipo | Longitud máxima | Obligatoria | Validación | Valores permitidos | Ejemplo válido | Mensajes específicos |
|---:|---|---|---:|---|---|---|---|---|
| 1 | `layout_version` | Texto | 10 | Sí | Coincidencia exacta con la versión del contrato. | `2.0-MV` | `2.0-MV` | `El valor debe ser exactamente '2.0-MV'.` |
| 2 | `fuente_codigo` | Texto | 50 | Sí | Fuente registrada y estable entre lotes. | Código registrado por implementación. | `FUENTE_LEGADO_01` | `La fuente '<valor>' no está registrada.` |
| 3 | `lote_clave` | Texto | 100 | Sí | Única para el lote; no identifica registros. | Clave definida para la entrega. | `CORTE_2026_09_09_01` | `La clave de lote '<valor>' ya fue utilizada con contenido diferente.` |
| 4 | `catalogo_version` | Texto | 50 | Sí | Debe coincidir con la versión publicada y disponible. | Versión publicada. | `CAT-2026-09` | `La versión de catálogo '<valor>' no está publicada o no coincide con el libro.` |
| 5 | `fecha_corte` | Fecha | No aplica | Sí | Fecha real de Excel, mostrada `aaaa-mm-dd`. | Fecha calendario válida. | `2026-09-09` | `El valor debe ser una fecha real de Excel con formato de visualización aaaa-mm-dd.` |
| 6 | `responsable_entrega` | Texto | 200 | Sí | No vacío. | Texto. | `María López Hernández` | Mensajes comunes de obligatoriedad y longitud. |
| 7 | `email_responsable` | Texto | 100 | Sí | Formato de correo válido; se usa sólo como contacto. | Correo sintácticamente válido. | `maria.lopez@cliente.mx` | `El valor debe ser un correo electrónico válido.` |

### 3.2 Validaciones del libro

1. `CONTROL` debe tener exactamente una fila de datos.
2. Los cinco archivos que integran una entrega deben compartir `layout_version`, `fuente_codigo`, `lote_clave`, `catalogo_version` y `fecha_corte`.
3. `lote_clave` no participa en la identidad de las filas.
4. Para cualquier comparación temporal, “futura” significa una fecha posterior a `fecha_corte`. Esta regla evita que una prevalidación repetida cambie por la fecha de ejecución.
5. Si alguna validación de estructura o de `CONTROL` falla, no se persiste ninguna fila del libro.

## 4. `Layout_Catalogos_V2_MV.xlsx`

### 4.1 Hojas y orden exacto

1. `CONTROL`
2. `GERENCIAS`
3. `MATERIAS`
4. `SEDES`
5. `PRIORIDADES`
6. `TIPOS_EXPEDIENTE`
7. `ORGANOS`
8. `TIPOS_AUDIENCIA`
9. `TIPOS_TERMINO`
10. `INSTRUCCIONES`
11. `DICCIONARIO`
12. `CATALOGOS_REFERENCIA`

Las ocho hojas de catálogo son importables. El backend debe validarlas como una unidad y respetar sus dependencias internas.

### 4.2 Hoja `GERENCIAS`

| Pos. | Columna | Tipo | Máx. | Oblig. | Validación | Valores permitidos | Ejemplo válido | Mensaje específico |
|---:|---|---|---:|---|---|---|---|---|
| 1 | `gerencia_codigo` | Texto | 100 | Sí | Único y estable. | Código capturado en esta hoja. | `GER_001` | `El código de gerencia '<valor>' está duplicado o ya existe.` |
| 2 | `nombre` | Texto | 200 | Sí | No vacío; unicidad conforme al destino. | Texto. | `Gerencia Jurídica Laboral` | `El nombre de gerencia '<valor>' viola la regla de unicidad del destino.` |
| 3 | `descripcion` | Texto | 32,767 | No | No truncar. | Texto o vacío. | `Atención de asuntos laborales.` | Mensaje común de longitud. |
| 4 | `activo` | Booleano | No aplica | Sí | Valor booleano real o literal controlado. | `true`, `false` | `true` | `El valor debe ser true o false.` |

### 4.3 Hoja `MATERIAS`

| Pos. | Columna | Tipo | Máx. | Oblig. | Validación | Valores permitidos | Ejemplo válido | Mensaje específico |
|---:|---|---|---:|---|---|---|---|---|
| 1 | `materia_codigo` | Texto | 100 | Sí | Único en el ámbito aprobado. | Código capturado en esta hoja. | `MAT_001` | `El código de materia '<valor>' está duplicado en su ámbito.` |
| 2 | `gerencia_codigo` | Texto | 100 | Sí | Debe existir en `GERENCIAS`. | Código de `GERENCIAS`. | `GER_001` | `La gerencia '<valor>' no existe en GERENCIAS.` |
| 3 | `nombre` | Texto | 100 | Sí | No vacío; unicidad conforme al destino. | Texto. | `Laboral` | `El nombre de materia '<valor>' viola la regla de unicidad del destino.` |
| 4 | `descripcion` | Texto | 32,767 | No | No truncar. | Texto o vacío. | `Materia laboral.` | Mensaje común de longitud. |
| 5 | `activo` | Booleano | No aplica | Sí | Booleano. | `true`, `false` | `true` | `El valor debe ser true o false.` |

### 4.4 Hoja `SEDES`

| Pos. | Columna | Tipo | Máx. | Oblig. | Validación | Valores permitidos | Ejemplo válido | Mensaje específico |
|---:|---|---|---:|---|---|---|---|---|
| 1 | `sede_codigo` | Texto | 100 | Sí | Único. | Código capturado en esta hoja. | `SEDE_001` | `El código de sede '<valor>' está duplicado o ya existe.` |
| 2 | `nombre` | Texto | 100 | Sí | Unicidad conforme al destino. | Texto. | `Sede Central` | `El nombre de sede '<valor>' viola la regla de unicidad del destino.` |
| 3 | `gerencia_codigo` | Texto | 100 | No | Vacío significa global; si tiene valor debe existir en `GERENCIAS`. | Código de `GERENCIAS` o vacío. | `GER_001` | `La gerencia '<valor>' no existe en GERENCIAS.` |
| 4 | `activo` | Booleano | No aplica | Sí | Booleano. | `true`, `false` | `true` | `El valor debe ser true o false.` |

### 4.5 Hoja `PRIORIDADES`

| Pos. | Columna | Tipo | Máx. | Oblig. | Validación | Valores permitidos | Ejemplo válido | Mensaje específico |
|---:|---|---|---:|---|---|---|---|---|
| 1 | `prioridad_codigo` | Texto | 50 | Sí | Único. Para V1 sólo los valores aprobados. | `ALTA`, `MEDIA`, `BAJA` | `ALTA` | `La prioridad '<valor>' no está permitida para V1.` |
| 2 | `nombre` | Texto | 50 | Sí | No vacío. | Nombre publicado. | `Alta` | Mensajes comunes de obligatoriedad y longitud. |
| 3 | `color` | Texto | 100 | No | Si existe, debe coincidir con el valor técnico suministrado por implementación; no es editable por el cliente. | Valor técnico publicado o vacío. | `(vacío)` | `El color de prioridad no coincide con el valor técnico publicado.` |
| 4 | `activo` | Booleano | No aplica | Sí | Booleano. | `true`, `false` | `true` | `El valor debe ser true o false.` |

### 4.6 Hoja `TIPOS_EXPEDIENTE`

| Pos. | Columna | Tipo | Máx. | Oblig. | Validación | Valores permitidos | Ejemplo válido | Mensaje específico |
|---:|---|---|---:|---|---|---|---|---|
| 1 | `tipo_expediente_codigo` | Texto | 100 | Sí | Único y estable. | Código capturado en esta hoja. | `TIPO_EXP_001` | `El código de tipo de expediente '<valor>' está duplicado o ya existe.` |
| 2 | `nombre` | Texto | 100 | Sí | Unicidad conforme al destino. | Texto. | `Tipo de expediente aprobado` | `El nombre de tipo de expediente '<valor>' viola la regla de unicidad del destino.` |
| 3 | `descripcion` | Texto | 32,767 | No | Debe describir el tipo, sin convertirlo en materia o etapa. No truncar. | Texto o vacío. | `Clasificación aprobada del expediente.` | Mensaje común de longitud. |
| 4 | `gerencia_codigo` | Texto | 100 | No | Vacío significa global; si existe debe referenciar `GERENCIAS`. | Código de `GERENCIAS` o vacío. | `GER_001` | `La gerencia '<valor>' no existe en GERENCIAS.` |
| 5 | `activo` | Booleano | No aplica | Sí | Booleano. | `true`, `false` | `true` | `El valor debe ser true o false.` |

`SIN_CLASIFICAR` sólo puede incluirse cuando exista aprobación del cliente. Si no existe aprobación, el mensaje es: `El tipo de expediente 'SIN_CLASIFICAR' no está autorizado por el cliente.`

### 4.7 Hoja `ORGANOS`

| Pos. | Columna | Tipo | Máx. | Oblig. | Validación | Valores permitidos | Ejemplo válido | Mensaje específico |
|---:|---|---|---:|---|---|---|---|---|
| 1 | `organo_codigo` | Texto | 100 | Sí | Único y estable. | Código capturado en esta hoja. | `ORG_001` | `El código de órgano '<valor>' está duplicado o ya existe.` |
| 2 | `gerencia_codigo` | Texto | 100 | No | Vacío significa global; si existe referencia `GERENCIAS`. | Código de `GERENCIAS` o vacío. | `GER_001` | `La gerencia '<valor>' no existe en GERENCIAS.` |
| 3 | `nombre` | Texto | 300 | Sí | No vacío. | Texto. | `Órgano jurisdiccional de ejemplo` | Mensajes comunes de obligatoriedad y longitud. |
| 4 | `sede_codigo` | Texto | 100 | No | Si existe referencia `SEDES`. | Código de `SEDES` o vacío. | `SEDE_001` | `La sede '<valor>' no existe en SEDES.` |
| 5 | `tipo` | Texto | 50 | No | Debe pertenecer a la lista controlada publicada. | Valor publicado o vacío. | `(vacío)` | `El tipo de órgano '<valor>' no pertenece a la lista publicada.` |
| 6 | `activo` | Booleano | No aplica | Sí | Booleano. | `true`, `false` | `true` | `El valor debe ser true o false.` |

### 4.8 Hoja `TIPOS_AUDIENCIA`

| Pos. | Columna | Tipo | Máx. | Oblig. | Validación | Valores permitidos | Ejemplo válido | Mensaje específico |
|---:|---|---|---:|---|---|---|---|---|
| 1 | `tipo_audiencia_codigo` | Texto | 100 | Sí | Único conforme al destino. | Código capturado en esta hoja. | `AUD_001` | `El código de tipo de audiencia '<valor>' está duplicado o ya existe.` |
| 2 | `gerencia_codigo` | Texto | 100 | Sí | Referencia `GERENCIAS`. | Código de `GERENCIAS`. | `GER_001` | `La gerencia '<valor>' no existe en GERENCIAS.` |
| 3 | `nombre` | Texto | 100 | Sí | No vacío. | Texto. | `Tipo de audiencia aprobado` | Mensajes comunes de obligatoriedad y longitud. |
| 4 | `descripcion` | Texto | 32,767 | No | No truncar. | Texto o vacío. | `Descripción del tipo de audiencia.` | Mensaje común de longitud. |
| 5 | `activo` | Booleano | No aplica | Sí | Booleano. | `true`, `false` | `true` | `El valor debe ser true o false.` |

### 4.9 Hoja `TIPOS_TERMINO`

| Pos. | Columna | Tipo | Máx. | Oblig. | Validación | Valores permitidos | Ejemplo válido | Mensaje específico |
|---:|---|---|---:|---|---|---|---|---|
| 1 | `tipo_termino_codigo` | Texto | 100 | Sí | Único y estable. | Código capturado en esta hoja. | `TERM_001` | `El código de tipo de término '<valor>' está duplicado o ya existe.` |
| 2 | `nombre` | Texto | 100 | Sí | Unicidad conforme al destino. | Texto. | `Tipo de término aprobado` | `El nombre de tipo de término '<valor>' viola la regla de unicidad del destino.` |
| 3 | `descripcion` | Texto | 32,767 | No | Debe distinguirse del asunto concreto. No truncar. | Texto o vacío. | `Regla aplicable al tipo de término.` | Mensaje común de longitud. |
| 4 | `dias_predeterminados` | Entero | No aplica | Sí | Mayor que cero. Es informativo/configuración actual. | Enteros `> 0`. | `5` | `El valor debe ser un entero mayor que cero.` |
| 5 | `tipo_dias` | Texto | 20 | Sí | Lista controlada; no recalcula históricos. | `HABILES`, `NATURALES` | `HABILES` | `El valor debe ser HABILES o NATURALES.` |
| 6 | `semaforo_rojo_dias` | Entero | No aplica | Sí | Mayor o igual a cero. | Enteros `>= 0`. | `1` | `El valor debe ser un entero mayor o igual a cero.` |
| 7 | `semaforo_amarillo_dias` | Entero | No aplica | Sí | Mayor que `semaforo_rojo_dias`. | Entero que cumpla la relación. | `3` | `El valor debe ser mayor que semaforo_rojo_dias.` |
| 8 | `materia_codigo` | Texto | 100 | No | Si existe, referencia `MATERIAS` y debe ser compatible con `gerencia_codigo`. | Código de `MATERIAS` o vacío. | `MAT_001` | `La materia '<valor>' no existe o no es compatible con la gerencia indicada.` |
| 9 | `gerencia_codigo` | Texto | 100 | No | Si existe, referencia `GERENCIAS`. | Código de `GERENCIAS` o vacío. | `GER_001` | `La gerencia '<valor>' no existe en GERENCIAS.` |
| 10 | `activo` | Booleano | No aplica | Sí | Booleano. | `true`, `false` | `true` | `El valor debe ser true o false.` |

### 4.10 Dependencias internas de Catálogos

1. Validar y resolver `GERENCIAS`.
2. Validar `MATERIAS` contra `GERENCIAS`.
3. Validar `SEDES`; su gerencia opcional debe existir.
4. Validar `PRIORIDADES`.
5. Validar `TIPOS_EXPEDIENTE` contra `GERENCIAS` cuando aplique.
6. Validar `ORGANOS` contra `GERENCIAS` y `SEDES` cuando aplique.
7. Validar `TIPOS_AUDIENCIA` contra `GERENCIAS`.
8. Validar `TIPOS_TERMINO` contra `GERENCIAS` y `MATERIAS` cuando aplique.
9. Si cualquier fila falla, no publicar `catalogo_version`.

## 5. `Layout_Usuarios_V2_MV.xlsx`

### 5.1 Hojas y orden exacto

1. `CONTROL`
2. `CAPTURA`
3. `INSTRUCCIONES`
4. `DICCIONARIO`
5. `CATALOGOS_REFERENCIA`

### 5.2 Hoja `CAPTURA`

| Pos. | Columna | Tipo | Máx. | Oblig. | Validación | Valores permitidos | Ejemplo válido | Mensaje específico |
|---:|---|---|---:|---|---|---|---|---|
| 1 | `usuario_clave_origen` | Texto | 100 | Sí | Única por `fuente_codigo`; referencia oficial de migración. | Clave de origen. | `USR_0001` | `La clave de usuario '<valor>' está duplicada para la fuente.` |
| 2 | `operacion` | Texto | 15 | Sí | Regla común de alta/actualización. | `ALTA`, `ACTUALIZACION` | `ALTA` | `El valor debe ser ALTA o ACTUALIZACION.` |
| 3 | `gerencia_codigo` | Texto | 100 | Condicional | Obligatoria para `GERENTE`, `JEFE_DEPTO` y `ABOGADO`; si existe debe estar activa en el catálogo publicado. | Código de `GERENCIAS` o vacío cuando el rol lo permita. | `GER_001` | `La gerencia es obligatoria para el rol '<rol>'.` / `La gerencia '<valor>' no existe o está inactiva.` |
| 4 | `nombre_completo` | Texto | 200 | Sí | No vacío. | Texto. | `Ana Pérez García` | Mensajes comunes de obligatoriedad y longitud. |
| 5 | `email` | Texto | 100 | No | Si existe: correo válido y único; no se usa como llave de migración. | Correo válido o vacío. | `ana.perez@cliente.mx` | `El correo electrónico no tiene un formato válido.` / `El correo '<valor>' ya está asignado.` |
| 6 | `username` | Texto | 80 | Condicional | Obligatorio cuando no exista login externo; único. | Texto único o vacío cuando exista login externo. | `aperez` | `El username es obligatorio cuando no existe login externo.` / `El username '<valor>' ya está asignado.` |
| 7 | `numero_empleado` | Texto | 40 | No | Si existe: único; conservar ceros a la izquierda. | Texto o vacío. | `000184` | `El número de empleado '<valor>' ya está asignado.` |
| 8 | `rol` | Texto | 50 | Sí | Debe pertenecer a roles migrables. `ADMIN` está prohibido. `DIRECTOR_PRINCIPAL` exige autorización. | `DIRECTOR`, `SUBDIRECTOR`, `GERENTE`, `JEFE_DEPTO`, `ABOGADO`; `DIRECTOR_PRINCIPAL` sólo autorizado. | `ABOGADO` | `El rol '<valor>' no es migrable.` / `El rol DIRECTOR_PRINCIPAL no está autorizado para este lote.` |
| 9 | `puesto` | Texto | 160 | No | Texto libre. | Texto o vacío. | `Abogada responsable` | Mensaje común de longitud. |
| 10 | `telefono_institucional` | Texto | 120 | No | Texto libre. | Texto o vacío. | `55 0000 0000 ext. 123` | Mensaje común de longitud. |
| 11 | `telefono_movil` | Texto | 80 | No | Texto libre. | Texto o vacío. | `55 0000 0000` | Mensaje común de longitud. |
| 12 | `ubicacion_oficina` | Texto | 180 | No | Texto libre; representa oficina, no sede jurisdiccional. | Texto o vacío. | `Edificio Central, piso 4` | Mensaje común de longitud. |
| 13 | `materias_codigos` | Texto | 1,000 | No | Códigos separados por `;`; cada código debe existir, estar activo y corresponder a la gerencia del usuario. | Lista de códigos de `MATERIAS` o vacío. | `MAT_001;MAT_002` | `La materia '<código>' no existe, está inactiva o no corresponde a la gerencia del usuario.` |
| 14 | `permisos_adicionales` | Texto | 1,000 | No | Sólo permisos no heredados; cada código debe existir y ser otorgable. | Códigos publicados separados por `;` o vacío. | `PERMISO_01;PERMISO_02` | `El permiso '<código>' no existe o no puede otorgarse como permiso adicional.` |
| 15 | `activo` | Booleano | No aplica | Sí | Booleano. Los usuarios históricos sin acceso deben capturarse como `false`. | `true`, `false` | `true` | `El valor debe ser true o false.` |

La contraseña, los tokens y los códigos de recuperación no forman parte del archivo. La cuenta se crea con el mecanismo de activación vigente.

## 6. `Layout_Expedientes_V2_MV.xlsx`

### 6.1 Hojas y orden exacto

1. `CONTROL`
2. `CAPTURA`
3. `INSTRUCCIONES`
4. `DICCIONARIO`
5. `CATALOGOS_REFERENCIA`

### 6.2 Hoja `CAPTURA`

| Pos. | Columna | Tipo | Máx. | Oblig. | Validación | Valores permitidos | Ejemplo válido | Mensaje específico |
|---:|---|---|---:|---|---|---|---|---|
| 1 | `clave_origen` | Texto | 100 | Sí | Única para la identidad `fuente_codigo + EXPEDIENTE + clave_origen`. | Clave de origen. | `EXP_000001` | `La clave de origen '<valor>' está duplicada para EXPEDIENTE y la fuente.` |
| 2 | `operacion` | Texto | 15 | Sí | Regla común de alta/actualización. | `ALTA`, `ACTUALIZACION` | `ALTA` | `El valor debe ser ALTA o ACTUALIZACION.` |
| 3 | `gerencia_codigo` | Texto | 100 | Sí | Debe existir, estar activa y ser compatible con materia y responsable. | Código de `GERENCIAS`. | `GER_001` | `La gerencia '<valor>' no existe, está inactiva o no es compatible con la materia o el responsable.` |
| 4 | `numero_expediente` | Texto | 50 | Sí | Número oficial. No se usa como llave única. Posibles homónimos se reportan para revisión y no se fusionan. | Texto. | `123/2024` | `El número de expediente es obligatorio.` / `Se detectó otro expediente con el número '<valor>'; requiere revisión y no se fusionará automáticamente.` |
| 5 | `materia_codigo` | Texto | 100 | Sí | Debe existir, estar activa y pertenecer a `gerencia_codigo`. | Código de `MATERIAS`. | `MAT_001` | `La materia '<valor>' no existe, está inactiva o no pertenece a la gerencia '<gerencia_codigo>'.` |
| 6 | `descripcion` | Texto | 32,767 | Sí | No vacía y sin truncamiento. | Texto. | `Descripción histórica íntegra del expediente.` | Mensajes comunes de obligatoriedad y longitud. |
| 7 | `etapa_procesal` | Texto | 30 | Sí | Lista controlada. | `TRAMITE`, `LAUDO`, `FIRME`, `CONCLUIDO` | `TRAMITE` | `La etapa procesal '<valor>' no está permitida.` |
| 8 | `tipo_expediente_codigo` | Texto | 100 | Sí | Debe existir, estar activo y ser aplicable a la gerencia. | Código de `TIPOS_EXPEDIENTE`. | `TIPO_EXP_001` | `El tipo de expediente '<valor>' no existe, está inactivo o no aplica a la gerencia.` |
| 9 | `prioridad_codigo` | Texto | 50 | No | Si existe: debe existir y estar activa. | Código de `PRIORIDADES` o vacío. | `ALTA` | `La prioridad '<valor>' no existe o está inactiva.` |
| 10 | `organo_codigo` | Texto | 100 | No | Si existe: referencia exacta a `ORGANOS`; no resolver por similitud. | Código de `ORGANOS` o vacío. | `ORG_001` | `El órgano '<valor>' no existe; no se permiten coincidencias por nombre.` |
| 11 | `sede_codigo` | Texto | 100 | No | Si existe: referencia `SEDES`; si el órgano tiene sede, debe coincidir. | Código de `SEDES` o vacío. | `SEDE_001` | `La sede '<valor>' no existe o no coincide con la sede del órgano '<organo_codigo>'.` |
| 12 | `partes` | Texto | 32,767 | No | Conservar íntegro como texto; no truncar. | Texto o vacío. | `Actor: Persona A; Demandado: Institución B` | Mensaje común de longitud. |
| 13 | `abogado_responsable_clave_origen` | Texto | 100 | Condicional | Si existe: debe resolver a un usuario con alcance compatible. Es obligatorio para expediente activo cuando el cliente conoce al responsable. | Clave de `USUARIOS` o vacío conforme a la condición. | `USR_0001` | `El usuario '<valor>' no existe o no tiene alcance para la gerencia del expediente.` / `El expediente activo tiene responsable conocido y debe indicarse.` |

## 7. `Layout_Audiencias_V2_MV.xlsx`

### 7.1 Hojas y orden exacto

1. `CONTROL`
2. `CAPTURA`
3. `INSTRUCCIONES`
4. `DICCIONARIO`
5. `CATALOGOS_REFERENCIA`

### 7.2 Hoja `CAPTURA`

| Pos. | Columna | Tipo | Máx. | Oblig. | Validación | Valores permitidos | Ejemplo válido | Mensaje específico |
|---:|---|---|---:|---|---|---|---|---|
| 1 | `clave_origen` | Texto | 100 | Sí | Única para la identidad `fuente_codigo + AUDIENCIA + clave_origen`. | Clave de origen. | `AUD_EVT_0001` | `La clave de origen '<valor>' está duplicada para AUDIENCIA y la fuente.` |
| 2 | `operacion` | Texto | 15 | Sí | Regla común de alta/actualización. | `ALTA`, `ACTUALIZACION` | `ALTA` | `El valor debe ser ALTA o ACTUALIZACION.` |
| 3 | `gerencia_codigo` | Texto | 100 | Sí | Debe coincidir con la gerencia del expediente referenciado. | Código de `GERENCIAS`. | `GER_001` | `La gerencia '<valor>' no coincide con la gerencia del expediente.` |
| 4 | `expediente_clave_origen` | Texto | 100 | Sí | Debe existir en las equivalencias de Expedientes para la misma fuente. | `clave_origen` de Expedientes. | `EXP_000001` | `No existe equivalencia de expediente para la clave '<valor>' y la fuente del lote.` |
| 5 | `tipo_audiencia_codigo` | Texto | 100 | Sí | Debe existir, estar activo y aplicar a la gerencia. | Código de `TIPOS_AUDIENCIA`. | `AUD_001` | `El tipo de audiencia '<valor>' no existe, está inactivo o no aplica a la gerencia.` |
| 6 | `fecha_audiencia` | Fecha | No aplica | Sí | Fecha real de Excel. La fecha por sí sola no cambia el estado. | Fecha calendario válida. | `2026-10-15` | `El valor debe ser una fecha real de Excel.` |
| 7 | `hora_audiencia` | Hora | No aplica | Sí | Hora real de Excel mostrada `hh:mm`; no se acepta un valor de sustitución. | Hora válida. | `10:30` | `El valor debe ser una hora real de Excel en formato hh:mm; no se admite una hora de sustitución.` |
| 8 | `estatus_audiencia` | Texto | 30 | Sí | Lista controlada; no se infiere de la fecha. | `PENDIENTE`, `CON_ACTA`, `CONCLUIDA` | `PENDIENTE` | `El estatus de audiencia '<valor>' no está permitido.` |
| 9 | `sala_lugar` | Texto | 500 | No | Describe el recinto; no se interpreta como órgano. | Texto o vacío. | `Sala 3, planta baja` | Mensaje común de longitud. |
| 10 | `es_virtual` | Booleano | No aplica | Sí | Booleano. | `true`, `false` | `false` | `El valor debe ser true o false.` |
| 11 | `url_reunion` | Texto | 2,048 | Condicional | Obligatoria cuando `es_virtual=true` y `fecha_audiencia > fecha_corte`; si existe debe ser URL válida. | URL válida o vacío conforme a la condición. | `https://reunion.ejemplo.mx/sala/123` | `La URL de reunión es obligatoria para una audiencia virtual futura.` / `El valor debe ser una URL válida.` |
| 12 | `abogado_compareciente_clave_origen` | Texto | 100 | No | Si existe: usuario conocido. Puede estar inactivo cuando sólo conserva historia. | Clave de `USUARIOS` o vacío. | `USR_0001` | `No existe un usuario para la clave de compareciente '<valor>'.` |
| 13 | `usuario_delegado_clave_origen` | Texto | 100 | No | Si existe: usuario conocido; debe estar activo y tener alcance compatible si la audiencia sigue activa. | Clave de `USUARIOS` o vacío. | `USR_0002` | `No existe un usuario para la clave de delegado '<valor>'.` / `El usuario delegado está inactivo o no tiene alcance para una audiencia activa.` |
| 14 | `fecha_desahogo` | Fecha | No aplica | Condicional | Obligatoria para `CONCLUIDA`; prohibida para `PENDIENTE`. | Fecha real o vacío conforme a la condición. | `2026-09-01` | `La fecha de desahogo es obligatoria para una audiencia CONCLUIDA.` / `Una audiencia PENDIENTE no admite fecha de desahogo.` |
| 15 | `estado_acta_codigo` | Texto | 30 | Sí | Lista controlada. | `RECIBIDO`, `PENDIENTE_ENTREGA`, `NO_DISPONIBLE`, `NO_COMPARTIDO`, `NO_APLICA` | `RECIBIDO` | `El estado de acta '<valor>' no está permitido.` |
| 16 | `motivo_falta_acta_codigo` | Texto | 50 | Condicional | Obligatorio para `NO_DISPONIBLE` o `NO_COMPARTIDO`. | `NO_LOCALIZADO`, `EXTRAVIADO`, `NO_GENERADO`, `CLIENTE_NO_COMPARTE`, `OTRO` | `CLIENTE_NO_COMPARTE` | `El motivo de falta de acta es obligatorio para el estado '<estado_acta_codigo>'.` / `El motivo de falta de acta '<valor>' no está permitido.` |
| 17 | `acta_archivo` | Texto | 500 | Condicional | Obligatorio cuando `estado_acta_codigo=RECIBIDO`; ruta relativa; el archivo debe existir y poder recuperarse. | Ruta relativa a un archivo entregado o vacío conforme a la condición. | `audiencias/AUD_EVT_0001/acta.pdf` | `El archivo de acta es obligatorio cuando el estado es RECIBIDO.` / `El archivo '<ruta>' no existe en la entrega del lote.` |
| 18 | `observaciones` | Texto | 32,767 | No | No truncar. | Texto o vacío. | `Registro histórico validado.` | Mensaje común de longitud. |

### 7.3 Reglas cruzadas de Audiencias

1. `CON_ACTA` exige `estado_acta_codigo=RECIBIDO` y `acta_archivo` existente. Mensaje: `Una audiencia CON_ACTA requiere estado de acta RECIBIDO y un archivo existente.`
2. `CONCLUIDA` exige `fecha_desahogo`.
3. `CONCLUIDA` admite un Acta recibida o un estado de faltante documentado conforme a las reglas de `estado_acta_codigo` y `motivo_falta_acta_codigo`.
4. `PENDIENTE` no admite `fecha_desahogo`, aunque `fecha_audiencia` sea anterior a `fecha_corte`.
5. El backend conserva exactamente el estado capturado; no lo deriva de las fechas.
6. Si `estado_acta_codigo=RECIBIDO`, el archivo debe verificarse antes de persistir la audiencia.

## 8. `Layout_Terminos_V2_MV.xlsx`

### 8.1 Hojas y orden exacto

1. `CONTROL`
2. `CAPTURA`
3. `INSTRUCCIONES`
4. `DICCIONARIO`
5. `CATALOGOS_REFERENCIA`

### 8.2 Hoja `CAPTURA`

| Pos. | Columna | Tipo | Máx. | Oblig. | Validación | Valores permitidos | Ejemplo válido | Mensaje específico |
|---:|---|---|---:|---|---|---|---|---|
| 1 | `clave_origen` | Texto | 100 | Sí | Única para la identidad `fuente_codigo + TERMINO + clave_origen`. | Clave de origen. | `TER_EVT_0001` | `La clave de origen '<valor>' está duplicada para TERMINO y la fuente.` |
| 2 | `operacion` | Texto | 15 | Sí | Regla común de alta/actualización. | `ALTA`, `ACTUALIZACION` | `ALTA` | `El valor debe ser ALTA o ACTUALIZACION.` |
| 3 | `gerencia_codigo` | Texto | 100 | Sí | Debe coincidir con la gerencia del expediente. | Código de `GERENCIAS`. | `GER_001` | `La gerencia '<valor>' no coincide con la gerencia del expediente.` |
| 4 | `expediente_clave_origen` | Texto | 100 | Sí | Debe existir en las equivalencias de Expedientes para la misma fuente. | `clave_origen` de Expedientes. | `EXP_000001` | `No existe equivalencia de expediente para la clave '<valor>' y la fuente del lote.` |
| 5 | `tipo_termino_codigo` | Texto | 100 | Sí | Debe existir, estar activo y aplicar a materia/gerencia. Debe persistirse en la FK correspondiente. | Código de `TIPOS_TERMINO`. | `TERM_001` | `El tipo de término '<valor>' no existe, está inactivo o no aplica a la materia o gerencia del expediente.` |
| 6 | `asunto` | Texto | 32,767 | Sí | Describe la actuación concreta; no es el tipo de término. No truncar. | Texto. | `Presentar contestación dentro del plazo registrado.` | Mensajes comunes de obligatoriedad y longitud. |
| 7 | `fecha_vencimiento` | Fecha | No aplica | Sí | Fecha histórica real; se conserva sin recalcular. | Fecha calendario válida. | `2026-09-30` | `El valor debe ser una fecha real de Excel.` |
| 8 | `fecha_ingreso` | Fecha | No aplica | No | Si existe: fecha histórica real; no se sustituye por fecha de carga. | Fecha calendario válida o vacío. | `2026-09-20` | `El valor debe ser una fecha real de Excel o quedar vacío.` |
| 9 | `estatus_termino` | Texto | 30 | Sí | Lista controlada; no se infiere de la fecha de vencimiento. | `PROYECTISTA`, `REVISION`, `GERENCIA`, `DIRECCION`, `LIBERADO`, `PRESENTADO`, `CONCLUIDO` | `PROYECTISTA` | `El estatus de término '<valor>' no está permitido.` |
| 10 | `prioridad_codigo` | Texto | 50 | No | Si existe: referencia activa a `PRIORIDADES`. | Código de `PRIORIDADES` o vacío. | `ALTA` | `La prioridad '<valor>' no existe o está inactiva.` |
| 11 | `abogado_responsable_clave_origen` | Texto | 100 | Condicional | Obligatorio para términos abiertos; debe resolver a un usuario activo y con alcance compatible. Para esta regla, abiertos son `PROYECTISTA`, `REVISION`, `GERENCIA`, `DIRECCION` y `LIBERADO`. | Clave de `USUARIOS`; vacío sólo para `PRESENTADO` o `CONCLUIDO`. | `USR_0001` | `El abogado responsable es obligatorio para un término abierto.` / `El usuario '<valor>' no existe, está inactivo o no tiene alcance.` |
| 12 | `fecha_presentacion` | Fecha | No aplica | Condicional | Obligatoria para `PRESENTADO`. En `CONCLUIDO` puede quedar vacía si `observaciones` documenta el motivo. Los demás estados no la admiten y deben revisarse antes de cargar. | Fecha real o vacío conforme a la condición. | `2026-09-28` | `La fecha de presentación es obligatoria para un término PRESENTADO.` / `El estado '<estatus_termino>' no admite fecha de presentación; revise la fuente antes de cargar.` |
| 13 | `estado_acuse_codigo` | Texto | 30 | Sí | Lista controlada. | `RECIBIDO`, `PENDIENTE_ENTREGA`, `NO_DISPONIBLE`, `NO_COMPARTIDO`, `NO_APLICA` | `RECIBIDO` | `El estado de acuse '<valor>' no está permitido.` |
| 14 | `motivo_falta_acuse_codigo` | Texto | 50 | Condicional | Obligatorio para `NO_DISPONIBLE` o `NO_COMPARTIDO`. | `NO_LOCALIZADO`, `EXTRAVIADO`, `NO_GENERADO`, `CLIENTE_NO_COMPARTE`, `OTRO` | `NO_LOCALIZADO` | `El motivo de falta de acuse es obligatorio para el estado '<estado_acuse_codigo>'.` / `El motivo de falta de acuse '<valor>' no está permitido.` |
| 15 | `acuse_archivo` | Texto | 500 | Condicional | Obligatorio cuando `estado_acuse_codigo=RECIBIDO`; ruta relativa; el archivo debe existir y poder recuperarse. | Ruta relativa a un archivo entregado o vacío conforme a la condición. | `terminos/TER_EVT_0001/acuse.pdf` | `El archivo de acuse es obligatorio cuando el estado es RECIBIDO.` / `El archivo '<ruta>' no existe en la entrega del lote.` |
| 16 | `observaciones` | Texto | 32,767 | No | No truncar. Obligatoria para `CONCLUIDO` sin `fecha_presentacion`, a fin de documentar el motivo. | Texto o vacío conforme a la condición. | `Concluido por causa distinta de presentación.` | `Las observaciones son obligatorias para un término CONCLUIDO sin fecha de presentación.` |

### 8.3 Reglas cruzadas de Términos

1. `PRESENTADO` exige `fecha_presentacion`.
2. Un término `PRESENTADO` sin Acuse debe usar un estado de faltante permitido y, cuando corresponda, su motivo; no se crea un archivo de sustitución.
3. `CONCLUIDO` sin `fecha_presentacion` exige `observaciones` no vacías.
4. `PROYECTISTA`, `REVISION`, `GERENCIA`, `DIRECCION` y `LIBERADO` no admiten `fecha_presentacion`; cualquier inconsistencia se corrige o documenta antes de cargar.
5. `fecha_vencimiento` se persiste exactamente como fue entregada y no se recalcula con `dias_predeterminados`, `tipo_dias` ni semáforos.
6. El backend conserva exactamente `estatus_termino`; no lo deriva de la fecha de vencimiento.
7. `tipo_termino_codigo` debe resolver y persistir la relación con el Tipo de Término.
8. Si `estado_acuse_codigo=RECIBIDO`, el archivo debe verificarse antes de persistir el término.

## 9. Valores controlados consolidados

| Lista | Valores exactos |
|---|---|
| `OPERACIONES_CARGA` | `ALTA`, `ACTUALIZACION` |
| `BOOLEANOS` | `true`, `false` |
| `ROLES_MIGRABLES` | `DIRECTOR`, `SUBDIRECTOR`, `GERENTE`, `JEFE_DEPTO`, `ABOGADO`; `DIRECTOR_PRINCIPAL` sólo con autorización |
| Rol excluido | `ADMIN` |
| `ETAPAS_PROCESALES` | `TRAMITE`, `LAUDO`, `FIRME`, `CONCLUIDO` |
| `ESTATUS_AUDIENCIA` | `PENDIENTE`, `CON_ACTA`, `CONCLUIDA` |
| `ESTATUS_TERMINO` | `PROYECTISTA`, `REVISION`, `GERENCIA`, `DIRECCION`, `LIBERADO`, `PRESENTADO`, `CONCLUIDO` |
| `ESTADOS_DOCUMENTO` | `RECIBIDO`, `PENDIENTE_ENTREGA`, `NO_DISPONIBLE`, `NO_COMPARTIDO`, `NO_APLICA` |
| `MOTIVOS_FALTA_DOCUMENTO` | `NO_LOCALIZADO`, `EXTRAVIADO`, `NO_GENERADO`, `CLIENTE_NO_COMPARTE`, `OTRO` |
| `TIPOS_DIAS` | `HABILES`, `NATURALES` |
| Prioridades V1 | `ALTA`, `MEDIA`, `BAJA`, salvo aprobación expresa de otra configuración |
| `PERMISOS` | Códigos existentes y otorgables publicados, con título y descripción |
| Tipo de órgano | Valores de la lista controlada publicada |

## 10. Diccionario de datos consolidado

La obligatoriedad y las reglas condicionales se aplican conforme a la tabla específica de cada layout. Este diccionario consolida el significado, tipo y longitud de cada columna del contrato.

| Columna | Archivo/hoja | Definición funcional | Tipo | Máx. |
|---|---|---|---|---:|
| `layout_version` | Todos / `CONTROL` | Versión exacta del contrato de layout. | Texto | 10 |
| `fuente_codigo` | Todos / `CONTROL` | Código estable del sistema o fuente histórica que entrega los registros. | Texto | 50 |
| `lote_clave` | Todos / `CONTROL` | Identificador único de la entrega; no identifica entidades. | Texto | 100 |
| `catalogo_version` | Todos / `CONTROL` | Versión publicada de catálogos usada para capturar y validar el lote. | Texto | 50 |
| `fecha_corte` | Todos / `CONTROL` | Fecha hasta la cual corresponde la información histórica entregada. | Fecha | No aplica |
| `responsable_entrega` | Todos / `CONTROL` | Persona responsable de la entrega. | Texto | 200 |
| `email_responsable` | Todos / `CONTROL` | Correo de contacto del responsable; no es llave. | Texto | 100 |
| `gerencia_codigo` | Catálogos y capturas | Código estable de gerencia o ámbito gerencial. | Texto | 100 |
| `nombre` | Hojas de Catálogos | Nombre visible del elemento de catálogo. | Texto | Según hoja |
| `descripcion` | Catálogos, Expedientes | Descripción íntegra del elemento o expediente. | Texto | 32,767 |
| `activo` | Catálogos, Usuarios | Indicador de disponibilidad o acceso. | Booleano | No aplica |
| `materia_codigo` | Catálogos, Expedientes | Código estable de materia. | Texto | 100 |
| `sede_codigo` | Catálogos, Expedientes | Código estable de sede. | Texto | 100 |
| `prioridad_codigo` | Catálogos, Expedientes, Términos | Código estable de prioridad. | Texto | 50 |
| `color` | Catálogos / `PRIORIDADES` | Valor técnico de visualización suministrado por implementación. | Texto | 100 |
| `tipo_expediente_codigo` | Catálogos, Expedientes | Código estable del Tipo de Expediente. | Texto | 100 |
| `organo_codigo` | Catálogos, Expedientes | Código estable del órgano jurisdiccional. | Texto | 100 |
| `tipo` | Catálogos / `ORGANOS` | Tipo de órgano según lista publicada. | Texto | 50 |
| `tipo_audiencia_codigo` | Catálogos, Audiencias | Código del Tipo de Audiencia aplicable. | Texto | 100 |
| `tipo_termino_codigo` | Catálogos, Términos | Código del Tipo de Término que debe persistirse en su relación de destino. | Texto | 100 |
| `dias_predeterminados` | Catálogos / `TIPOS_TERMINO` | Días configurados para el tipo; informativos en la migración histórica. | Entero | No aplica |
| `tipo_dias` | Catálogos / `TIPOS_TERMINO` | Indica si la configuración usa días hábiles o naturales. | Texto | 20 |
| `semaforo_rojo_dias` | Catálogos / `TIPOS_TERMINO` | Umbral rojo configurado para el tipo. | Entero | No aplica |
| `semaforo_amarillo_dias` | Catálogos / `TIPOS_TERMINO` | Umbral amarillo configurado para el tipo. | Entero | No aplica |
| `usuario_clave_origen` | Usuarios | Clave estable del usuario en la fuente. | Texto | 100 |
| `clave_origen` | Expedientes, Audiencias, Términos | Clave estable del registro en la fuente, única por entidad. | Texto | 100 |
| `operacion` | Usuarios, Expedientes, Audiencias, Términos | Indica alta o actualización del registro. | Texto | 15 |
| `nombre_completo` | Usuarios | Nombre visible completo del usuario. | Texto | 200 |
| `email` | Usuarios | Correo opcional y único del usuario; no es llave. | Texto | 100 |
| `username` | Usuarios | Nombre único de acceso cuando no se usa login externo. | Texto | 80 |
| `numero_empleado` | Usuarios | Identificador laboral opcional tratado como texto. | Texto | 40 |
| `rol` | Usuarios | Rol migrable asignado al usuario. | Texto | 50 |
| `puesto` | Usuarios | Puesto laboral descriptivo. | Texto | 160 |
| `telefono_institucional` | Usuarios | Teléfono institucional descriptivo. | Texto | 120 |
| `telefono_movil` | Usuarios | Teléfono móvil descriptivo. | Texto | 80 |
| `ubicacion_oficina` | Usuarios | Ubicación administrativa del usuario. | Texto | 180 |
| `materias_codigos` | Usuarios | Lista de códigos de materias separados por punto y coma. | Texto | 1,000 |
| `permisos_adicionales` | Usuarios | Lista de permisos excepcionales separados por punto y coma. | Texto | 1,000 |
| `numero_expediente` | Expedientes | Número oficial visible del expediente; no es llave única. | Texto | 50 |
| `etapa_procesal` | Expedientes | Etapa procesal histórica controlada. | Texto | 30 |
| `partes` | Expedientes | Texto histórico íntegro de las partes. | Texto | 32,767 |
| `abogado_responsable_clave_origen` | Expedientes, Términos | Clave de origen del abogado responsable. | Texto | 100 |
| `expediente_clave_origen` | Audiencias, Términos | Clave que enlaza la actuación con la equivalencia del expediente. | Texto | 100 |
| `fecha_audiencia` | Audiencias | Fecha programada o histórica de la audiencia. | Fecha | No aplica |
| `hora_audiencia` | Audiencias | Hora real de la audiencia. | Hora | No aplica |
| `estatus_audiencia` | Audiencias | Estado explícito de la audiencia. | Texto | 30 |
| `sala_lugar` | Audiencias | Recinto o lugar; no representa un órgano. | Texto | 500 |
| `es_virtual` | Audiencias | Indica si la audiencia es virtual. | Booleano | No aplica |
| `url_reunion` | Audiencias | Enlace para la audiencia virtual. | Texto | 2,048 |
| `abogado_compareciente_clave_origen` | Audiencias | Clave del abogado que compareció. | Texto | 100 |
| `usuario_delegado_clave_origen` | Audiencias | Clave del usuario delegado. | Texto | 100 |
| `fecha_desahogo` | Audiencias | Fecha en que se desahogó una audiencia concluida. | Fecha | No aplica |
| `estado_acta_codigo` | Audiencias | Estado documental del Acta. | Texto | 30 |
| `motivo_falta_acta_codigo` | Audiencias | Motivo controlado de falta del Acta. | Texto | 50 |
| `acta_archivo` | Audiencias | Ruta relativa del Acta recibida. | Texto | 500 |
| `asunto` | Términos | Actuación concreta controlada por el término; no es su tipo. | Texto | 32,767 |
| `fecha_vencimiento` | Términos | Fecha histórica de vencimiento que se conserva sin recalcular. | Fecha | No aplica |
| `fecha_ingreso` | Términos | Fecha histórica de incorporación al control. | Fecha | No aplica |
| `estatus_termino` | Términos | Estado explícito del término. | Texto | 30 |
| `fecha_presentacion` | Términos | Fecha de presentación cuando corresponde al estado. | Fecha | No aplica |
| `estado_acuse_codigo` | Términos | Estado documental del Acuse. | Texto | 30 |
| `motivo_falta_acuse_codigo` | Términos | Motivo controlado de falta del Acuse. | Texto | 50 |
| `acuse_archivo` | Términos | Ruta relativa del Acuse recibido. | Texto | 500 |
| `observaciones` | Audiencias, Términos | Texto histórico o justificación requerida por una regla del estado. | Texto | 32,767 |

## 11. Matriz de dependencias entre layouts

| Layout | Requiere antes de prevalidar | Requiere antes de cargar | Produce referencias para | Puede cargarse en paralelo con |
|---|---|---|---|---|
| Catálogos | `CONTROL` válido y aprobación de valores | Ningún otro layout | Usuarios, Expedientes, Audiencias, Términos | Ninguno durante su publicación |
| Usuarios | Catálogos publicados en `catalogo_version` | Catálogos | Expedientes, Audiencias, Términos | Ninguno antes de Expedientes |
| Expedientes | Catálogos y Usuarios disponibles | Catálogos y Usuarios | Audiencias y Términos | Ninguno antes de actuaciones |
| Audiencias | Catálogos, Usuarios y equivalencias de Expedientes | Catálogos, Usuarios y Expedientes | Conciliación | Términos, una vez resueltas todas sus dependencias |
| Términos | Catálogos, Usuarios y equivalencias de Expedientes | Catálogos, Usuarios y Expedientes | Conciliación | Audiencias, una vez resueltas todas sus dependencias |

Dependencias de referencia:

| Origen | Campo | Destino requerido |
|---|---|---|
| Usuarios | `gerencia_codigo` | `GERENCIAS.gerencia_codigo` |
| Usuarios | `materias_codigos` | `MATERIAS.materia_codigo` |
| Usuarios | `permisos_adicionales` | Catálogo publicado de permisos otorgables |
| Expedientes | `gerencia_codigo` | `GERENCIAS.gerencia_codigo` |
| Expedientes | `materia_codigo` | `MATERIAS.materia_codigo` |
| Expedientes | `tipo_expediente_codigo` | `TIPOS_EXPEDIENTE.tipo_expediente_codigo` |
| Expedientes | `prioridad_codigo` | `PRIORIDADES.prioridad_codigo` |
| Expedientes | `organo_codigo` | `ORGANOS.organo_codigo` |
| Expedientes | `sede_codigo` | `SEDES.sede_codigo` |
| Expedientes | `abogado_responsable_clave_origen` | Equivalencia de Usuario |
| Audiencias | `expediente_clave_origen` | Equivalencia de Expediente |
| Audiencias | `tipo_audiencia_codigo` | `TIPOS_AUDIENCIA.tipo_audiencia_codigo` |
| Audiencias | claves de compareciente/delegado | Equivalencia de Usuario |
| Términos | `expediente_clave_origen` | Equivalencia de Expediente |
| Términos | `tipo_termino_codigo` | `TIPOS_TERMINO.tipo_termino_codigo` |
| Términos | `prioridad_codigo` | `PRIORIDADES.prioridad_codigo` |
| Términos | `abogado_responsable_clave_origen` | Equivalencia de Usuario |

## 12. Reglas de carga

### 12.1 Prevalidación obligatoria

1. Recibir los cinco archivos de la entrega.
2. Verificar nombre de archivo, extensión, nombres y orden de hojas.
3. Verificar encabezados exactos y su orden.
4. Validar `CONTROL` y la coincidencia de sus valores entre libros.
5. Validar tipo, longitud, obligatoriedad y listas controladas de todas las filas.
6. Validar unicidad dentro de cada hoja y contra las equivalencias existentes.
7. Resolver todas las referencias de catálogos y entidades por código o clave exacta.
8. Ejecutar las validaciones cruzadas del apartado 17.
9. Verificar la existencia y recuperación de cada `acta_archivo` y `acuse_archivo` declarado `RECIBIDO`.
10. Generar un reporte de prevalidación por fila sin persistir datos operativos.
11. Permitir la carga únicamente con la misma interpretación y la misma versión de catálogos que se prevalidó.

### 12.2 Secuencia de persistencia

1. Publicar Catálogos.
2. Cargar Usuarios.
3. Cargar Expedientes.
4. Cargar Audiencias y Términos después de que existan las equivalencias de Expedientes.
5. Incorporar los archivos de Acta y Acuse asociados a sus filas validadas.
6. Ejecutar conciliación.
7. Confirmar responsables vigentes y abrir la operación/notificaciones.

### 12.3 Procesamiento de filas

- Cada fila recibe uno de estos resultados de conciliación: `IMPORTADO`, `ACTUALIZADO`, `OMITIDO_JUSTIFICADO` o `RECHAZADO`.
- El reporte incluye archivo, hoja, número de fila, identidad de origen, resultado, causa e ID destino cuando exista.
- Una fila `RECHAZADO` no crea ni modifica equivalencias ni registros operativos.
- Una fila válida se persiste con los valores explícitos del archivo; no se completan valores mediante inferencias.
- Los archivos asociados se consideran parte de la validez de su fila.
- Las hojas informativas nunca se recorren como datos.

## 13. Reglas de actualización

1. `ACTUALIZACION` exige una equivalencia existente para `(fuente_codigo, entidad, clave_origen)` o `(fuente_codigo, USUARIO, usuario_clave_origen)`.
2. Si no existe equivalencia, rechazar con: `La operación ACTUALIZACION requiere una equivalencia existente para la clave '<valor>'.`
3. Una actualización conserva la identidad y el ID destino existentes.
4. La clave de origen usada para localizar la equivalencia no se modifica durante esa operación.
5. La fila de actualización debe contener todos los campos obligatorios y satisfacer las mismas validaciones que una alta.
6. La fila representa el estado completo de los campos del contrato: un campo opcional vacío deja vacío el valor correspondiente cuando el destino lo permite.
7. No se actualiza por correo, username, número de expediente, nombre ni coincidencia aproximada.
8. Las referencias actualizadas deben existir, estar activas cuando así lo exige el campo y mantener compatibilidad de gerencia/materia.
9. Cambiar la referencia de expediente de una Audiencia o Término exige que la nueva equivalencia exista y que todas las validaciones de gerencia y alcance sigan cumpliéndose.
10. Para Catálogos, el código estable identifica el registro: un código inexistente se crea y un código existente conserva su identidad y recibe los valores del libro publicado, sujeto a sus reglas de unicidad.

## 14. Reglas de idempotencia

1. La identidad de un registro es `(fuente_codigo, entidad, clave_origen)`; para Usuarios, `usuario_clave_origen` cumple la función de `clave_origen`.
2. Debe existir una restricción única para esa identidad en las equivalencias.
3. `lote_clave` identifica la entrega, pero no cambia la identidad del registro.
4. Reprocesar la misma `lote_clave` con el mismo contenido no crea registros ni archivos duplicados y devuelve los resultados ya conciliados.
5. Reutilizar la misma `lote_clave` con contenido diferente se rechaza antes de persistir.
6. Una fila `ALTA` con equivalencia previamente creada se rechaza con: `La operación ALTA requiere una clave nueva; ya existe equivalencia para '<valor>'.`
7. Una fila `ACTUALIZACION` que no cambia ningún valor se reporta `OMITIDO_JUSTIFICADO` con causa `El registro ya contiene los valores entregados.`
8. Una fila `ACTUALIZACION` con cambios válidos se reporta `ACTUALIZADO` y conserva el mismo ID destino.
9. Dos cargas concurrentes de la misma identidad se resuelven mediante la restricción única; sólo una puede crear la equivalencia.
10. Una fila rechazada puede corregirse y reenviarse sin riesgo de duplicidad porque no creó equivalencia.
11. La incorporación de Acta o Acuse debe verificar si el archivo de la misma fila ya fue procesado antes de volver a almacenarlo.

## 15. Reglas de conciliación

### 15.1 Ecuación obligatoria

Para cada hoja importable y para el lote completo:

`RECIBIDOS = IMPORTADOS + ACTUALIZADOS + OMITIDOS_JUSTIFICADOS + RECHAZADOS`

No se contabilizan la fila de encabezado, filas totalmente vacías ni hojas informativas.

### 15.2 Contenido mínimo del reporte

| Dato | Regla |
|---|---|
| Archivo | Nombre exacto del libro procesado. |
| Hoja | Nombre exacto de la hoja importable. |
| Fila | Número real de fila en Excel. |
| Fuente | `fuente_codigo`. |
| Entidad | Catálogo concreto, Usuario, Expediente, Audiencia o Término. |
| Clave de origen | `clave_origen` o `usuario_clave_origen`; para catálogos, su código estable. |
| Operación | `ALTA` o `ACTUALIZACION` cuando exista en el layout. |
| Resultado | Uno de los cuatro resultados definidos. |
| Causa | Vacía en éxito o mensaje específico/común aplicable. |
| ID destino | Obligatorio para importados, actualizados y omitidos que ya tienen equivalencia. |

### 15.3 Controles de cierre

- Todos los registros recibidos deben aparecer exactamente una vez en el reporte.
- Cada Audiencia y Término importado debe apuntar al ID destino del expediente resuelto por equivalencia.
- Cada Tipo de Término importado debe quedar persistido en su relación de destino.
- Cada archivo declarado `RECIBIDO` debe existir y recuperarse desde el registro importado.
- Un documento faltante no puede figurar como archivo recibido.
- Antes de habilitar notificaciones, los eventos pendientes y términos abiertos deben tener responsable vigente conforme a las reglas del contrato.
- La conciliación se conserva asociada a `lote_clave`.

## 16. Validaciones cruzadas

| ID | Campos relacionados | Regla | Mensaje de rechazo |
|---|---|---|---|
| VC-01 | Todos los `CONTROL` | Los cinco libros deben compartir versión, fuente, lote, catálogo y fecha de corte. | `Los metadatos de CONTROL no coinciden entre los archivos de la entrega.` |
| VC-02 | `catalogo_version` + catálogos | Todos los códigos deben resolverse contra la versión publicada indicada. | `El código '<valor>' no pertenece a la versión de catálogo '<catalogo_version>'.` |
| VC-03 | `operacion` + clave | `ALTA` requiere ausencia de equivalencia. | `La operación ALTA requiere una clave nueva; ya existe equivalencia para '<valor>'.` |
| VC-04 | `operacion` + clave | `ACTUALIZACION` requiere equivalencia previa. | `La operación ACTUALIZACION requiere una equivalencia existente para la clave '<valor>'.` |
| VC-05 | Materia + gerencia | La materia debe pertenecer a la gerencia indicada. | `La materia '<materia_codigo>' no pertenece a la gerencia '<gerencia_codigo>'.` |
| VC-06 | Sede + gerencia | Una sede con gerencia sólo puede usarse dentro de su ámbito; una sede global no restringe gerencia. | `La sede '<sede_codigo>' no aplica a la gerencia '<gerencia_codigo>'.` |
| VC-07 | Órgano + sede | Si el órgano tiene sede, `sede_codigo` del expediente debe coincidir cuando se capture. | `La sede '<sede_codigo>' no coincide con la sede del órgano '<organo_codigo>'.` |
| VC-08 | Tipo de expediente + gerencia | El tipo global aplica a cualquier gerencia; el tipo con gerencia sólo a esa gerencia. | `El tipo de expediente '<tipo_expediente_codigo>' no aplica a la gerencia '<gerencia_codigo>'.` |
| VC-09 | Usuario + rol + gerencia | `GERENTE`, `JEFE_DEPTO` y `ABOGADO` requieren gerencia. | `La gerencia es obligatoria para el rol '<rol>'.` |
| VC-10 | Usuario + materias | Cada materia asignada debe corresponder a la gerencia del usuario. | `La materia '<materia_codigo>' no corresponde a la gerencia del usuario.` |
| VC-11 | Usuario + permisos | Cada permiso adicional debe existir y ser otorgable; no se capturan permisos heredados como sustitución del rol. | `El permiso '<permiso>' no existe o no puede otorgarse como permiso adicional.` |
| VC-12 | Expediente + responsable | Si se captura responsable, el usuario debe existir y tener alcance compatible. | `El responsable '<clave>' no existe o no tiene alcance para la gerencia del expediente.` |
| VC-13 | Actuación + expediente | `expediente_clave_origen` debe resolver en la misma fuente. | `No existe equivalencia de expediente para '<expediente_clave_origen>' en la fuente '<fuente_codigo>'.` |
| VC-14 | Audiencia + gerencia | La gerencia de Audiencia debe coincidir con la del expediente. | `La gerencia de la audiencia no coincide con la gerencia del expediente.` |
| VC-15 | Audiencia + tipo | El Tipo de Audiencia debe aplicar a la gerencia del expediente. | `El tipo de audiencia '<tipo_audiencia_codigo>' no aplica a la gerencia del expediente.` |
| VC-16 | Audiencia virtual futura + URL | Si `es_virtual=true` y `fecha_audiencia > fecha_corte`, la URL es obligatoria y válida. | `La URL de reunión es obligatoria para una audiencia virtual futura.` |
| VC-17 | Audiencia `CON_ACTA` + Acta | Exige estado `RECIBIDO` y archivo existente. | `Una audiencia CON_ACTA requiere estado de acta RECIBIDO y un archivo existente.` |
| VC-18 | Audiencia `CONCLUIDA` + desahogo | Exige `fecha_desahogo`. | `La fecha de desahogo es obligatoria para una audiencia CONCLUIDA.` |
| VC-19 | Audiencia `PENDIENTE` + desahogo | No admite `fecha_desahogo`. | `Una audiencia PENDIENTE no admite fecha de desahogo.` |
| VC-20 | Estado de Acta + motivo | `NO_DISPONIBLE` y `NO_COMPARTIDO` exigen motivo. | `El motivo de falta de acta es obligatorio para el estado '<estado_acta_codigo>'.` |
| VC-21 | Estado de Acta + archivo | `RECIBIDO` exige ruta relativa y archivo recuperable. | `El archivo de acta es obligatorio y debe existir cuando el estado es RECIBIDO.` |
| VC-22 | Término + gerencia | La gerencia del Término debe coincidir con la del expediente. | `La gerencia del término no coincide con la gerencia del expediente.` |
| VC-23 | Término + tipo | El Tipo de Término debe aplicar a materia y gerencia del expediente. | `El tipo de término '<tipo_termino_codigo>' no aplica a la materia o gerencia del expediente.` |
| VC-24 | Término abierto + responsable | Los estados `PROYECTISTA`, `REVISION`, `GERENCIA`, `DIRECCION` y `LIBERADO` exigen responsable activo y con alcance. | `El abogado responsable es obligatorio y debe estar vigente para un término abierto.` |
| VC-25 | `PRESENTADO` + presentación | Exige `fecha_presentacion`. | `La fecha de presentación es obligatoria para un término PRESENTADO.` |
| VC-26 | `CONCLUIDO` sin presentación | Exige `observaciones`. | `Las observaciones son obligatorias para un término CONCLUIDO sin fecha de presentación.` |
| VC-27 | Otros estados + presentación | No admiten fecha de presentación; la fuente debe revisarse antes de carga. | `El estado '<estatus_termino>' no admite fecha de presentación; revise la fuente antes de cargar.` |
| VC-28 | Estado de Acuse + motivo | `NO_DISPONIBLE` y `NO_COMPARTIDO` exigen motivo. | `El motivo de falta de acuse es obligatorio para el estado '<estado_acuse_codigo>'.` |
| VC-29 | Estado de Acuse + archivo | `RECIBIDO` exige ruta relativa y archivo recuperable. | `El archivo de acuse es obligatorio y debe existir cuando el estado es RECIBIDO.` |
| VC-30 | Tipo de Término + semáforos | `dias_predeterminados > 0`, rojo `>= 0` y amarillo `> rojo`. | `La configuración de días y semáforos del Tipo de Término es inválida.` |

## 17. Casos de prueba de aceptación

### 17.1 Estructura y CONTROL

| ID | Escenario | Entrada/condición | Resultado esperado |
|---|---|---|---|
| CP-001 | Libro correcto | Nombre, extensión, hojas y encabezados exactos. | Prevalidación estructural aprobada. |
| CP-002 | Falta `CONTROL` | Libro sin hoja `CONTROL`. | Libro rechazado: `Falta la hoja obligatoria 'CONTROL'.` |
| CP-003 | Columna renombrada | `clave origen` en lugar de `clave_origen`. | Libro rechazado por encabezado ausente y adicional. |
| CP-004 | Columna extra | Columna `comentarios_migracion` no definida. | Libro rechazado: columna fuera del contrato. |
| CP-005 | Versión incorrecta | `layout_version=2.0`. | Libro rechazado: debe ser `2.0-MV`. |
| CP-006 | Controles inconsistentes | Audiencias tiene otro `catalogo_version`. | Entrega rechazada antes de persistir. |
| CP-007 | Fecha como texto | `fecha_corte` escrita como cadena, no fecha Excel. | Libro rechazado por tipo inválido. |
| CP-008 | Repetición estable | Mismo libro prevalidado en días distintos. | Mismo resultado; las condiciones futuras se comparan con `fecha_corte`. |

### 17.2 Catálogos

| ID | Escenario | Entrada/condición | Resultado esperado |
|---|---|---|---|
| CP-009 | Catálogos autoconsistentes | `MAT_001` referencia `GER_001` existente. | Filas válidas. |
| CP-010 | Materia huérfana | Materia referencia gerencia inexistente. | Fila rechazada y versión no publicada. |
| CP-011 | Sede global | `gerencia_codigo` vacío. | Fila válida. |
| CP-012 | Prioridad aprobada | `prioridad_codigo=ALTA`. | Fila válida. |
| CP-013 | Prioridad no aprobada | Código diferente sin aprobación. | Fila rechazada. |
| CP-014 | Tipo de término válido | Días `5`, rojo `1`, amarillo `3`. | Fila válida. |
| CP-015 | Semáforos inválidos | Rojo `3`, amarillo `3`. | Fila rechazada: amarillo debe ser mayor. |
| CP-016 | `SIN_CLASIFICAR` autorizado | Existe aprobación del cliente. | Tipo de expediente válido. |
| CP-017 | `SIN_CLASIFICAR` no autorizado | No existe aprobación. | Fila rechazada. |

### 17.3 Usuarios

| ID | Escenario | Entrada/condición | Resultado esperado |
|---|---|---|---|
| CP-018 | Abogado válido | Clave nueva, gerencia válida, rol `ABOGADO`. | Usuario importado y equivalencia creada. |
| CP-019 | Abogado sin gerencia | Rol `ABOGADO`, gerencia vacía. | Fila rechazada. |
| CP-020 | Rol excluido | `rol=ADMIN`. | Fila rechazada. |
| CP-021 | Director principal no autorizado | `rol=DIRECTOR_PRINCIPAL` sin autorización. | Fila rechazada. |
| CP-022 | Usuario histórico | `activo=false`. | Usuario importado sin acceso. |
| CP-023 | Materia incompatible | Materia de otra gerencia en `materias_codigos`. | Fila rechazada. |
| CP-024 | Permiso inválido | Código inexistente/no otorgable. | Fila rechazada. |
| CP-025 | Número con ceros | `numero_empleado=000184`. | Se conserva exactamente como texto. |

### 17.4 Expedientes

| ID | Escenario | Entrada/condición | Resultado esperado |
|---|---|---|---|
| CP-026 | Expediente válido | Referencias activas y compatibles. | Expediente importado y equivalencia creada. |
| CP-027 | Número repetido con claves distintas | Dos claves de origen comparten `123/2024`. | Se reporta conflicto para revisión; no se fusionan automáticamente. |
| CP-028 | Materia de otra gerencia | `materia_codigo` incompatible. | Fila rechazada. |
| CP-029 | Sede distinta al órgano | Órgano con sede publicada diferente. | Fila rechazada. |
| CP-030 | Texto máximo permitido | Descripción/partes dentro de 32,767. | Se conserva íntegro. |
| CP-031 | Texto excedido | Descripción de 32,768 caracteres. | Fila rechazada; no se trunca. |
| CP-032 | Responsable por correo | Se coloca correo en campo de clave. | Fila rechazada al no existir equivalencia exacta. |

### 17.5 Audiencias

| ID | Escenario | Entrada/condición | Resultado esperado |
|---|---|---|---|
| CP-033 | Audiencia pendiente válida | Expediente válido, fecha/hora reales, sin desahogo. | Audiencia importada. |
| CP-034 | Pendiente histórica vencida | Fecha anterior a corte y estado `PENDIENTE`. | Se conserva `PENDIENTE`; no se infiere otro estado. |
| CP-035 | Virtual futura sin URL | `es_virtual=true`, fecha posterior al corte, URL vacía. | Fila rechazada. |
| CP-036 | Con Acta válida | `CON_ACTA`, `RECIBIDO`, archivo existente. | Audiencia y Acta importadas. |
| CP-037 | Con Acta sin archivo | `CON_ACTA`, `RECIBIDO`, ruta vacía. | Fila rechazada. |
| CP-038 | Concluida sin desahogo | `CONCLUIDA`, fecha de desahogo vacía. | Fila rechazada. |
| CP-039 | Acta no compartida | `NO_COMPARTIDO` + `CLIENTE_NO_COMPARTE`, sin archivo. | Fila válida si las demás reglas se cumplen. |
| CP-040 | Gerencia distinta | Gerencia de audiencia diferente a expediente. | Fila rechazada. |

### 17.6 Términos

| ID | Escenario | Entrada/condición | Resultado esperado |
|---|---|---|---|
| CP-041 | Término abierto válido | Estado `PROYECTISTA`, responsable activo y compatible. | Término importado. |
| CP-042 | Término abierto sin responsable | Estado `REVISION`, responsable vacío. | Fila rechazada. |
| CP-043 | Presentado válido | Fecha de presentación y Acuse recibido existente. | Término y Acuse importados. |
| CP-044 | Presentado sin fecha | Estado `PRESENTADO`, fecha vacía. | Fila rechazada. |
| CP-045 | Presentado con Acuse no disponible | Estado `PRESENTADO`, fecha presente, `NO_DISPONIBLE` + motivo. | Fila válida sin crear archivo. |
| CP-046 | Concluido sin presentación justificado | Estado `CONCLUIDO`, fecha vacía, observaciones no vacías. | Fila válida. |
| CP-047 | Concluido sin presentación ni motivo | Estado `CONCLUIDO`, fecha y observaciones vacías. | Fila rechazada. |
| CP-048 | Estado abierto con presentación | `LIBERADO` con `fecha_presentacion`. | Fila rechazada para revisión previa. |
| CP-049 | Vencimiento anterior | Fecha de vencimiento pasada y estado abierto. | Se conserva fecha y estado; no se recalcula ni cambia. |
| CP-050 | Tipo incompatible | Tipo de Término de otra materia/gerencia. | Fila rechazada. |

### 17.7 Actualización, idempotencia y conciliación

| ID | Escenario | Entrada/condición | Resultado esperado |
|---|---|---|---|
| CP-051 | Alta nueva | `ALTA` sin equivalencia. | `IMPORTADO`; se crea una equivalencia. |
| CP-052 | Alta duplicada | `ALTA` con equivalencia existente. | `RECHAZADO`; no se crea duplicado. |
| CP-053 | Actualización existente | `ACTUALIZACION` con cambio válido. | `ACTUALIZADO`; mismo ID destino. |
| CP-054 | Actualización inexistente | `ACTUALIZACION` sin equivalencia. | `RECHAZADO`. |
| CP-055 | Actualización sin cambios | Valores idénticos al destino. | `OMITIDO_JUSTIFICADO`; mismo ID. |
| CP-056 | Reproceso del lote | Misma clave y mismo contenido. | Se recuperan resultados; no hay duplicados. |
| CP-057 | Clave de lote reutilizada | Misma clave con contenido diferente. | Entrega rechazada antes de persistir. |
| CP-058 | Archivo declarado inexistente | Acta/Acuse `RECIBIDO` cuya ruta no existe. | Fila rechazada y ningún documento ficticio. |
| CP-059 | Conciliación exacta | 100 recibidos: 70 importados, 10 actualizados, 5 omitidos, 15 rechazados. | Lote conciliado porque `100=70+10+5+15`. |
| CP-060 | Conciliación incorrecta | Total de resultados distinto de recibidos. | Lote no puede cerrarse ni abrir operación. |

## 18. Checklist funcional

- [ ] Los cinco archivos corresponden a la versión `2.0-MV`.
- [ ] Los catálogos fueron aprobados y publicados antes de la captura operativa.
- [ ] La fuente y fecha de corte están definidas.
- [ ] Cada registro tiene una clave de origen estable.
- [ ] Los usuarios migrables tienen rol válido y los roles excluidos no aparecen.
- [ ] Los usuarios históricos sin acceso están marcados `activo=false`.
- [ ] Los expedientes conservan número oficial, descripción y partes sin truncamiento.
- [ ] Cada expediente tiene materia, gerencia, etapa y Tipo de Expediente válidos.
- [ ] Los responsables conocidos se referencian por clave de origen.
- [ ] Cada Audiencia está vinculada al expediente correcto.
- [ ] Cada Audiencia conserva fecha, hora y estado explícitos.
- [ ] Cada Término está vinculado al expediente correcto.
- [ ] Cada Término conserva el vencimiento histórico y el Tipo de Término.
- [ ] Los términos abiertos tienen responsable vigente.
- [ ] Las Actas y Acuses usan estados y motivos aprobados.
- [ ] Ningún faltante documental se representa mediante un archivo ficticio.
- [ ] Las excepciones documentales fueron conciliadas.
- [ ] Los homónimos de número de expediente fueron revisados sin fusión automática.
- [ ] Las notificaciones se habilitarán sólo después de verificar responsables activos.

## 19. Checklist técnico para Backend

- [ ] Reconocer exclusivamente el contrato `2.0-MV`.
- [ ] Leer cada hoja por nombre y verificar el orden establecido.
- [ ] Validar encabezados exactos y rechazar columnas adicionales.
- [ ] Validar una sola fila en `CONTROL`.
- [ ] Comparar los metadatos de `CONTROL` de los cinco libros.
- [ ] Leer códigos y claves como texto, preservando ceros a la izquierda.
- [ ] Leer fechas y horas como valores reales de Excel.
- [ ] Aplicar límites de longitud antes de persistir y nunca truncar.
- [ ] Aplicar listas controladas con coincidencia exacta.
- [ ] Resolver catálogos por código, sin comparación aproximada de nombres.
- [ ] Resolver Usuarios, Expedientes, Audiencias y Términos mediante equivalencias de origen.
- [ ] Aplicar unicidad a `(fuente_codigo, entidad, clave_origen)`.
- [ ] Implementar `ALTA` y `ACTUALIZACION` conforme a las reglas del apartado 13.
- [ ] Garantizar que una repetición no cree duplicados.
- [ ] Registrar estado y errores del lote.
- [ ] Validar todo el libro antes de guardar datos operativos.
- [ ] Implementar todas las validaciones cruzadas VC-01 a VC-30.
- [ ] Persistir la FK del Tipo de Término.
- [ ] Conservar fechas y estados sin inferir ni recalcular.
- [ ] Verificar existencia y recuperación de Actas y Acuses antes de marcarlos recibidos.
- [ ] No usar fecha actual, `00:00`, primera materia, primera prioridad ni valores por defecto para corregir faltantes.
- [ ] Generar resultado, causa e ID destino por fila.
- [ ] Calcular y verificar la ecuación de conciliación por hoja y lote.
- [ ] Impedir cierre/apertura operativa cuando la conciliación no cuadre.
- [ ] Mantener el mismo resultado de interpretación entre prevalidación y carga.

## 20. Checklist técnico para Excel

- [ ] Generar los cinco libros en formato `.xlsx` con sus nombres exactos.
- [ ] Crear las hojas con nombres y orden exactos.
- [ ] Colocar encabezados en fila 1 y datos desde fila 2.
- [ ] Crear tablas ampliables en todas las hojas importables.
- [ ] Formatear códigos, claves, teléfonos, número de empleado y rutas como texto.
- [ ] Formatear fechas como `aaaa-mm-dd` y horas como `hh:mm`.
- [ ] Configurar listas desplegables para operaciones, booleanos, roles, etapas, estados, motivos y tipos de días.
- [ ] Vincular listas de códigos a `CATALOGOS_REFERENCIA`.
- [ ] Proteger `CATALOGOS_REFERENCIA` contra edición.
- [ ] Incluir instrucciones de obligatoriedad condicional en `INSTRUCCIONES`.
- [ ] Incluir definición, tipo y longitud de cada columna en `DICCIONARIO`.
- [ ] Impedir valores fuera de lista mediante validación de datos.
- [ ] Configurar validaciones de longitud compatibles con los máximos definidos.
- [ ] Configurar validación de enteros para días y semáforos.
- [ ] Configurar validación de fechas y horas reales.
- [ ] No incluir columnas ocultas de datos fuera del contrato.
- [ ] No incluir fórmulas que sustituyan valores faltantes con fechas, horas, catálogos o responsables.
- [ ] Verificar que agregar filas a una tabla conserve listas, formatos y validaciones.
- [ ] Probar apertura y guardado en la versión de Excel objetivo sin reparar el archivo.

## 21. Checklist de salida a producción

- [ ] Las cinco plantillas definitivas están disponibles para descarga.
- [ ] La versión visible en todas las plantillas es `2.0-MV`.
- [ ] La versión de catálogos de producción está aprobada, publicada y bloqueada.
- [ ] La tabla/restricción de equivalencias está desplegada y probada.
- [ ] El registro de estados y errores de lote está desplegado.
- [ ] La tabla y FK de Tipos de Término están confirmadas y operativas.
- [ ] La UI persiste correctamente el Tipo de Término.
- [ ] La UI muestra la prevalidación y la conciliación.
- [ ] El importador superó CP-001 a CP-060.
- [ ] Se ejecutó una carga completa en ambiente previo con los cinco libros.
- [ ] Se comprobó la recuperación de Actas y Acuses marcados `RECIBIDO`.
- [ ] Se comprobó que repetir el lote no genera duplicados.
- [ ] Se comprobó que una actualización conserva el ID destino.
- [ ] Se comprobó que ningún error produce truncamiento ni valores inventados.
- [ ] Se revisaron los conflictos por números de expediente homónimos.
- [ ] La ecuación de conciliación cuadra por hoja y por lote.
- [ ] Los registros rechazados tienen causa accionable y referencia de fila.
- [ ] Los responsables de eventos pendientes y términos abiertos están vigentes.
- [ ] Están aprobados el orden de carga, el corte y las excepciones documentales.
- [ ] Existe respaldo previo a la carga productiva.
- [ ] La activación de notificaciones se ejecutará después de la conciliación final.

## 22. Criterio de terminación de la implementación

La construcción queda aceptada cuando:

1. Las cinco plantillas coinciden exactamente con esta especificación.
2. El importador prevalida sin persistir y carga sólo después de una prevalidación válida.
3. Todas las claves y códigos se resuelven por equivalencia o catálogo exacto.
4. Las altas, actualizaciones y repeticiones cumplen las reglas de identidad e idempotencia.
5. Audiencias y Términos quedan vinculados al expediente correcto.
6. El Tipo de Expediente queda disponible en alta/edición y el Tipo de Término se persiste.
7. Las fechas, horas, estados, materias, prioridades y responsables no se inventan.
8. Cada Acta y Acuse declarado `RECIBIDO` existe y puede recuperarse.
9. El reporte por fila y la ecuación de conciliación son correctos.
10. Los 60 casos de prueba y los cuatro checklists quedan aprobados.
