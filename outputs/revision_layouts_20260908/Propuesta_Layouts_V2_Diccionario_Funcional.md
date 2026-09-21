# Propuesta de Layouts V2 — Diccionario de datos funcional

Fecha: 8 de septiembre de 2026  
Versión del contrato: 2.0

## 1. Objetivo y condición de uso

Esta propuesta define el contrato funcional de cinco archivos Excel para migrar información histórica a SIAJ. “Listo para producción” significa que los campos, dependencias, obligatoriedad, catálogos y reglas están definidos sin ambigüedades. La liberación efectiva requiere implementar y probar el importador V2, las equivalencias de claves, los cambios de modelo indicados y la conciliación del lote. Los Excel por sí solos no garantizan una carga correcta.

Archivos V2:

1. `Layout_Catalogos_V2.xlsx`
2. `Layout_Usuarios_V2.xlsx`
3. `Layout_Expedientes_V2.xlsx`
4. `Layout_Audiencias_V2.xlsx`
5. `Layout_Terminos_V2.xlsx`

Orden de carga: **Catálogos → Usuarios → Expedientes → Audiencias y Términos**. Audiencias y Términos pueden cargarse en paralelo después de Expedientes.

## 2. Reglas transversales del contrato V2

### 2.1 Estructura común

Todos los libros tendrán estas hojas:

| Hoja | Función | Importable |
|---|---|---|
| CONTROL | Identifica versión, fuente, lote, corte y responsables. | Sí, como cabecera del lote. |
| INSTRUCCIONES | Secuencia, formatos y reglas condicionales. | No. |
| DICCIONARIO | Definición de campos del libro. | No. |
| CATALOGOS_REFERENCIA | Copia bloqueada del catálogo maestro aplicable al libro. | No. |
| VALIDACION | Resultado de revisión previa: fila, campo, código y mensaje. | No; la genera el validador/importador. |
| Hojas de datos | Una fila por entidad o relación. | Sí. |

El importador debe localizar las hojas por nombre y validar `layout_version`; nunca debe depender de la posición de la hoja. Las hojas de datos deben ser Tablas de Excel ampliables, sin límite funcional de 200 filas.

### 2.2 CONTROL — diccionario común

| Campo | Descripción | Tipo / longitud | Obligatorio | Validación / regla | Responsable |
|---|---|---:|---|---|---|
| layout_version | Versión exacta del contrato. | Texto, 10 | Sí | Valor fijo `2.0`; rechazar otra versión. | Implementación |
| fuente_codigo | Identificador estable del sistema o despacho de origen. | Texto, 50 | Sí | Catálogo FUENTES; no cambia entre lotes de la misma fuente. | Implementación + cliente |
| lote_clave | Identificador único de la entrega. | Texto, 100 | Sí | Único; patrón `FUENTE-AAAAMMDD-SECUENCIA`. | Implementación |
| catalogo_version | Versión del catálogo maestro usado para llenar el libro. | Texto, 50 | Sí | Debe coincidir con una versión publicada y vigente para el lote. | Implementación |
| fecha_corte | Fecha hasta la cual los datos históricos son válidos. | Fecha | Sí | Fecha real `AAAA-MM-DD`; no puede ser posterior a la entrega sin justificación. | Cliente |
| responsable_entrega | Persona que responde por el contenido. | Texto, 200 | Sí | No vacío. | Cliente |
| email_responsable | Medio de contacto para aclaraciones. | Texto, 100 | Sí | Formato de correo; no se usa como llave de datos. | Cliente |
| gerencia_ambito_codigo | Gerencia a la que se limita el archivo. | Texto, 100 | No | Si se captura, todas las filas operativas deben corresponder a ella. Puede quedar vacío para libros multigerencia autorizados. | Cliente |

### 2.3 Convenciones de captura y validación

- Los identificadores (`*_codigo`, `*_clave_origen`, números de expediente y números de empleado) se capturan como texto, aunque contengan sólo dígitos. Se conservan ceros, diagonales y guiones.
- Las claves de origen cumplen `^[A-Z0-9][A-Z0-9._-]{2,99}$`, sin espacios, normalizadas a mayúsculas. La unicidad se valida sin distinguir mayúsculas.
- Fechas son valores de fecha reales, mostrados como `AAAA-MM-DD`. Horas son valores reales de 24 horas, mostrados como `HH:mm`.
- Booleanos aceptan exclusivamente `true` y `false`.
- Los códigos deben existir, estar vigentes para la `fecha_corte` y pertenecer al ámbito correcto. No se admite resolver por coincidencia parcial de nombre.
- Vacío significa ausencia. No se aceptan `N/A`, `NULL`, `S/D`, guiones ni fórmulas como sustitutos.
- Se rechazan fórmulas, macros, celdas combinadas, filas ocultas dentro de datos, subtotales y encabezados modificados.
- El backend valida tipos, longitudes, obligatoriedad, relaciones, duplicados y reglas condicionales antes de persistir. No trunca textos ni reemplaza fechas/horas inválidas.
- Cada fila recibe un resultado: `IMPORTADO`, `RECHAZADO`, `OMITIDO_DUPLICADO` o `ACTUALIZADO`, con causa.
- La identidad de migración es `(fuente_codigo, entidad, clave_origen)`. El lote no forma parte de la identidad; repetir un lote no crea otro registro.
- Los ID de SIAJ son generados por el sistema. El resultado de carga debe devolver `clave_origen → id_destino`.

### 2.4 Catálogos de sistema y catálogos de negocio

Los siguientes valores son vocabularios cerrados del sistema y deben aparecer bloqueados en `CATALOGOS_REFERENCIA`: booleanos, roles permitidos en migración, permisos, etapas/estados técnicos, modalidad, disponibilidad documental, tipo de documento y motivo de cierre.

Los catálogos de negocio se administran en `Layout_Catalogos_V2.xlsx`. La pestaña de referencia de los otros cuatro libros se genera desde una versión publicada; el usuario no la edita.

---

## 3. Layout_Catalogos_V2.xlsx

### 3.1 Resumen de cambios

**Se mantienen:** gerencias, materias, prioridades, tipos de expediente, órganos, tipos de audiencia y sedes, con códigos estables y vigencia.

**Se eliminan o sustituyen:**

- La hoja genérica editable `CATALOGOS` repetida. Se sustituye por `CATALOGOS_REFERENCIA`, bloqueada y generada.
- `ORGANOS.tipo` como texto libre. Se sustituye por `tipo_organo_codigo`.
- `PRIORIDADES.color` capturado por el cliente. El color técnico lo administra el sistema; el layout recibe `orden_visual`.
- Ejemplos ficticios dentro de listas productivas.

**Se agregan:** `TIPOS_ORGANO`, `TIPOS_TERMINO`, reglas de tiempo en tipos de audiencia/término y vigencia por fechas. Los catálogos de disponibilidad documental y motivos de cierre son catálogos de sistema bloqueados; no requieren hojas editables del cliente.

### 3.2 GERENCIAS

