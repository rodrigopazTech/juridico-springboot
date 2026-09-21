# Layouts V2 Mínimos Viables para Migración

Fecha: 9 de septiembre de 2026  
Contrato mínimo: `2.0-MV`

## 1. Alcance

Esta versión incorpora únicamente los cambios necesarios para migrar de forma íntegra, repetible y conciliable hacia las funcionalidades actuales de SIAJ. No añade normalización de partes, nuevos estados procesales, calendarios jurídicos, motores de vencimiento, tipos normalizados de órgano, gerencia principal, folios nuevos ni soporte general para múltiples documentos.

Se conservan los cinco archivos. El backend debe leer por nombre de hoja, validar el contrato completo antes de guardar y devolver equivalencias y resultados por fila.

Orden: **Catálogos → Usuarios → Expedientes → Audiencias y Términos → archivos asociados → conciliación y apertura operativa**.

## 2. Estructura común mínima

Cada libro contiene:

| Hoja | Contenido | Regla |
|---|---|---|
| CONTROL | Metadatos de lote. | Importable y obligatoria. |
| CAPTURA o las hojas de catálogo | Registros. | Tablas ampliables; encabezados exactos. |
| INSTRUCCIONES | Reglas de captura. | No importable. |
| DICCIONARIO | Definiciones. | No importable. |
| CATALOGOS_REFERENCIA | Copia bloqueada de valores autorizados. | No editable ni importable. |

### CONTROL

| Campo | Tipo / longitud | Obligatorio | Validación |
|---|---:|---|---|
| layout_version | Texto, 10 | Sí | Valor fijo `2.0-MV`. |
| fuente_codigo | Texto, 50 | Sí | Fuente registrada; estable entre lotes. |
| lote_clave | Texto, 100 | Sí | Única; no forma parte de la identidad del registro. |
| catalogo_version | Texto, 50 | Sí | Debe coincidir con la versión publicada. |
| fecha_corte | Fecha | Sí | Fecha real `AAAA-MM-DD`. |
| responsable_entrega | Texto, 200 | Sí | No vacío. |
| email_responsable | Texto, 100 | Sí | Correo válido; sólo contacto. |

Reglas comunes:

- Identidad: `(fuente_codigo, entidad, clave_origen)` con restricción única.
- `operacion`: ALTA exige clave nueva; ACTUALIZACION exige equivalencia existente.
- Códigos y claves son texto; fechas/horas son valores reales de Excel.
- El backend rechaza valores inválidos. No usa hoy, 00:00, primera materia/prioridad ni truncamiento como sustitutos.
- No se resuelve por nombre parecido.
- El reporte devuelve por fila: resultado, causa e ID destino.

## 3. Layout_Catalogos_V2_MV.xlsx

Hojas de datos: GERENCIAS, MATERIAS, SEDES, PRIORIDADES, TIPOS_EXPEDIENTE, ORGANOS, TIPOS_AUDIENCIA y TIPOS_TERMINO.

### GERENCIAS

| Campo | Tipo / longitud | Obligatorio | Validación |
|---|---:|---|---|
| gerencia_codigo | Texto, 100 | Sí | Único y estable. |
| nombre | Texto, 200 | Sí | No vacío; regla de unicidad del destino. |
| descripcion | Texto, 32,767 | No | No truncar. |
| activo | Booleano | Sí | true/false. |

### MATERIAS

| Campo | Tipo / longitud | Obligatorio | Validación |
|---|---:|---|---|
| materia_codigo | Texto, 100 | Sí | Único en el ámbito aprobado. |
| gerencia_codigo | Texto, 100 | Sí | Existe en GERENCIAS. |
| nombre | Texto, 100 | Sí | Único conforme al destino. |
| descripcion | Texto, 32,767 | No | No truncar. |
| activo | Booleano | Sí | true/false. |

### SEDES

| Campo | Tipo / longitud | Obligatorio | Validación |
|---|---:|---|---|
| sede_codigo | Texto, 100 | Sí | Único. |
| nombre | Texto, 100 | Sí | Único conforme al destino. |
| gerencia_codigo | Texto, 100 | No | Vacío significa global; si existe, referencia GERENCIAS. |
| activo | Booleano | Sí | true/false. |