| Campo | Estado V2 | Descripción | Tipo / longitud | Obligatorio | Validación | Dependencia / destino |
|---|---|---|---:|---|---|---|
| gerencia_codigo | Mantener | Código estable de la unidad responsable. | Texto, 100 | Sí | Único global; no cambia tras ser referenciado. | Equivalencia a `gerencias.id`. |
| nombre | Mantener | Nombre visible oficial. | Texto, 200 | Sí | Único según regla aprobada; sin espacios extremos. | `gerencias.nombre`. |
| descripcion | Mantener | Alcance funcional. | Texto, 32,767 | No | Texto sin fórmulas. | `gerencias.descripcion`. |
| activo | Mantener | Vigencia lógica. | Booleano | Sí | `true/false`. | `gerencias.activo`. |

### 3.3 MATERIAS

| Campo | Estado V2 | Descripción | Tipo / longitud | Obligatorio | Validación | Dependencia / destino |
|---|---|---|---:|---|---|---|
| materia_codigo | Mantener | Código estable de materia. | Texto, 100 | Sí | Único por ámbito aprobado; no resolver por nombre. | Equivalencia a `materias.id`. |
| gerencia_codigo | Mantener | Gerencia propietaria. | Texto, 100 | Sí | Debe existir en GERENCIAS y estar activa para nuevas asignaciones. | `materias.gerencia_id`. |
| nombre | Mantener | Nombre visible. | Texto, 100 | Sí | Único por gerencia; respetar restricción real del destino. | `materias.nombre`. |
| descripcion | Mantener | Descripción funcional. | Texto, 32,767 | No | Texto libre. | `materias.descripcion`. |
| activo | Mantener | Vigencia lógica. | Booleano | Sí | `true/false`. | `materias.activo`. |

### 3.4 SEDES

Definición V2: territorio o ubicación jurisdiccional utilizada para clasificar órganos. No representa la oficina del usuario ni la sala física de una audiencia.

| Campo | Estado V2 | Descripción | Tipo / longitud | Obligatorio | Validación | Dependencia / destino |
|---|---|---|---:|---|---|---|
| sede_codigo | Mantener | Código estable de sede. | Texto, 100 | Sí | Único global. | Equivalencia a `cat_estados.id`. |
| nombre | Mantener | Nombre visible. | Texto, 100 | Sí | Único; longitud alineada al destino de órgano. | `cat_estados.nombre`. |
| gerencia_codigo | Mantener con regla | Gerencia propietaria; vacío significa global. | Texto, 100 | No | Si se captura, debe existir. La propiedad no duplica una sede global. | `cat_estados.gerencia_id`. |
| activo | Mantener | Vigencia lógica. | Booleano | Sí | `true/false`. | `cat_estados.activo`. |

### 3.5 TIPOS_ORGANO — nueva

| Campo | Estado V2 | Descripción | Tipo / longitud | Obligatorio | Validación | Dependencia / destino |
|---|---|---|---:|---|---|---|
| tipo_organo_codigo | Nuevo | Código de clase de autoridad. | Texto, 50 | Sí | Único; ejemplo conceptual: JUZGADO, TRIBUNAL, JUNTA. Valores reales los aprueba el cliente. | Nueva equivalencia; FK si se normaliza. |
| nombre | Nuevo | Nombre visible. | Texto, 100 | Sí | Único. | Nuevo catálogo o mapeo a texto de órgano. |
| descripcion | Nuevo | Alcance del tipo. | Texto, 500 | No | Texto libre. | Catálogo. |
| activo | Nuevo | Vigencia lógica. | Booleano | Sí | `true/false`. | Catálogo. |

### 3.6 ORGANOS

| Campo | Estado V2 | Descripción | Tipo / longitud | Obligatorio | Validación | Dependencia / destino |
|---|---|---|---:|---|---|---|
| organo_codigo | Mantener | Código estable de la autoridad concreta. | Texto, 100 | Sí | Único global; no usar el nombre como código. | Equivalencia a `organos_jurisdiccionales.id`. |
| gerencia_codigo | Mantener con regla | Gerencia que lo administra; vacío si es compartido. | Texto, 100 | No | Debe existir si se captura. | `organo.gerencia_id`; relación N:M si un órgano es compartido y se requiere alcance. |
| nombre | Mantener | Nombre oficial de la autoridad. | Texto, 300 | Sí | No vacío; verificar duplicados por tipo/sede. | `organo.nombre`. |
| sede_codigo | Mantener | Sede jurisdiccional. | Texto, 100 | No | Debe existir en SEDES; compatible con ámbito. | Actualmente requiere resolución a texto/ID según modelo final. |
| tipo | Eliminar | Texto libre ambiguo. | — | — | Sustituido por `tipo_organo_codigo`. | — |
| tipo_organo_codigo | Nuevo | Tipo controlado del órgano. | Texto, 50 | Sí | Debe existir y estar activo en TIPOS_ORGANO. | FK nueva o conversión controlada a `organo.tipo`. |
| activo | Mantener | Vigencia lógica. | Booleano | Sí | `true/false`. | `organo.activo`. |

### 3.7 PRIORIDADES

| Campo | Estado V2 | Descripción | Tipo / longitud | Obligatorio | Validación | Dependencia / destino |
|---|---|---|---:|---|---|---|
| prioridad_codigo | Mantener | Código estable. | Texto, 50 | Sí | Único. Para V2 inicial: ALTA, MEDIA, BAJA salvo aprobación distinta. | Equivalencia a `cat_prioridad.id`. |
| nombre | Mantener | Etiqueta visible. | Texto, 50 | Sí | Única. | `cat_prioridad.nombre`. |
| color | Eliminar de captura | Clase CSS/técnica. | — | — | La define UI/backend por código o configuración administrativa. | `cat_prioridad.color` lo completa implementación. |
| orden_visual | Nuevo | Orden de presentación. | Entero | Sí | Entero positivo, único; ALTA antes de MEDIA y BAJA. | Requiere soporte UI/BD o regla del backend. |
| activo | Mantener | Vigencia lógica. | Booleano | Sí | `true/false`. | `cat_prioridad.activo`. |

### 3.8 TIPOS_EXPEDIENTE

| Campo | Estado V2 | Descripción | Tipo / longitud | Obligatorio | Validación | Dependencia / destino |
|---|---|---|---:|---|---|---|
| tipo_expediente_codigo | Mantener | Código estable de clasificación. | Texto, 100 | Sí | Único global según restricción actual. | Equivalencia a `cat_tipo_expediente.id`. |
| nombre | Mantener | Nombre visible. | Texto, 100 | Sí | Único global según modelo actual. | `cat_tipo_expediente.nombre`. |
| descripcion | Mantener | Uso funcional del tipo. | Texto, 32,767 | No | Debe permitir distinguirlo de materia y etapa. | `descripcion`. |
| gerencia_codigo | Mantener con regla | Ámbito; vacío significa global. | Texto, 100 | No | Debe existir si se captura. | `gerencia_id`. |
| activo | Mantener | Vigencia lógica. | Booleano | Sí | `true/false`. | `activo`. |

### 3.9 TIPOS_AUDIENCIA