### PRIORIDADES

| Campo | Tipo / longitud | Obligatorio | Validación |
|---|---:|---|---|
| prioridad_codigo | Texto, 50 | Sí | Único; ALTA, MEDIA o BAJA para V1 salvo aprobación. |
| nombre | Texto, 50 | Sí | No vacío. |
| color | Texto, 100 | No | Valor técnico suministrado por implementación; el cliente no lo modifica. |
| activo | Booleano | Sí | true/false. |

### TIPOS_EXPEDIENTE

| Campo | Tipo / longitud | Obligatorio | Validación |
|---|---:|---|---|
| tipo_expediente_codigo | Texto, 100 | Sí | Único y estable. |
| nombre | Texto, 100 | Sí | Único conforme al destino. |
| descripcion | Texto, 32,767 | No | Distinguir de materia y etapa. |
| gerencia_codigo | Texto, 100 | No | Vacío global; si existe, referencia GERENCIAS. |
| activo | Booleano | Sí | true/false. |

Debe existir un valor `SIN_CLASIFICAR` sólo si el cliente aprueba que algunos históricos no pueden homologarse.

### ORGANOS

| Campo | Tipo / longitud | Obligatorio | Validación |
|---|---:|---|---|
| organo_codigo | Texto, 100 | Sí | Único y estable. |
| gerencia_codigo | Texto, 100 | No | Vacío global; si existe, referencia GERENCIAS. |
| nombre | Texto, 300 | Sí | No vacío. |
| sede_codigo | Texto, 100 | No | Referencia SEDES. |
| tipo | Texto, 50 | No | Lista controlada publicada; continúa como texto para evitar cambio de modelo. |
| activo | Booleano | Sí | true/false. |

### TIPOS_AUDIENCIA

| Campo | Tipo / longitud | Obligatorio | Validación |
|---|---:|---|---|
| tipo_audiencia_codigo | Texto, 100 | Sí | Único conforme al destino. |
| gerencia_codigo | Texto, 100 | Sí | Referencia GERENCIAS. |
| nombre | Texto, 100 | Sí | No vacío. |
| descripcion | Texto, 32,767 | No | No truncar. |
| activo | Booleano | Sí | true/false. |

Los umbrales de audiencia permanecen bajo la configuración actual; V1-MV no promete reglas por tipo hasta que se unifique su aplicación.

### TIPOS_TERMINO

| Campo | Tipo / longitud | Obligatorio | Validación |
|---|---:|---|---|
| tipo_termino_codigo | Texto, 100 | Sí | Único y estable. |
| nombre | Texto, 100 | Sí | Único conforme al destino. |
| descripcion | Texto, 32,767 | No | Distinguir del asunto concreto. |
| dias_predeterminados | Entero | Sí | Mayor que cero; valor informativo/configuración actual. |
| tipo_dias | Texto, 20 | Sí | HABILES o NATURALES. V1-MV no recalcula históricos. |
| semaforo_rojo_dias | Entero | Sí | Mayor o igual a cero. |
| semaforo_amarillo_dias | Entero | Sí | Mayor que semaforo_rojo_dias. |
| materia_codigo | Texto, 100 | No | Debe existir y ser compatible con gerencia. |
| gerencia_codigo | Texto, 100 | No | Debe existir si se captura. |
| activo | Booleano | Sí | true/false. |

Dependencias internas: GERENCIAS antes de MATERIAS; SEDES antes de ORGANOS; GERENCIAS/MATERIAS antes de tipos con ámbito. El libro completo debe publicarse antes de Usuarios.

## 4. Layout_Usuarios_V2_MV.xlsx

Hoja importable: CAPTURA.