| Campo | Estado V2 | Descripción | Tipo / longitud | Obligatorio | Validación | Dependencia / destino |
|---|---|---|---:|---|---|---|
| tipo_audiencia_codigo | Mantener | Código estable. | Texto, 100 | Sí | Único global; si negocio exige repetición por gerencia, ajustar BD antes. | Equivalencia a `tipos_audiencia.id`. |
| gerencia_codigo | Mantener | Ámbito del tipo; vacío si global. | Texto, 100 | No | Debe existir si se captura. | `gerencia_id`. |
| nombre | Mantener | Nombre visible. | Texto, 100 | Sí | No vacío. | `nombre`. |
| descripcion | Mantener | Uso funcional. | Texto, 32,767 | No | Texto libre. | `descripcion`. |
| dias_notificacion_previa | Nuevo | Anticipación ordinaria del aviso. | Entero | Sí | `>=0`. | Campo existente en JPA; verificar BD. |
| semaforo_rojo_dias | Nuevo | Días restantes para estado crítico. | Entero | Sí | `>=0`. | Campo existente en JPA; backend debe aplicarlo. |
| semaforo_amarillo_dias | Nuevo | Días restantes para advertencia. | Entero | Sí | `> semaforo_rojo_dias`. | Campo existente en JPA; backend debe aplicarlo. |
| activo | Mantener | Vigencia lógica. | Booleano | Sí | `true/false`. | `activo`. |

### 3.10 TIPOS_TERMINO — nueva

| Campo | Estado V2 | Descripción | Tipo / longitud | Obligatorio | Validación | Dependencia / destino |
|---|---|---|---:|---|---|---|
| tipo_termino_codigo | Nuevo | Código estable del tipo de término. | Texto, 100 | Sí | Único global según modelo actual. | Equivalencia a `tipos_termino.id`. |
| nombre | Nuevo | Nombre visible. | Texto, 100 | Sí | Único; distinto de la descripción particular del asunto. | `tipos_termino.nombre`. |
| descripcion | Nuevo | Alcance y supuesto de uso. | Texto, 32,767 | No | Texto libre. | `descripcion`. |
| gerencia_codigo | Nuevo | Ámbito; vacío si global. | Texto, 100 | No | Debe existir si se captura. | `gerencia_id`. |
| materia_codigo | Nuevo | Materia a la que aplica; vacío si transversal. | Texto, 100 | No | Debe existir; compatible con gerencia. | `materia_id`. |
| metodo_vencimiento_codigo | Nuevo | Forma de determinar vencimiento. | Texto, 30 | Sí | `DIAS_DESDE_INICIO` o `FECHA_CAPTURADA`. Sustituye “fijo/dinámico” ambiguo. | Requiere backend/UI. |
| dias_plazo | Nuevo | Número ordinario de días del plazo. | Entero | Condicional | Obligatorio y `>0` si método = DIAS_DESDE_INICIO; vacío si FECHA_CAPTURADA. | `dias_predeterminados`. |
| tipo_dias | Nuevo | Naturaleza de los días. | Texto, 20 | Condicional | HABILES o NATURALES; obligatorio con DIAS_DESDE_INICIO. | `tipo_dias`. |
| calendario_codigo | Nuevo | Calendario de inhábiles aplicable. | Texto, 100 | Condicional | Obligatorio si tipo_dias=HABILES; debe existir en configuración de calendarios. | Nuevo catálogo/BD si SIAJ calculará fechas. |
| semaforo_rojo_dias | Nuevo | Días restantes para crítico. | Entero | Sí | `>=0`. | Campo JPA existente. |
| semaforo_amarillo_dias | Nuevo | Días restantes para advertencia. | Entero | Sí | `> semaforo_rojo_dias`. | Campo JPA existente. |
| activo | Nuevo | Vigencia lógica. | Booleano | Sí | `true/false`. | `activo`. |

### 3.11 Catálogos requeridos por este libro

- Catálogos maestros: GERENCIAS, MATERIAS, SEDES, TIPOS_ORGANO, ORGANOS, PRIORIDADES, TIPOS_EXPEDIENTE, TIPOS_AUDIENCIA, TIPOS_TERMINO.
- Catálogos de sistema de referencia: BOOLEANOS, METODOS_VENCIMIENTO, TIPOS_DIAS y CALENDARIOS si se habilita cálculo de días hábiles.

### 3.12 Dependencias

- GERENCIAS antes de MATERIAS y ámbitos de otros catálogos.
- SEDES y TIPOS_ORGANO antes de ORGANOS.
- MATERIAS antes de TIPOS_TERMINO con ámbito de materia.
- El libro debe aprobarse y publicarse antes de generar los otros cuatro.

---

## 4. Layout_Usuarios_V2.xlsx

### 4.1 Resumen de cambios

**Se mantienen:** nombre, correo, username, número de empleado, rol, puesto, teléfonos, oficina y estado activo.

**Se eliminan o sustituyen:**

- `gerencia_codigo` de una sola gerencia se sustituye por la hoja `USUARIO_GERENCIAS`.
- `materias_codigos` se sustituye por `USUARIO_MATERIAS`, una relación por fila.
- `permisos_adicionales` se sustituye por `USUARIO_PERMISOS`, una relación por fila.
- Las referencias por correo en otros libros se sustituyen por `usuario_clave_origen`.
- ADMIN se elimina de las opciones del layout operativo. Su aprovisionamiento es técnico. DIRECTOR_PRINCIPAL sólo aparece en una plantilla administrativa restringida si el cliente confirma ese rol.

**Se agregan:** clave de origen de usuario, tipo de registro operativo/histórico y relación multigerencia con gerencia principal explícita.

### 4.2 USUARIOS

| Campo | Estado V2 | Descripción | Tipo / longitud | Obligatorio | Validación | Dependencia / destino |
|---|---|---|---:|---|---|---|
| usuario_clave_origen | Nuevo | Identificador estable de la persona/cuenta en la fuente. | Texto, 100 | Sí | Único por fuente. Se usa en todos los layouts. | Equivalencia a `usuarios.id`. |
| nombre_completo | Mantener | Nombre visible. | Texto, 200 | Sí | No vacío. | `usuarios.nombre_completo`. |
| email | Mantener | Correo institucional/contacto. | Texto, 100 | Condicional | Obligatorio si se habilitarán notificaciones o Microsoft login; único si existe. No es llave. | `usuarios.email`. |
| username | Mantener | Usuario local. | Texto, 80 | Condicional | Obligatorio para cuenta operativa sin login externo; único sin distinguir mayúsculas. | `usuarios.username`. |
| numero_empleado | Mantener | Identificador laboral. | Texto, 40 | No | Único si existe; preservar ceros. | `usuarios.numero_empleado`. |
| rol | Mantener con regla | Rol base. | Texto, 50 | Sí | Catálogo ROLES_MIGRABLES. ADMIN excluido. REFERENCIA_HISTORICA exige el rol sin privilegios del mismo nombre. | `usuarios.rol`; requiere agregar el rol REFERENCIA_HISTORICA al backend. |
| puesto | Mantener | Puesto institucional. | Texto, 160 | No | Texto libre. | `usuarios.puesto`. |
| telefono_institucional | Mantener | Teléfono institucional. | Texto, 120 | No | Texto; conservar extensiones. | Campo existente. |
| telefono_movil | Mantener | Teléfono móvil. | Texto, 80 | No | Texto. | Campo existente. |
| ubicacion_oficina | Mantener | Oficina física del usuario. | Texto, 180 | No | No confundir con sede jurisdiccional. | Campo existente. |
| tipo_registro_usuario | Nuevo | Finalidad del registro. | Texto, 20 | Sí | OPERATIVO o REFERENCIA_HISTORICA. | Requiere regla de importación y, si debe persistirse, columna BD. |
| activo | Mantener con regla | Habilita la cuenta. | Booleano | Sí | REFERENCIA_HISTORICA exige false. OPERATIVO puede ser true/false. | `usuarios.activo`. |
| login_microsoft_habilitado | Nuevo | Indica si usará identidad Microsoft. | Booleano | Sí | true exige email y datos de vinculación posterior; no se importan tokens. | `microsoft_login_enabled`. |

La contraseña no se captura. El backend crea un secreto seguro temporal o inicia el proceso de activación; nunca se distribuye en Excel.

### 4.3 USUARIO_GERENCIAS

| Campo | Estado V2 | Descripción | Tipo / longitud | Obligatorio | Validación | Dependencia / destino |
|---|---|---|---:|---|---|---|
| usuario_clave_origen | Nuevo | Usuario relacionado. | Texto, 100 | Sí | Debe existir en USUARIOS. | `usuario_gerencias.usuario_id`. |
| gerencia_codigo | Sustituye campo único | Gerencia asignada. | Texto, 100 | Sí | Debe existir y estar activa para operación nueva. | `usuario_gerencias.gerencia_id`. |
| es_principal | Nuevo | Gerencia usada como contexto predeterminado. | Booleano | Sí | Exactamente una true por usuario operativo con gerencias. | Requiere relación con atributo o campo principal explícito en backend/BD. |

### 4.4 USUARIO_MATERIAS

| Campo | Estado V2 | Descripción | Tipo / longitud | Obligatorio | Validación | Dependencia / destino |
|---|---|---|---:|---|---|---|
| usuario_clave_origen | Sustituye lista | Usuario relacionado. | Texto, 100 | Sí | Debe existir en USUARIOS. | `usuario_materias.usuario_id`. |
| materia_codigo | Sustituye lista | Materia autorizada. | Texto, 100 | Sí | Debe existir y pertenecer a una gerencia asignada al usuario. | `usuario_materias.materia_id`. |

No se permiten pares repetidos.

### 4.5 USUARIO_PERMISOS

| Campo | Estado V2 | Descripción | Tipo / longitud | Obligatorio | Validación | Dependencia / destino |
|---|---|---|---:|---|---|---|
| usuario_clave_origen | Sustituye lista | Usuario relacionado. | Texto, 100 | Sí | Debe existir en USUARIOS. | `usuario_permisos_adicionales.usuario_id`. |
| permiso_codigo | Sustituye lista | Excepción adicional al rol. | Texto, 80 | Sí | Catálogo PERMISOS; no repetir permisos heredados del rol. Actor importador debe poder otorgarlo. | `permiso`. |
| justificacion | Nuevo | Razón de la excepción. | Texto, 500 | Sí | No vacío; queda en reporte de aprobación. | Auditoría de migración; BD si se requiere persistencia. |

### 4.6 Catálogos requeridos

- GERENCIAS, MATERIAS.
- ROLES_MIGRABLES: DIRECTOR_PRINCIPAL sólo si se autoriza, DIRECTOR, SUBDIRECTOR, GERENTE, JEFE_DEPTO y ABOGADO. ADMIN queda fuera.
- PERMISOS, con título comprensible, descripción y ámbito.
- TIPOS_REGISTRO_USUARIO: OPERATIVO, REFERENCIA_HISTORICA.
- BOOLEANOS.

### 4.7 Reglas de obligatoriedad y dependencias

- Un usuario OPERATIVO con rol GERENTE, JEFE_DEPTO o ABOGADO debe tener al menos una gerencia y exactamente una principal.
- Cada materia debe pertenecer a alguna gerencia del usuario.
- REFERENCIA_HISTORICA debe quedar inactivo, usar el rol sin privilegios REFERENCIA_HISTORICA y no tener permisos adicionales.
- Debe existir al menos email o username para una cuenta OPERATIVA. El backend puede exigir datos adicionales según el método de autenticación.
- Depende de Layout_Catalogos_V2. Debe cargarse antes de Expedientes, Audiencias y Términos.

---

## 5. Layout_Expedientes_V2.xlsx

### 5.1 Resumen de cambios

**Se mantienen:** clave de origen, gerencia, número oficial, materia, descripción, etapa, tipo, prioridad, órgano y abogado responsable.

**Se eliminan o sustituyen:**

- `abogado_responsable_email` se sustituye por `abogado_responsable_clave_origen`.
- `partes` se sustituye por la hoja relacional `EXPEDIENTE_PARTES`; se conserva el texto original en `partes_texto_origen` sólo para trazabilidad.
- `sede_codigo` deja de ser obligatorio junto con órgano. Se deriva de ORGANOS; sólo se captura como sede histórica cuando no hay órgano homologable.

**Se agregan:** operación del registro para reintentos/actualizaciones, partes estructuradas y origen del responsable.

### 5.2 EXPEDIENTES

| Campo | Estado V2 | Descripción | Tipo / longitud | Obligatorio | Validación | Dependencia / destino |
|---|---|---|---:|---|---|---|
| clave_origen | Mantener | Identidad estable del expediente en la fuente. | Texto, 100 | Sí | Única por fuente y entidad. | Equivalencia a UUID de `expedientes.id`. |
| operacion | Nuevo | Intención de la fila. | Texto, 15 | Sí | ALTA o ACTUALIZACION. Actualización exige equivalencia previa. | Control del importador. |
| gerencia_codigo | Mantener | Gerencia responsable. | Texto, 100 | Sí | Debe existir; compatible con materia y responsable. | `gerencia_id`. |
| numero_expediente | Mantener | Número oficial. | Texto, 50 | Sí | No vacío; no es llave única. `S/N` exige partes o referencia adicional suficiente. | `expedientes.numero`. |
| materia_codigo | Mantener | Materia jurídica. | Texto, 100 | Sí | Debe existir y pertenecer a la gerencia. | `materia_id`. |
| descripcion | Mantener | Descripción del asunto. | Texto, 32,767 | Sí | No vacía; no truncar. | `descripcion`. |
| etapa_procesal | Mantener | Etapa vigente a fecha de corte. | Texto, 30 | Sí | Catálogo ETAPAS_PROCESALES. | `etapa_procesal`. |
| tipo_expediente_codigo | Mantener y hacer obligatorio | Clasificación operativa. | Texto, 100 | Sí | Debe existir, estar activa y aplicar a la gerencia. Para históricos sin clasificación usar código aprobado SIN_CLASIFICAR, no texto libre. | `tipo_expediente_id`. |
| prioridad_codigo | Mantener | Prioridad propia del expediente. | Texto, 50 | No | Catálogo PRIORIDADES. No se hereda automáticamente a hijos. | `prioridad_id`. |
| organo_codigo | Mantener | Autoridad asociada. | Texto, 100 | Condicional | Requerido si la fuente conoce el órgano; debe existir. Si no se homologa, documentar excepción, no crear coincidencia parcial. | `organo_jurisdiccional_id`. |
| sede_codigo | Sustituir | Evitar duplicar la sede derivada del órgano. | — | — | Sustituido por `sede_historica_codigo` sólo para excepción. | — |
| sede_historica_codigo | Nuevo condicional | Sede conocida cuando no existe órgano homologable. | Texto, 100 | Condicional | Sólo se permite si organo_codigo está vacío; debe existir en SEDES. | `expedientes.sede` o snapshot; backend debe definir resolución. |
| partes | Eliminar | Texto libre mezclado. | — | — | Sustituido por EXPEDIENTE_PARTES. | — |
| partes_texto_origen | Nuevo | Texto original sin transformar. | Texto, 32,767 | No | No se usa para crear relaciones automáticas sin revisión. | `expedientes.partes` mientras se implementa estructura o campo de trazabilidad. |
| abogado_responsable_email | Eliminar | Correo usado como llave. | — | — | Sustituido por clave de usuario. | — |
| abogado_responsable_clave_origen | Nuevo | Responsable actual al corte. | Texto, 100 | Condicional | Obligatorio para expedientes activos si el cliente conoce responsable; usuario OPERATIVO activo y con materia/gerencia. | `abogado_responsable_id`. |
| responsable_historico_texto | Nuevo | Nombre de responsable no localizable como usuario. | Texto, 200 | Condicional | Sólo si no hay clave de usuario; no concede acceso ni recibe notificaciones. | `abogado_responsable_nombre` o snapshot. |