| Campo | Tipo / longitud | Obligatorio | Validación |
|---|---:|---|---|
| usuario_clave_origen | Texto, 100 | Sí | Única por fuente; referencia oficial de migración. |
| operacion | Texto, 15 | Sí | ALTA o ACTUALIZACION; actualización exige equivalencia previa. |
| gerencia_codigo | Texto, 100 | Condicional | Obligatoria para GERENTE, JEFE_DEPTO y ABOGADO; debe existir. |
| nombre_completo | Texto, 200 | Sí | No vacío. |
| email | Texto, 100 | No | Único y válido si existe; no es llave. |
| username | Texto, 80 | Condicional | Obligatorio si no hay login externo; único. |
| numero_empleado | Texto, 40 | No | Único si existe; preservar ceros. |
| rol | Texto, 50 | Sí | Rol permitido; ADMIN excluido. DIRECTOR_PRINCIPAL requiere autorización. |
| puesto | Texto, 160 | No | Texto libre. |
| telefono_institucional | Texto, 120 | No | Texto libre. |
| telefono_movil | Texto, 80 | No | Texto libre. |
| ubicacion_oficina | Texto, 180 | No | No confundir con sede jurisdiccional. |
| materias_codigos | Texto, 1,000 | No | Códigos separados por punto y coma; cada valor debe existir y corresponder a gerencia. |
| permisos_adicionales | Texto, 1,000 | No | Sólo extras no heredados; cada código debe existir y ser otorgable. |
| activo | Booleano | Sí | true/false. Usuarios históricos sin acceso quedan false. |

La contraseña, tokens y códigos de recuperación no se capturan. El importador crea la cuenta mediante el mecanismo de activación vigente. Si el lote contiene usuarios realmente multigerencia, las hojas relacionales R02 dejan de ser opcionales.

Depende de Catálogos. Se carga antes de Expedientes.

## 5. Layout_Expedientes_V2_MV.xlsx

Hoja importable: CAPTURA.

| Campo | Tipo / longitud | Obligatorio | Validación |
|---|---:|---|---|
| clave_origen | Texto, 100 | Sí | Única por fuente/entidad. |
| operacion | Texto, 15 | Sí | ALTA o ACTUALIZACION. |
| gerencia_codigo | Texto, 100 | Sí | Existe y es compatible con materia/responsable. |
| numero_expediente | Texto, 50 | Sí | Número oficial; no se usa como llave única. |
| materia_codigo | Texto, 100 | Sí | Existe y pertenece a gerencia. |
| descripcion | Texto, 32,767 | Sí | No vacía ni truncada. |
| etapa_procesal | Texto, 30 | Sí | TRAMITE, LAUDO, FIRME o CONCLUIDO. |
| tipo_expediente_codigo | Texto, 100 | Sí | Existe, activo y aplicable. |
| prioridad_codigo | Texto, 50 | No | Existe y activo. |
| organo_codigo | Texto, 100 | No | Existe; no resolver por similitud de nombre. |
| sede_codigo | Texto, 100 | No | Existe; si órgano tiene sede, debe coincidir. |
| partes | Texto, 32,767 | No | Se conserva íntegro como texto en V1-MV. |
| abogado_responsable_clave_origen | Texto, 100 | Condicional | Usuario existente y con alcance; obligatorio para expediente activo si el cliente conoce responsable. |

Se elimina `abogado_responsable_email`. No se añaden partes estructuradas. La detección de posibles homónimos genera conflicto para revisión y nunca fusiona automáticamente.

Depende de Catálogos y Usuarios. Se carga antes de Audiencias y Términos.

## 6. Layout_Audiencias_V2_MV.xlsx

Hoja importable: CAPTURA.