### 5.3 EXPEDIENTE_PARTES

| Campo | Estado V2 | Descripción | Tipo / longitud | Obligatorio | Validación | Dependencia / destino |
|---|---|---|---:|---|---|---|
| expediente_clave_origen | Nuevo | Expediente relacionado. | Texto, 100 | Sí | Debe existir en EXPEDIENTES. | FK mediante equivalencia. |
| parte_clave_origen | Nuevo | Identificador estable de la parte dentro de la fuente. | Texto, 100 | Sí | Único por fuente; no usar sólo nombre. | Nueva entidad/equivalencia si se normaliza. |
| tipo_persona_codigo | Nuevo | Persona física, moral u otra categoría aprobada. | Texto, 30 | Sí | Catálogo TIPOS_PERSONA. | Nuevo modelo si se estructura. |
| nombre_razon_social | Nuevo | Nombre completo o razón social. | Texto, 300 | Sí | No vacío. | Nueva entidad o snapshot. |
| calidad_procesal_codigo | Nuevo | Rol en el expediente. | Texto, 50 | Sí | Catálogo por materia; admite ACTOR, DEMANDADO, TERCERO y valores aprobados. | Nueva relación expediente–parte. |
| identificador_externo | Nuevo | RFC, número institucional u otro identificador permitido. | Texto, 100 | No | No inventar; tratar conforme a reglas de datos aplicables. | Nueva entidad si se persiste. |

Si la normalización de partes no entra en V2 técnica, esta hoja puede validarse y conservarse en staging, y `partes_texto_origen` seguirá alimentando el campo actual. Esa reducción debe declararse; no se prometerán filtros por calidad procesal.

### 5.4 Catálogos requeridos

- GERENCIAS, MATERIAS, ETAPAS_PROCESALES, TIPOS_EXPEDIENTE, PRIORIDADES, ORGANOS, SEDES.
- TIPOS_PERSONA y CALIDADES_PROCESALES si se implementa EXPEDIENTE_PARTES.
- USUARIOS por `usuario_clave_origen`.
- OPERACIONES_CARGA: ALTA, ACTUALIZACION.

### 5.5 Reglas y dependencias

- Número oficial nunca sustituye clave_origen. La detección por número/partes sólo genera candidatos para revisión.
- Materia, órgano, tipo y responsable deben ser compatibles con gerencia.
- ACTUALIZACION no permite cambiar `clave_origen`; los cambios deben quedar auditados.
- Un expediente concluido puede carecer de responsable actual; uno activo sólo puede carecerlo mediante excepción explícita.
- Depende de Catálogos y Usuarios. Se carga antes de Audiencias y Términos.

---

## 6. Layout_Audiencias_V2.xlsx

### 6.1 Resumen de cambios

**Se mantienen:** clave de origen, expediente, tipo, fecha, hora, sala/lugar, modalidad virtual, URL, compareciente, delegado, fecha de desahogo y observaciones.

**Se eliminan o sustituyen:**

- `gerencia_codigo` se elimina porque la audiencia hereda ámbito del expediente; el backend lo valida.
- Correos de compareciente/delegado se sustituyen por claves de usuario.
- `acta_archivo` se mueve a `DOCUMENTOS_AUDIENCIA` para permitir cero, uno o varios documentos.
- `estatus_audiencia` se sustituye por `estado_audiencia_codigo` procesal; CON_ACTA deja de ser estado procesal.

**Se agregan:** operación de fila, órgano propio opcional, disponibilidad del acta, motivo de faltante y manifiesto documental.

### 6.2 AUDIENCIAS

| Campo | Estado V2 | Descripción | Tipo / longitud | Obligatorio | Validación | Dependencia / destino |
|---|---|---|---:|---|---|---|
| clave_origen | Mantener | Identidad estable de la audiencia en fuente. | Texto, 100 | Sí | Única por fuente y entidad. | Equivalencia a `audiencias.id`. |
| operacion | Nuevo | Intención de la fila. | Texto, 15 | Sí | ALTA o ACTUALIZACION. | Importador. |
| gerencia_codigo | Eliminar | Duplicaba ámbito del expediente. | — | — | El backend deriva y valida desde expediente. | — |
| expediente_clave_origen | Mantener | Expediente padre. | Texto, 100 | Sí | Debe existir en equivalencias del mismo origen o carga previa autorizada. | `expediente_id`. |
| tipo_audiencia_codigo | Mantener | Tipo de audiencia. | Texto, 100 | Sí | Debe existir, estar activo y ser compatible con gerencia. | `tipo_audiencia_id`. |
| fecha_audiencia | Mantener | Fecha programada/histórica. | Fecha | Sí | Fecha real; no concluir por antigüedad. | `fecha_audiencia`. |
| hora_audiencia | Mantener | Hora programada/histórica. | Hora | Sí | Hora real `HH:mm`; no sustituir por 00:00. Excepciones históricas requieren proceso aparte. | `hora_audiencia`. |
| estado_audiencia_codigo | Sustituye estatus | Estado procesal. | Texto, 30 | Sí | PROGRAMADA, CONCLUIDA, CANCELADA o REPROGRAMADA, según catálogo aprobado. Requiere mapeo del enum actual. | Backend/BD/UI. |
| sala_lugar | Mantener | Recinto o sala concreta. | Texto, 500 | Condicional | Obligatorio para presencial si se conoce; no se usa como órgano. | `sala_lugar`. |
| es_virtual | Mantener | Modalidad virtual. | Booleano | Sí | `true/false`. | `es_virtual`. |
| url_reunion | Mantener | Enlace de sesión. | Texto, 2,048 | Condicional | Obligatorio para evento futuro virtual; URL válida. Puede omitirse en histórico cerrado si no se conserva. | `url_reunion`. |
| organo_audiencia_codigo | Nuevo | Autoridad propia de la audiencia si difiere del expediente o debe congelarse históricamente. | Texto, 100 | No | Si queda vacío se usa órgano del expediente; si se captura, debe existir y se registra como snapshot/relación. | Requiere soporte en contexto/BD y UI. |
| abogado_compareciente_email | Eliminar | Correo como llave. | — | — | Sustituido por clave. | — |
| abogado_compareciente_clave_origen | Nuevo | Usuario que comparece. | Texto, 100 | No | Debe existir; compatibilidad de ámbito. Histórico puede referenciar usuario inactivo sin habilitar acceso. | `abogado_comparece_id`. |
| usuario_delegado_email | Eliminar | Correo como llave. | — | — | Sustituido por clave. | — |
| usuario_delegado_clave_origen | Nuevo | Usuario delegado. | Texto, 100 | No | Debe existir y tener alcance. | `usuario_delegado_id`. |
| fecha_desahogo | Mantener | Fecha efectiva de celebración/cierre. | Fecha | Condicional | Obligatoria si estado=CONCLUIDA; vacía si PROGRAMADA. | `fecha_desahogo`. |
| audiencia_previa_clave_origen | Nuevo | Audiencia anterior a la que sustituye una reprogramación. | Texto, 100 | Condicional | Obligatoria si estado=REPROGRAMADA; debe existir en la misma fuente. | Nueva autorrelación o tabla de reprogramaciones. |
| estado_acta_codigo | Nuevo | Disponibilidad documental. | Texto, 30 | Sí | RECIBIDO, PENDIENTE_ENTREGA, NO_DISPONIBLE, NO_COMPARTIDO o NO_APLICA. | Nueva columna/entidad o metadato documental. |
| motivo_falta_acta_codigo | Nuevo | Razón de no disponibilidad. | Texto, 50 | Condicional | Obligatorio para NO_DISPONIBLE/NO_COMPARTIDO; vacío para RECIBIDO/NO_APLICA. | Nuevo catálogo y persistencia. |
| acta_archivo | Eliminar/mover | Nombre único del acta. | — | — | Se reemplaza por DOCUMENTOS_AUDIENCIA. | — |
| observaciones | Mantener | Notas de resultado/contexto. | Texto, 32,767 | No | No truncar. | `observaciones`. |

### 6.3 DOCUMENTOS_AUDIENCIA

| Campo | Estado V2 | Descripción | Tipo / longitud | Obligatorio | Validación | Dependencia / destino |
|---|---|---|---:|---|---|---|
| audiencia_clave_origen | Nuevo | Audiencia relacionada. | Texto, 100 | Sí | Debe existir en AUDIENCIAS. | Equivalencia a audiencia. |
| documento_clave_origen | Nuevo | Identidad del documento en fuente. | Texto, 100 | Sí | Única por fuente y entidad. | Equivalencia a documento. |
| tipo_documento_codigo | Nuevo | Tipo de documento. | Texto, 50 | Sí | ACTA, CONSTANCIA_FALTANTE u otro aprobado. CONSTANCIA_FALTANTE no satisface estado RECIBIDO. | Catálogo documental. |
| archivo_relativo | Sustituye acta_archivo | Ruta relativa dentro del paquete. | Texto, 500 | Sí | Sin ruta absoluta ni `..`; archivo debe existir y coincidir exactamente. | Almacenamiento. |
| fecha_documento | Nuevo | Fecha propia del documento si se conoce. | Fecha | No | Fecha real. | Metadato documental. |
| observaciones | Nuevo | Nota específica del archivo. | Texto, 1,000 | No | Texto libre. | Metadato. |

Regla: `estado_acta_codigo=RECIBIDO` exige al menos un documento ACTA válido. Los otros estados prohíben un documento ACTA, aunque permiten una constancia separada si la política lo autoriza.

### 6.4 Catálogos requeridos

- TIPOS_AUDIENCIA, ORGANOS, ESTADOS_AUDIENCIA, ESTADOS_DOCUMENTO, MOTIVOS_FALTA_DOCUMENTO, TIPOS_DOCUMENTO_AUDIENCIA, OPERACIONES_CARGA y BOOLEANOS.
- EXPEDIENTES y USUARIOS mediante claves de origen/equivalencias.

### 6.5 Reglas y dependencias

- Presencial: `es_virtual=false`; URL vacía. Virtual: URL obligatoria si el evento seguirá operativo después del corte.
- CONCLUIDA exige fecha_desahogo. PROGRAMADA no admite fecha_desahogo.
- REPROGRAMADA conserva la audiencia anterior y la relaciona con el nuevo evento mediante `audiencia_previa_clave_origen`; ambas mantienen claves distintas.
- Depende de Catálogos, Usuarios y Expedientes. Documentos se cargan después de crear/identificar audiencias.

---

## 7. Layout_Terminos_V2.xlsx

### 7.1 Resumen de cambios

**Se mantienen:** clave de origen, expediente, asunto/actuación, ingreso, vencimiento, estado, prioridad, responsable, presentación y observaciones.

**Se eliminan o sustituyen:**

- `gerencia_codigo` se elimina porque se deriva del expediente.
- `abogado_responsable_email` se sustituye por clave de usuario.
- `acuse_archivo` se mueve a `DOCUMENTOS_TERMINO`.
- `asunto` se conserva, pero se etiqueta “Actuación / descripción”; no se renombra a tipo.

**Se agregan:** tipo de término, operación, fecha de inicio del plazo, origen del vencimiento, motivo de conclusión y estado documental del acuse.

### 7.2 TERMINOS