| Campo | Tipo / longitud | Obligatorio | Validación |
|---|---:|---|---|
| clave_origen | Texto, 100 | Sí | Única por fuente/entidad. |
| operacion | Texto, 15 | Sí | ALTA o ACTUALIZACION. |
| gerencia_codigo | Texto, 100 | Sí | Debe coincidir con el expediente. Se conserva para control cruzado en V1-MV. |
| expediente_clave_origen | Texto, 100 | Sí | Debe existir en equivalencias. |
| tipo_audiencia_codigo | Texto, 100 | Sí | Existe y aplica a gerencia. |
| fecha_audiencia | Fecha | Sí | Fecha real; no determina por sí sola el estado. |
| hora_audiencia | Hora | Sí | Hora real HH:mm; no sustituir por 00:00. |
| estatus_audiencia | Texto, 30 | Sí | PENDIENTE, CON_ACTA o CONCLUIDA; conserva el enum actual. |
| sala_lugar | Texto, 500 | No | Recinto, no órgano. |
| es_virtual | Booleano | Sí | true/false. |
| url_reunion | Texto, 2,048 | Condicional | Obligatoria para audiencia futura virtual; URL válida. |
| abogado_compareciente_clave_origen | Texto, 100 | No | Usuario existente; puede estar inactivo si sólo se conserva historia. |
| usuario_delegado_clave_origen | Texto, 100 | No | Usuario existente y con alcance si el evento seguirá activo. |
| fecha_desahogo | Fecha | Condicional | Obligatoria para CONCLUIDA. |
| estado_acta_codigo | Texto, 30 | Sí | RECIBIDO, PENDIENTE_ENTREGA, NO_DISPONIBLE, NO_COMPARTIDO o NO_APLICA. |
| motivo_falta_acta_codigo | Texto, 50 | Condicional | Obligatorio para NO_DISPONIBLE o NO_COMPARTIDO. |
| acta_archivo | Texto, 500 | Condicional | Obligatorio si estado_acta=RECIBIDO; ruta relativa y archivo existente. |
| observaciones | Texto, 32,767 | No | No truncar. |

Se eliminan los dos correos usados como llave. V1-MV no agrega órgano propio, reprogramaciones relacionadas ni manifiesto multianexo. El estado documental puede guardarse en metadatos de migración; para atender pendientes desde la pantalla se implementa R04.

Reglas cruzadas mínimas:

- CON_ACTA exige `estado_acta_codigo=RECIBIDO` y un `acta_archivo` existente.
- CONCLUIDA exige `fecha_desahogo`; puede tener acta recibida o un estado de faltante documentado.
- PENDIENTE no admite `fecha_desahogo`. Una fecha de audiencia pasada no cambia automáticamente el estado.

Depende de Catálogos, Usuarios y Expedientes.

## 7. Layout_Terminos_V2_MV.xlsx

Hoja importable: CAPTURA.

| Campo | Tipo / longitud | Obligatorio | Validación |
|---|---:|---|---|
| clave_origen | Texto, 100 | Sí | Única por fuente/entidad. |
| operacion | Texto, 15 | Sí | ALTA o ACTUALIZACION. |
| gerencia_codigo | Texto, 100 | Sí | Debe coincidir con expediente. Se conserva como control cruzado. |
| expediente_clave_origen | Texto, 100 | Sí | Debe existir en equivalencias. |
| tipo_termino_codigo | Texto, 100 | Sí | Debe existir y aplicar a materia/gerencia. |
| asunto | Texto, 32,767 | Sí | Actuación concreta; no es el tipo. No truncar. |
| fecha_vencimiento | Fecha | Sí | Fecha histórica registrada; no se recalcula en V1-MV. |
| fecha_ingreso | Fecha | No | Fecha de incorporación al control; no usar fecha de carga. |
| estatus_termino | Texto, 30 | Sí | PROYECTISTA, REVISION, GERENCIA, DIRECCION, LIBERADO, PRESENTADO o CONCLUIDO. Una fecha pasada no cambia el estado. |
| prioridad_codigo | Texto, 50 | No | Existe y activo. |
| abogado_responsable_clave_origen | Texto, 100 | Condicional | Usuario existente y con alcance para términos abiertos. |
| fecha_presentacion | Fecha | Condicional | Obligatoria para PRESENTADO; para CONCLUIDO se valida conforme al motivo documentado en observaciones de V1-MV. |
| estado_acuse_codigo | Texto, 30 | Sí | RECIBIDO, PENDIENTE_ENTREGA, NO_DISPONIBLE, NO_COMPARTIDO o NO_APLICA. |
| motivo_falta_acuse_codigo | Texto, 50 | Condicional | Obligatorio para NO_DISPONIBLE o NO_COMPARTIDO. |
| acuse_archivo | Texto, 500 | Condicional | Obligatorio si estado_acuse=RECIBIDO; ruta relativa y archivo existente. |
| observaciones | Texto, 32,767 | No | No truncar; documentar motivo de CONCLUIDO si no fue presentación. |