| Campo | Estado V2 | Descripción | Tipo / longitud | Obligatorio | Validación | Dependencia / destino |
|---|---|---|---:|---|---|---|
| clave_origen | Mantener | Identidad estable del término. | Texto, 100 | Sí | Única por fuente y entidad. | Equivalencia a `terminos.id`. |
| operacion | Nuevo | Intención de fila. | Texto, 15 | Sí | ALTA o ACTUALIZACION. | Importador. |
| gerencia_codigo | Eliminar | Duplicaba ámbito del expediente. | — | — | Backend deriva gerencia y materia del expediente. | — |
| expediente_clave_origen | Mantener | Expediente padre. | Texto, 100 | Sí | Debe existir en equivalencias. Asuntos únicos quedan fuera de este contrato V2 y requieren layout/contexto específico. | `expediente_id`. |
| tipo_termino_codigo | Nuevo | Clasificación y regla aplicable. | Texto, 100 | Sí | Debe existir, estar activa y aplicar a materia/gerencia. | `tipo_termino_id`. |
| asunto | Mantener/clarificar | Actuación o descripción concreta. | Texto, 32,767 | Sí | No vacío; no confundir con tipo. No truncar. | `terminos.asunto`. |
| fecha_ingreso | Mantener | Fecha de incorporación al control del despacho de origen. | Fecha | No | No sustituir por fecha de carga. | `fecha_ingreso`. |
| fecha_inicio_plazo | Nuevo | Fecha base que inicia el cómputo cuando corresponde. | Fecha | Condicional | Obligatoria si método del tipo=DIAS_DESDE_INICIO; puede diferir de ingreso. | Nueva columna o metadato de cálculo. |
| fecha_vencimiento | Mantener | Fecha límite histórica registrada. | Fecha | Sí | Siempre se conserva. Si se calcula una fecha de contraste, no sobrescribir sin aprobación. | `fecha_vencimiento`. |
| origen_vencimiento_codigo | Nuevo | Procedencia de la fecha límite. | Texto, 30 | Sí | FUENTE_HISTORICA, CALCULADO_SIAJ o CAPTURA_MANUAL. Para migración inicial normalmente FUENTE_HISTORICA. | Nueva columna/auditoría. |
| estatus_termino | Mantener | Estado vigente/final al corte. | Texto, 30 | Sí | Catálogo ESTATUS_TERMINO actual, hasta que negocio apruebe otro flujo. Una fecha vencida no cambia el estado. | `estatus_termino`. |
| prioridad_codigo | Mantener | Prioridad propia del término. | Texto, 50 | No | Catálogo PRIORIDADES. No equivale al semáforo. | `prioridad_id`. |
| abogado_responsable_email | Eliminar | Correo como llave. | — | — | Sustituido por clave. | — |
| abogado_responsable_clave_origen | Nuevo | Responsable actual. | Texto, 100 | Condicional | Obligatorio para términos abiertos si se conoce; debe tener alcance. | `abogado_responsable_id`. |
| fecha_presentacion | Mantener | Fecha efectiva de presentación. | Fecha | Condicional | Obligatoria si estatus=PRESENTADO. Para CONCLUIDO depende de motivo. | `fecha_presentacion`. |
| motivo_conclusion_codigo | Nuevo | Razón del cierre cuando no basta el estado. | Texto, 50 | Condicional | Obligatorio si estatus=CONCLUIDO; PRESENTADO puede usar PRESENTACION. | Nuevo catálogo/BD o metadato. |
| estado_acuse_codigo | Nuevo | Disponibilidad del acuse. | Texto, 30 | Sí | RECIBIDO, PENDIENTE_ENTREGA, NO_DISPONIBLE, NO_COMPARTIDO o NO_APLICA. | Nueva persistencia documental. |
| motivo_falta_acuse_codigo | Nuevo | Razón de ausencia. | Texto, 50 | Condicional | Obligatorio para NO_DISPONIBLE/NO_COMPARTIDO; vacío para RECIBIDO/NO_APLICA. | Catálogo y persistencia. |
| acuse_archivo | Eliminar/mover | Nombre único del archivo. | — | — | Sustituido por DOCUMENTOS_TERMINO. | — |
| observaciones | Mantener | Notas del término. | Texto, 32,767 | No | No truncar. | `observaciones`. |

### 7.3 DOCUMENTOS_TERMINO

| Campo | Estado V2 | Descripción | Tipo / longitud | Obligatorio | Validación | Dependencia / destino |
|---|---|---|---:|---|---|---|
| termino_clave_origen | Nuevo | Término relacionado. | Texto, 100 | Sí | Debe existir en TERMINOS. | Equivalencia a término. |
| documento_clave_origen | Nuevo | Identidad del documento. | Texto, 100 | Sí | Única por fuente y entidad. | Equivalencia documental. |
| tipo_documento_codigo | Nuevo | BORRADOR, ACUSE, CONSTANCIA_FALTANTE u otro aprobado. | Texto, 50 | Sí | Catálogo TIPOS_DOCUMENTO_TERMINO. | Entidad de archivos de término. |
| archivo_relativo | Sustituye acuse_archivo | Ruta relativa en paquete. | Texto, 500 | Sí | Debe existir; sin ruta absoluta ni `..`. | Almacenamiento. |
| fecha_documento | Nuevo | Fecha del documento. | Fecha | No | Fecha real. | Metadato. |
| observaciones | Nuevo | Nota del archivo. | Texto, 1,000 | No | Texto libre. | Metadato. |

Regla: `estado_acuse_codigo=RECIBIDO` exige al menos un documento ACUSE. Un PDF de “documento no disponible” sólo puede clasificarse como CONSTANCIA_FALTANTE y no satisface esa regla.

### 7.4 Catálogos requeridos

- TIPOS_TERMINO, ESTATUS_TERMINO, PRIORIDADES, ORIGENES_VENCIMIENTO, MOTIVOS_CONCLUSION, ESTADOS_DOCUMENTO, MOTIVOS_FALTA_DOCUMENTO, TIPOS_DOCUMENTO_TERMINO y OPERACIONES_CARGA.
- CALENDARIOS y TIPOS_DIAS si SIAJ calculará plazos.
- EXPEDIENTES y USUARIOS mediante equivalencias.

### 7.5 Reglas y dependencias

- `fecha_presentacion` y `fecha_vencimiento` son hechos distintos; pueden coincidir.
- PRESENTADO exige fecha_presentacion y, salvo excepción documentada, estado_acuse=RECIBIDO.
- CONCLUIDO exige motivo. Si motivo=PRESENTACION, exige fecha_presentacion.
- Un término abierto puede tener fecha de vencimiento pasada; debe conservarse vencido, no marcarse concluido.
- Si tipo usa DIAS_DESDE_INICIO, fecha_inicio_plazo y calendario permiten calcular una fecha de contraste. Una diferencia con la histórica se reporta para revisión.
- Depende de Catálogos, Usuarios y Expedientes. Documentos se cargan después del término.

---

## 8. Catálogos de sistema V2

Estos valores se publican de forma central y bloqueada. La lista exacta se valida con negocio antes de congelar V2.

| Catálogo | Valores base propuestos | Uso |
|---|---|---|
| OPERACIONES_CARGA | ALTA, ACTUALIZACION | Control idempotente de filas. |
| BOOLEANOS | true, false | Campos lógicos. |
| TIPOS_REGISTRO_USUARIO | OPERATIVO, REFERENCIA_HISTORICA | Acceso frente a atribución histórica. |
| ROLES_MIGRABLES | REFERENCIA_HISTORICA, DIRECTOR, SUBDIRECTOR, GERENTE, JEFE_DEPTO, ABOGADO; DIRECTOR_PRINCIPAL sólo autorizado | Rol base; ADMIN excluido. REFERENCIA_HISTORICA no concede permisos. |
| ETAPAS_PROCESALES | TRAMITE, LAUDO, FIRME, CONCLUIDO | Etapa vigente del expediente. |
| ESTADOS_AUDIENCIA | PROGRAMADA, CONCLUIDA, CANCELADA, REPROGRAMADA | Estado procesal propuesto; requiere adaptar enum actual. |
| ESTATUS_TERMINO | PROYECTISTA, REVISION, GERENCIA, DIRECCION, LIBERADO, PRESENTADO, CONCLUIDO | Flujo actual de términos. |
| ESTADOS_DOCUMENTO | RECIBIDO/A, PENDIENTE_ENTREGA, NO_DISPONIBLE, NO_COMPARTIDO/A, NO_APLICA | Disponibilidad separada del estado procesal. Implementar códigos neutros internos si se comparte catálogo. |
| MOTIVOS_FALTA_DOCUMENTO | NO_LOCALIZADO, EXTRAVIADO, NO_GENERADO, CLIENTE_NO_COMPARTE, OTRO | Explicación controlada; OTRO exige observación. |
| ORIGENES_VENCIMIENTO | FUENTE_HISTORICA, CALCULADO_SIAJ, CAPTURA_MANUAL | Auditoría de fecha límite. |
| METODOS_VENCIMIENTO | DIAS_DESDE_INICIO, FECHA_CAPTURADA | Sustituye “fijo/dinámico”. |
| TIPOS_DIAS | HABILES, NATURALES | Cómputo de plazo. |
| MOTIVOS_CONCLUSION_TERMINO | PRESENTACION, CUMPLIMIENTO_SIN_PRESENTACION, CANCELACION, DUPLICADO, OTRO | Cierre controlado; aprobación funcional requerida. |
| TIPOS_DOCUMENTO_AUDIENCIA | ACTA, CONSTANCIA_FALTANTE, OTRO | Manifiesto documental. |
| TIPOS_DOCUMENTO_TERMINO | BORRADOR, ACUSE, CONSTANCIA_FALTANTE, OTRO | Manifiesto documental. |

Los valores propuestos de estados/motivos que no existen hoy exigen cambio coordinado en backend, BD y UI. No deben colocarse en Excel hasta que el sistema destino los acepte.

## 9. Matriz de dependencias

| Origen | Consumidor | Llave de relación | Regla |
|---|---|---|---|
| CONTROL | Todas las filas del libro | fuente_codigo + lote_clave | Una fuente y versión coherentes por libro. |
| GERENCIAS | MATERIAS y catálogos con ámbito | gerencia_codigo | Padre previamente aprobado. |
| SEDES + TIPOS_ORGANO | ORGANOS | sede_codigo + tipo_organo_codigo | Cargar antes de órganos. |
| MATERIAS/GERENCIAS | TIPOS_TERMINO | materia_codigo/gerencia_codigo | Ámbito compatible. |
| USUARIOS | asignaciones | usuario_clave_origen | Un usuario, varias relaciones. |
| Catálogos + Usuarios | EXPEDIENTES | códigos + clave de responsable | Resolver todo antes de persistir. |
| EXPEDIENTES | PARTES/AUDIENCIAS/TERMINOS | expediente_clave_origen | Equivalencia única fuente–destino. |
| AUDIENCIAS | DOCUMENTOS_AUDIENCIA | audiencia_clave_origen | Primero entidad, después binarios. |
| TERMINOS | DOCUMENTOS_TERMINO | termino_clave_origen | Primero entidad, después binarios. |

## 10. Validación y aceptación del lote

### 10.1 Prevalidación

El sistema debe revisar todo el archivo sin persistir y devolver como mínimo: archivo, hoja, fila, clave_origen, campo, valor rechazado, código de error, mensaje y acción esperada. Ningún error se sustituye por un valor predeterminado.

Controles mínimos:

1. Versión, hojas y encabezados exactos.
2. Claves duplicadas dentro del archivo, entre hojas y contra equivalencias anteriores.
3. Códigos inexistentes, inactivos o fuera de ámbito.
4. Longitudes y formatos reales; fechas y horas inválidas.
5. Reglas condicionales de estados, fechas, responsables y documentos.
6. Existencia física de archivos del manifiesto, ruta segura y asociación única.
7. Coherencia entre gerencia, materia, órgano, tipo y usuario.
8. Simulación de altas/actualizaciones con conteos esperados.

### 10.2 Persistencia y conciliación

- El lote se aplica sólo después de aprobar la prevalidación o un conjunto explícito de excepciones.
- Cada fila mantiene estatus individual. Una excepción no puede terminar como PROCESADO/IMPORTADO.
- Reintento idempotente: misma fuente, entidad y clave actualiza o confirma; nunca crea un duplicado.
- Resultado obligatorio: recibidos = importados + actualizados + omitidos justificados + rechazados.
- Devolver IDs destino, relaciones creadas, documentos almacenados y errores. Verificar registros en destino, no sólo el estado del lote.

### 10.3 Pruebas de aceptación

- Más de 200 filas por hoja.
- Expedientes con número repetido en fuentes/órganos diferentes.
- Usuarios sin correo, multigerencia e históricos inactivos.
- Término vencido pero abierto; presentado antes, el día y después del vencimiento.
- Audiencia concluida con acta, pendiente de entrega y no compartida.
- Documento faltante, duplicado, ruta inválida y archivo no correspondiente.
- Repetición completa del lote sin duplicados.
- Edición posterior en UI de expediente, audiencia y término migrados.
- Verificación de tipo de término y semáforo en lista, agenda, dashboard y alertas cuando esa automatización forme parte de V2.

## 11. Cambios técnicos necesarios antes de publicar los Excel V2

| Componente | Cambios mínimos |
|---|---|
| Excel | Crear los cinco libros con Tablas ampliables, CONTROL, listas de referencia bloqueadas, diccionario y validaciones condicionales. |
| Backend | Importador versionado por hoja; resolución de códigos/claves; prevalidación; idempotencia; reporte por fila; documentos por manifiesto; sin transformaciones silenciosas. |
| Base de Datos | Equivalencias de migración únicas. Confirmar/desplegar tipos_termino y FK. Agregar estado documental/motivos y campos temporales sólo para las funciones aprobadas. Normalizar tipo de órgano, partes o gerencia principal si se adoptan esas capacidades. |
| UI | Alinear obligatoriedad de tipo de expediente; persistir tipo de término; separar estados documentales/procesales; mostrar equivalencias/errores; administrar catálogos nuevos. |
| Proceso operativo | Gobierno de catálogo, responsables de aprobación, regla de corte, atención de pendientes, recepción documental, supresión/activación controlada de notificaciones y acta de conciliación. |

## 12. Decisiones que deben cerrarse antes de generar los XLSX definitivos

1. Si V2 calculará vencimientos o sólo preservará fechas históricas. Si calculará, se requieren calendarios, fecha de inicio y reglas versionadas.
2. Si partes procesales se normalizan en V2 o se conserva sólo texto con una limitación explícita.
3. Si una audiencia puede tener órgano distinto al expediente y si se conservarán reprogramaciones como eventos relacionados.
4. Estados finales de audiencia y términos, motivos de cierre y política de evidencia faltante.
5. Tratamiento de Director Principal y de referencias históricas sin acceso.
6. Ámbito de unicidad global o por gerencia para materias, tipos y órganos, alineado con restricciones reales de BD.
7. Política para históricos sin tipo de expediente: homologación previa o código controlado SIN_CLASIFICAR.
8. Alcance contractual de la historia: estado al corte, cronología completa, responsables anteriores y versiones documentales.

Hasta cerrar estas decisiones, este documento es un contrato funcional propuesto. Una plantilla con desplegables no debe distribuirse como “productiva” si el backend y el esquema aún no aceptan los mismos campos y valores.

## 13. Fuentes locales revisadas

- Cinco layouts actuales bajo `Layouts_Carga_Masiva_SIAJ/`.
- `Revision_funcional_layouts_SIAJ.md` y `Matriz_GAP_Funcional_SIAJ_V1.md` del análisis previo.
- Entidades JPA de Usuario, Expediente, Audiencia, Término, catálogos y contextos.
- Controladores, servicios de importación, permisos, catálogos y semáforos.
- Plantillas de alta/edición y tabla de audiencias.
- `db_schema_actual.sql` local.

La propuesta no acredita que los cambios indicados estén implementados en el ambiente destino; la aceptación técnica debe verificarlo antes de liberar los libros.