Se elimina `abogado_responsable_email`. V1-MV no agrega fecha_inicio_plazo, origen_vencimiento, calendario, método de cálculo ni manifiesto multianexo. El backend debe persistir `tipo_termino_codigo` en la FK y conservar la fecha de vencimiento aportada.

Reglas cruzadas mínimas:

- PRESENTADO exige `fecha_presentacion`. Si no hay acuse, debe registrarse un estado de faltante y su motivo; no se inventa un archivo.
- CONCLUIDO sin fecha de presentación exige explicar el motivo en `observaciones` durante V1-MV.
- Los demás estados no reciben `fecha_presentacion` salvo que la fuente documente una transición incoherente que deba revisarse antes de cargar.

Depende de Catálogos, Usuarios y Expedientes.

## 8. Catálogos de referencia mínimos

| Catálogo | Valores/regla mínima |
|---|---|
| OPERACIONES_CARGA | ALTA, ACTUALIZACION. |
| BOOLEANOS | true, false. |
| ROLES_MIGRABLES | DIRECTOR, SUBDIRECTOR, GERENTE, JEFE_DEPTO, ABOGADO; DIRECTOR_PRINCIPAL sólo autorizado. ADMIN excluido. |
| PERMISOS | Códigos existentes; mostrar título y descripción. |
| ETAPAS_PROCESALES | TRAMITE, LAUDO, FIRME, CONCLUIDO. |
| ESTATUS_AUDIENCIA | PENDIENTE, CON_ACTA, CONCLUIDA. |
| ESTATUS_TERMINO | PROYECTISTA, REVISION, GERENCIA, DIRECCION, LIBERADO, PRESENTADO, CONCLUIDO. |
| ESTADOS_DOCUMENTO | RECIBIDO, PENDIENTE_ENTREGA, NO_DISPONIBLE, NO_COMPARTIDO, NO_APLICA. |
| MOTIVOS_FALTA_DOCUMENTO | NO_LOCALIZADO, EXTRAVIADO, NO_GENERADO, CLIENTE_NO_COMPARTE, OTRO. |
| TIPOS_DIAS | HABILES, NATURALES; informativo en V1-MV. |

## 9. Componentes que V1-MV debe cambiar

| Componente | Cambio mínimo obligatorio |
|---|---|
| Excel | Cinco plantillas OOXML, CONTROL, claves estables, tipo de término, referencias de usuario por clave, estado documental mínimo y listas bloqueadas. |
| Backend | Contrato versionado; mapeo de claves/códigos; persistencia del tipo de término; fechas/estados sin inferencias; archivos verificados; prevalidación e idempotencia. |
| Base de Datos | Tabla/restricción de equivalencias; estados y errores del lote; confirmar/desplegar tabla/FK de tipos de término. El estado documental puede quedar como metadato de migración en V1-MV. |
| UI | Descargar plantillas reales, mostrar prevalidación/conciliación y corregir guardado del Tipo de Término. No se rediseñan pantallas operativas. |
| Proceso operativo | Aprobar catálogos, secuencia, corte, excepciones documentales, responsables vigentes, reintentos, conciliación y activación de notificaciones. |

## 10. Condiciones de aceptación

1. El mismo libro puede prevalidarse y cargarse después sin cambiar su interpretación.
2. Repetir el lote no crea duplicados.
3. Ninguna fecha, hora, materia, prioridad, estado o responsable se inventa como fallback.
4. Cada audiencia y término queda relacionado con el expediente correcto por equivalencia.
5. El Tipo de Expediente permite alta/edición y el Tipo de Término queda persistido.
6. Cada archivo declarado RECIBIDO existe y puede recuperarse; un faltante no se registra como evidencia.
7. Recibidos = importados + actualizados + omitidos justificados + rechazados.
8. Los pendientes activos tienen responsable vigente antes de habilitar notificaciones.

Si los documentos quedan completamente fuera del alcance contractual, pueden omitirse `estado_*`, `motivo_falta_*` y `*_archivo`, y O16/O17 se documentan como exclusión. Si existen usuarios multigerencia o varios documentos por actuación, R02 o R05 pasan a ser obligatorios y deben incorporarse antes de la carga.
