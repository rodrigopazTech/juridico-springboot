El presente Diccionario de Datos es un documento técnico que describe de manera estructurada y detallada los elementos que conforman la base de datos del sistema. Su finalidad es proporcionar una guía clara sobre la organización, significado y características de cada uno de los datos utilizados dentro del sistema.

Tabla:actividad_expedientes

Historial de actividades/acciones realizadas en un expediente (subidas de documentos, ediciones, cambios de estado

| **Nombre Tabla** | **Nombre Columna** | **Tipo de Dato** | **Longitud Max** | **Requisito** | **Tipo de Llave (PK/FK)** | **Descripción Funcional** |
| --- | --- | --- | --- | --- | --- | --- |
| actividad_expedientes | id | integer |  | Obligatorio (NOT NULL) | PK | Identificador único de la actividad. |
| actividad_expedientes | descripcion | text |  | Opcional (NULL) |  | Texto detallado o notas adicionales. |
| actividad_expedientes | fecha_registro | timestamp without time zone |  | Opcional (NULL) |  | Marca de tiempo exacta del suceso |
| actividad_expedientes | tipo_icono | character varying | 30 | Obligatorio (NOT NULL) |  | Nombre del icono (ej. fa-edit) para mostrar en la línea de tiempo. |
| actividad_expedientes | titulo | character varying | 200 | Obligatorio (NOT NULL) |  | Título breve de la actividad (ej. "Demanda Admitida"). |
| actividad_expedientes | expediente_id | uuid |  | Obligatorio (NOT NULL) | FK | Llave Foránea (FK). Relación con la tabla expediente. |
| actividad_expedientes | usuario_id | integer |  | Opcional (NULL) | FK | Llave Foránea (FK). Relación con la tabla usuario. |

Audiencias:Registra las audiencias programadas para cada expediente, incluyendo fecha, hora y estado procesal.

| **Nombre Tabla** | **Nombre Columna** | **Tipo de Dato** | **Longitud Max** | **Requisito** | **Tipo de Llave (PK/FK)** | **Descripción Funcional** |
| --- | --- | --- | --- | --- | --- | --- |
| audiencias | id | integer |  | Obligatorio (NOT NULL) | PK | Llave primaria autoincremental del registro. |
| audiencias | acta_documento | character varying | 500 | Opcional (NULL) |  | Ruta o referencia al archivo del acta levantada. |
| audiencias | created_at | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora automática de creación. |
| audiencias | es_virtual | boolean |  | Opcional (NULL) |  | Indica la modalidad. Si es TRUE, el campo url_reunion es obligatorio” |
| audiencias | estatus_audiencia | character varying | 30 | Opcional (NULL) |  | "Valores permitidos: 'PENDIENTE', 'CON ACTA' 'CONCLUIDA'. Controla si se puede editar el acta." |
| audiencias | fecha_audiencia | date |  | Obligatorio (NOT NULL) |  | Fecha programada para la audiencia. |
| audiencias | fecha_desahogo | date |  | Opcional (NULL) |  | Fecha real en que ocurrió (puede diferir de la programada). |
| audiencias | hora_audiencia | time without time zone |  | Obligatorio (NOT NULL) |  | Hora en que se llevara acabo la audiencia |
| audiencias | observaciones | text |  | Opcional (NULL) |  | Comentarios u observaciones relevantes. |
| audiencias | sala_lugar | text |  | Opcional (NULL) |  | Lugar presencial donde se llebara acabo la audiencia |
| audiencias | updated_at | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora de la última modificación. |
| audiencias | url_reunion | text |  | Opcional (NULL) |  | Enlace a la videollamada si es_virtual es TRUE. |
| audiencias | abogado_comparece_id | integer |  | Opcional (NULL) | FK | Llave Foránea (FK). Relación con la tabla abogado_comparece. |
| audiencias | expediente_id | uuid |  | Obligatorio (NOT NULL) | FK | Llave Foránea (FK). Relación con la tabla expediente. |
| audiencias | tipo_audiencia_id | integer |  | Opcional (NULL) | FK | Llave Foránea (FK). Relación con la tabla tipo_audiencia. |

Tabla:Audiencias_desahogadas

Almacena el resultado o conclusión de las audiencias realizadas, incluyendo observaciones y fecha de cierre.

|  |  |  |  |  |  |  |
| --- | --- | --- | --- | --- | --- | --- |
| **Nombre Tabla** | **Nombre Columna** | **Nombre Columna** | **Nombre Columna** | **Nombre Columna** | **Nombre Columna** | **Descripción Funcional** |
| audiencias_desahogadas | id | integer |  | Obligatorio (NOT NULL) | PK | Llave primaria autoincremental del registro. |
| audiencias_desahogadas | expediente_numero | character varying | 50 | Opcional (NULL) |  | Referencia alfanumérica del expediente asociado |
| audiencias_desahogadas | fecha_audiencia | date |  | Obligatorio (NOT NULL) |  | Fecha programada para la audiencia. |
| audiencias_desahogadas | fecha_desahogo | date |  | Obligatorio (NOT NULL) |  | Fecha real en que ocurrió (puede diferir de la programada). |
| audiencias_desahogadas | hora_audiencia | time without time zone |  | Obligatorio (NOT NULL) |  | Hora en que se llevara acabo la audiencia |
| audiencias_desahogadas | sincronizado_at | timestamp without time zone |  | Opcional (NULL) |  | Marca de tiempo de sincronización con sistemas externos o auditoría. |
| audiencias_desahogadas | audiencia_id | integer |  | Obligatorio (NOT NULL) | FK | Llave Foránea (FK). Relación con la tabla audiencia. |

Tabla: Carpetas

Organiza los documentos de un expediente en estructuras lógicas, facilitando la clasificación y orden interno de archivos.

| **Nombre Tabla** | **Nombre Columna** | **Tipo de Dato** | **Longitud Max** | **Requisito** | **Tipo de Llave (PK/FK)** | **Descripción Funcional** |
| --- | --- | --- | --- | --- | --- | --- |
| carpetas | id | uuid |  | Obligatorio (NOT NULL) | PK | Llave primaria autoincremental del registro. |
| carpetas | es_sistema | boolean |  | Obligatorio (NOT NULL) |  | Flag de control. Define si la carpeta es predefinida por el sistema (protegida) o creada por el usuario |
| carpetas | expediente_id | uuid |  | Obligatorio (NOT NULL) | FK | Llave Foránea (FK). Relación con la tabla expediente. |
| carpetas | fecha_creacion | timestamp without time zone |  | Obligatorio (NOT NULL) |  | Registro temporal de la instancia. |
| carpetas | nombre | character varying | 255 | Obligatorio (NOT NULL) |  | Nombre descriptivo del registro. |
| carpetas | orden | integer |  | Obligatorio (NOT NULL) |  | Valor numérico para determinar la secuencia de visualización en la interfaz. |
| carpetas | carpeta_padre_id | uuid |  | Opcional (NULL) | FK | Llave Foránea (FK). Relación con la tabla carpeta_padre. |
| carpetas | usuario_creador_id | integer |  | Opcional (NULL) | FK | Llave Foránea (FK). Relación con la tabla usuario_creador. |

Tabla:Cat_estados

Catálogo de estados de la República Mexicana para jurisdicción.

| **Nombre Tabla** | **Nombre Columna** | **Tipo de Dato** | **Longitud Max** | **Requisito** | **Tipo de Llave (PK/FK)** | **Descripción Funcional** |
| --- | --- | --- | --- | --- | --- | --- |
| cat_estados | id | integer |  | Obligatorio (NOT NULL) | PK | Llave primaria autoincremental del registro. |
| cat_estados | nombre | character varying | 255 | Opcional (NULL) |  | Nombre descriptivo del registro. |

Tabla:Colaboradores_expediente

Usuarios externos o internos que tienen acceso compartido a un expediente, con fecha de expiración y nivel de permiso.

| **Nombre Tabla** | **Nombre Columna** | **Tipo de Dato** | **Longitud Max** | **Requisito** | **Tipo de Llave (PK/FK)** | **Descripción Funcional** |
| --- | --- | --- | --- | --- | --- | --- |
| colaboradores_expediente | id | integer |  | Obligatorio (NOT NULL) | PK | Llave primaria autoincremental del registro. |
| colaboradores_expediente | fecha_expiracion | timestamp without time zone |  | Obligatorio (NOT NULL) |  | Límite temporal de validez del acceso. |
| colaboradores_expediente | motivo | text |  | Opcional (NULL) |  | Justificación narrativa para la asignación del colaborador. |
| colaboradores_expediente | permiso_nivel | character varying | 255 | Opcional (NULL) |  | Definición del alcance de acciones permitidas |
| colaboradores_expediente | expediente_id | uuid |  | Obligatorio (NOT NULL) | FK | Llave Foránea (FK). Relación con la tabla expediente. |
| colaboradores_expediente | usuario_id | integer |  | Obligatorio (NOT NULL) | FK | Llave Foránea (FK). Relación con la tabla usuario. |

Tabla:Comentarios

Comentarios genéricos en cualquier entidad del sistema. Incluye: tipo de entidad, ID de entidad, contenido del comentario, usuario autor y fecha..

| **Nombre Tabla** | **Nombre Columna** | **Tipo de Dato** | **Longitud Max** | **Requisito** | **Tipo de Llave (PK/FK)** | **Descripción Funcional** |
| --- | --- | --- | --- | --- | --- | --- |
| comentarios | id | integer |  | Obligatorio (NOT NULL) | PK | Llave primaria autoincremental del registro. |
| comentarios | comentario | text |  | Obligatorio (NOT NULL) |  | Contenido textual de la nota. |
| comentarios | created_at | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora automática de creación. |
| comentarios | entidad_id | character varying | 50 | Obligatorio (NOT NULL) |  |  |
| comentarios | entidad_tipo | character varying | 30 | Obligatorio (NOT NULL) |  | tipo de entidad que es el comentario |
| comentarios | usuario_nombre | character varying | 255 | Opcional (NULL) |  | Nombre del suario que realiza el comentario |
| comentarios | usuario_id | integer |  | Opcional (NULL) | FK | Llave Foránea (FK). Relación con la tabla usuario. |

Tabla:Documentos

Archivos subidos al sistema. Incluye: ID del expediente, nombre del archivo, ruta, tipo MIME, tamaño, carpeta destino, usuario que subió, fecha de subida y descripción.

| **Nombre Tabla** | **Nombre Columna** | **Tipo de Dato** | **Longitud Max** | **Requisito** | **Tipo de Llave (PK/FK)** | **Descripción Funcional** |
| --- | --- | --- | --- | --- | --- | --- |
| documentos | id | uuid |  | Obligatorio (NOT NULL) | PK | Llave primaria autoincremental del registro. |
| documentos | created_at | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora automática de creación. |
| documentos | descripcion | text |  | Opcional (NULL) |  | Texto detallado o notas adicionales. |
| documentos | expediente_id | uuid |  | Obligatorio (NOT NULL) | FK | Llave Foránea (FK). Relación con la tabla expediente. |
| documentos | fecha_subida | timestamp without time zone |  | Obligatorio (NOT NULL) |  | Fecha y hora de subida del archivo |
| documentos | nombre_archivo | character varying | 255 | Obligatorio (NOT NULL) |  | Nombre con el que se guarda el archivo |
| documentos | nombre_original | character varying | 255 | Obligatorio (NOT NULL) |  | Nombre original del archivo antes de ser guardado |
| documentos | ruta_archivo | character varying | 500 | Obligatorio (NOT NULL) |  | Ruta en la que se encuentran el o los archivos |
| documentos | tamanio_bytes | bigint |  | Obligatorio (NOT NULL) |  | Tamaño de arhivo guardado |
| documentos | tipo_mime | character varying | 100 | Opcional (NULL) |  | Tipo de archivo que se guarda |
| documentos | updated_at | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora de la última modificación. |
| documentos | carpeta_id | uuid |  | Opcional (NULL) | FK | Llave Foránea (FK). Relación con la tabla carpeta. |
| documentos | usuario_subida_id | integer |  | Opcional (NULL) | FK | Llave Foránea (FK). Relación con la tabla usuario_subida. |

Tabla:Documentos_expediente

Gestiona los archivos electrónicos asociados a cada expediente, garantizando almacenamiento estructurado y trazabilidad documental.

| **Nombre Tabla** | **Nombre Columna** | **Tipo de Dato** | **Longitud Max** | **Requisito** | **Tipo de Llave (PK/FK)** | **Descripción Funcional** |
| --- | --- | --- | --- | --- | --- | --- |
| documentos_expediente | id | integer |  | Obligatorio (NOT NULL) | PK | Llave primaria autoincremental del registro. |
| documentos_expediente | descripcion | text |  | Opcional (NULL) |  | Texto detallado o notas adicionales. |
| documentos_expediente | mime_type | character varying | 100 | Opcional (NULL) |  | Identificador de formato de archivo |
| documentos_expediente | nombre_archivo | character varying | 300 | Obligatorio (NOT NULL) |  | Nombre original o lógico del documento. |
| documentos_expediente | ruta_archivo | character varying | 500 | Obligatorio (NOT NULL) |  | Path o URI de almacenamiento físico/nube. |
| documentos_expediente | tamanio_bytes | bigint |  | Opcional (NULL) |  | Peso del archivo en bytes para control de cuotas. |
| documentos_expediente | tipo_documento | character varying | 50 | Opcional (NULL) |  | Categoría interna |
| documentos_expediente | uploaded_at | timestamp without time zone |  | Opcional (NULL) |  | Registro temporal de la carga del archivo |
| documentos_expediente | expediente_id | uuid |  | Obligatorio (NOT NULL) | FK | Llave Foránea (FK). Relación con la tabla expediente. |
| documentos_expediente | uploaded_by | integer |  | Opcional (NULL) |  | D del usuario que realizó la carga |

Tabla: Eventos_calendario

Eventos del calendario personal del usuario. Incluye: título, descripción, fecha inicio/fin, categoría, color y referencia a entidad.

| **Nombre Tabla** | **Nombre Columna** | **Tipo de Dato** | **Longitud Max** | **Requisito** | **Tipo de Llave (PK/FK)** | **Descripción Funcional** |
| --- | --- | --- | --- | --- | --- | --- |
| eventos_calendario | id | integer |  | Obligatorio (NOT NULL) | PK | Llave primaria autoincremental del registro. |
| eventos_calendario | categoria | character varying | 30 | Obligatorio (NOT NULL) |  | Etiqueta de clasificación funcional del evento |
| eventos_calendario | color | character varying | 7 | Opcional (NULL) |  | Código hexadecimal para representación visual en la interfaz |
| eventos_calendario | created_at | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora automática de creación. |
| eventos_calendario | descripcion | text |  | Opcional (NULL) |  | Texto detallado o notas adicionales. |
| eventos_calendario | entidad_id | integer |  | Opcional (NULL) | FK | Llave Foránea (FK). Relación con la tabla entidad. |
| eventos_calendario | entidad_tipo | character varying | 30 | Opcional (NULL) |  | dentificador de categoría o discriminador de tipo de evento |
| eventos_calendario | fecha_fin | timestamp without time zone |  | Opcional (NULL) |  | Punto de control temporal de finalización |
| eventos_calendario | fecha_inicio | timestamp without time zone |  | Obligatorio (NOT NULL) |  | Punto de control temporal de inicio. |
| eventos_calendario | titulo | character varying | 200 | Obligatorio (NOT NULL) |  | Descriptor alfanumérico principal del registro. |
| eventos_calendario | todo_el_dia | boolean |  | Opcional (NULL) |  | Flag booleano para definir eventos sin granularidad horaria |
| eventos_calendario | usuario_id | integer |  | Opcional (NULL) | FK | Llave Foránea (FK). Relación con la tabla usuario. |

Tabla:Expedientes

Tabla principal del sistema. Contiene la información general de cada expediente jurídico, incluyendo su estado, clasificación y responsables.

| **Nombre Tabla** | **Nombre Columna** | **Tipo de Dato** | **Longitud Max** | **Requisito** | **Tipo de Llave (PK/FK)** | **Descripción Funcional** |
| --- | --- | --- | --- | --- | --- | --- |
| expedientes | id | uuid |  | Obligatorio (NOT NULL) | PK | Llave primaria autoincremental del registro. |
| expedientes | abogado_responsable_nombre | character varying | 200 | Opcional (NULL) |  |  |
| expedientes | created_at | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora automática de creación. |
| expedientes | descripcion | text |  | Obligatorio (NOT NULL) |  | Texto detallado o notas adicionales. |
| expedientes | etapa_procesal | character varying | 30 | Obligatorio (NOT NULL) |  | Indica la fase actual del proceso jurídico en la que se encuentra el expediente. |
| expedientes | numero | character varying | 50 | Obligatorio (NOT NULL) |  | Número oficial que identifica de manera única al expediente dentro del sistema. |
| expedientes | organo_jurisdiccional_texto | text |  | Opcional (NULL) |  | Nombre descriptivo del órgano jurisdiccional que conoce el expediente (cuando se registra en formato libre). |
| expedientes | partes | text |  | Opcional (NULL) |  | Contiene la información de las partes involucradas en el expediente (actor, demandado u otros) |
| expedientes | prioridad | character varying | 20 | Obligatorio (NOT NULL) |  | Nivel de urgencia o importancia asignado al expediente para su atención y seguimiento. |
| expedientes | sede | character varying | 150 | Opcional (NULL) |  | Ubicación o sede donde se tramita físicamente o administrativamente el expediente. |
| expedientes | updated_at | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora de la última modificación. |
| expedientes | abogado_responsable_id | integer |  | Opcional (NULL) | FK | Llave Foránea (FK). Relación con la tabla abogado_responsable. |
| expedientes | gerencia_id | integer |  | Obligatorio (NOT NULL) | FK | Llave Foránea (FK). Relación con la tabla gerencia. |
| expedientes | materia_id | integer |  | Obligatorio (NOT NULL) | FK | FK. Rama del derecho (Civil, Mercantil,etc.). |
| expedientes | organo_jurisdiccional_id | integer |  | Opcional (NULL) | FK | Llave Foránea (FK). Relación con la tabla organo_jurisdiccional. |
| expedientes | tipo_expediente_id | integer |  | Opcional (NULL) | FK | Llave Foránea (FK). Relación con la tabla tipo_expediente. |

Tabla:Gerencias

Catálogo institucional que contiene las distintas gerencias o áreas administrativas responsables de los expedientes jurídicos.

| **Nombre Tabla** | **Nombre Columna** | **Tipo de Dato** | **Longitud Max** | **Requisito** | **Tipo de Llave (PK/FK)** | **Descripción Funcional** |
| --- | --- | --- | --- | --- | --- | --- |
| gerencias | id | integer |  | Obligatorio (NOT NULL) | PK | Llave primaria autoincremental del registro. |
| gerencias | activo | boolean |  | Opcional (NULL) |  | Booleano: TRUE=Visible, FALSE=Deshabilitado. |
| gerencias | created_at | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora automática de creación. |
| gerencias | descripcion | text |  | Opcional (NULL) |  | Texto detallado o notas adicionales. |
| gerencias | nombre | character varying | 200 | Obligatorio (NOT NULL) |  | Nombre descriptivo del registro. |
| gerencias | updated_at | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora de la última modificación. |

Tabla:Materias

Catálogo que clasifica los expedientes según su naturaleza jurídica (laboral, administrativo, civil, etc.), facilitando organización y análisis.

| **Nombre Tabla** | **Nombre Columna** | **Tipo de Dato** | **Longitud Max** | **Requisito** | **Tipo de Llave (PK/FK)** | **Descripción Funcional** |
| --- | --- | --- | --- | --- | --- | --- |
| materias | id | integer |  | Obligatorio (NOT NULL) | PK | Llave primaria autoincremental del registro. |
| materias | activo | boolean |  | Opcional (NULL) |  | Booleano: TRUE=Visible, FALSE=Deshabilitado. |
| materias | created_at | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora automática de creación. |
| materias | descripcion | text |  | Opcional (NULL) |  | Texto detallado o notas adicionales. |
| materias | nombre | character varying | 100 | Obligatorio (NOT NULL) |  | Nombre descriptivo del registro. |
| materias | updated_at | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora de la última modificación. |
| materias | gerencia_id | integer |  | Obligatorio (NOT NULL) | FK | Llave Foránea (FK). Relación con la tabla gerencia. |

Tabla:Notificaciones

Administra los avisos generados por el sistema hacia los usuarios sobre eventos relevantes (audiencias, cambios de estado, etc.).

| **Nombre Tabla** | **Nombre Columna** | **Tipo de Dato** | **Longitud Max** | **Requisito** | **Tipo de Llave (PK/FK)** | **Descripción Funcional** |
| --- | --- | --- | --- | --- | --- | --- |
| notificaciones | id | integer |  | Obligatorio (NOT NULL) | PK | Llave primaria autoincremental del registro. |
| notificaciones | created_at | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora automática de creación. |
| notificaciones | entidad_id | character varying | 50 | Opcional (NULL) | FK | Llave Foránea (FK). Relación con la tabla entidad. |
| notificaciones | entidad_tipo | character varying | 30 | Opcional (NULL) |  | Indica el tipo de entidad o módulo del sistema que generó la notificación |
| notificaciones | fecha_leida | timestamp without time zone |  | Opcional (NULL) |  | Registra la fecha y hora en que el usuario abrió o visualizó la notificación. |
| notificaciones | leida | boolean |  | Opcional (NULL) |  | Campo booleano que señala si la notificación ya fue leída por el usuario. |
| notificaciones | mensaje | text |  | Obligatorio (NOT NULL) |  | Contenido principal de la notificación que informa el evento o acción realizada. |
| notificaciones | notificar_en | timestamp without time zone |  | Obligatorio (NOT NULL) |  | Fecha y hora programada para que la notificación sea enviada o mostrada al usuario. |
| notificaciones | prioridad | character varying | 20 | Obligatorio (NOT NULL) |  | Nivel de importancia asignado a la notificación (alta, media, baja), utilizado para gestión y alertas. |
| notificaciones | tipo | character varying | 30 | Obligatorio (NOT NULL) |  | Clasificación de la notificación según su naturaleza (audiencia,termino, recordatorio, ). |
| notificaciones | titulo | character varying | 200 | Obligatorio (NOT NULL) |  | Encabezado breve que resume el contenido o motivo de la notificación. |
| notificaciones | usuario_id | integer |  | Obligatorio (NOT NULL) | FK | Llave Foránea (FK). Relación con la tabla usuario. |

Tablas: Organos_jurisdiccionales

Tribunales, juzgados y órganos judiciales. Incluye tipo (Civil, Penal, Familiar) y sede.

| **Nombre Tabla** | **Nombre Columna** | **Tipo de Dato** | **Longitud Max** | **Requisito** | **Tipo de Llave (PK/FK)** | **Descripción Funcional** |
| --- | --- | --- | --- | --- | --- | --- |
| organos_jurisdiccionales | id | integer |  | Obligatorio (NOT NULL) | PK | Llave primaria autoincremental del registro. |
| organos_jurisdiccionales | activo | boolean |  | Opcional (NULL) |  | Booleano: TRUE=Visible, FALSE=Deshabilitado. |
| organos_jurisdiccionales | created_at | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora automática de creación. |
| organos_jurisdiccionales | nombre | character varying | 300 | Obligatorio (NOT NULL) |  | Nombre descriptivo del registro. |
| organos_jurisdiccionales | sede | character varying | 100 | Opcional (NULL) |  | Indica la ubicación física o territorial donde opera el órgano jurisdiccional, permitiendo identificar su ámbito geográfico de competencia. |
| organos_jurisdiccionales | tipo | character varying | 50 | Opcional (NULL) |  | Especifica la clasificación del órgano jurisdiccional según su naturaleza o jerarquía (por ejemplo, juzgado, tribunal, sala, corte). |
| organos_jurisdiccionales | updated_at | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora de la última modificación. |

Tabla: Recordatorios

Recordatorios personales creados por usuarios. Incluye: fecha, hora, prioridad, detalles y estatus de completado

| **Nombre Tabla** | **Nombre Columna** | **Tipo de Dato** | **Longitud Max** | **Requisito** | **Tipo de Llave (PK/FK)** | **Descripción Funcional** |
| --- | --- | --- | --- | --- | --- | --- |
| recordatorios | id | integer |  | Obligatorio (NOT NULL) | PK | Llave primaria autoincremental del registro. |
| recordatorios | completado | boolean |  | Opcional (NULL) |  | Indica si el recordatorio ya fue atendido o marcado como finalizado por el usuario. |
| recordatorios | created_at | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora automática de creación. |
| recordatorios | detalles | text |  | Opcional (NULL) |  | Contiene la descripción o información adicional relacionada con el recordatorio. |
| recordatorios | fecha_recordatorio | date |  | Obligatorio (NOT NULL) |  | Fecha programada en la que debe cumplirse o atenderse el recordatorio. |
| recordatorios | hora_recordatorio | time without time zone |  | Obligatorio (NOT NULL) |  | Hora específica en la que debe activarse o mostrarse el recordatorio. |
| recordatorios | prioridad | character varying | 20 | Obligatorio (NOT NULL) |  | Nivel de importancia asignado al recordatorio para su gestión (alta, media, baja). |
| recordatorios | titulo | character varying | 200 | Obligatorio (NOT NULL) |  | Nombre o encabezado breve que identifica el recordatorio. |
| recordatorios | updated_at | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora de la última modificación. |
| recordatorios | usuario_id | integer |  | Obligatorio (NOT NULL) | FK | Llave Foránea (FK). Relación con la tabla usuario. |

Tabla: Terminos

Plazos procesales. Contiene: expediente, actuación requerida, fecha de ingreso, fecha de vencimiento, estatus (proyectista, revisión, etc.), prioridad, abogado responsable, observaciones y fecha de presentación

| **Nombre Tabla** | **Nombre Columna** | **Tipo de Dato** | **Longitud Max** | **Requisito** | **Tipo de Llave (PK/FK)** | **Descripción Funcional** |
| --- | --- | --- | --- | --- | --- | --- |
| terminos | id | integer |  | Obligatorio (NOT NULL) | PK | Llave primaria autoincremental del registro. |
| terminos | actuacion | text |  | Obligatorio (NOT NULL) |  | Describe la actuación procesal a la que corresponde el término. |
| terminos | archivo_word | character varying | 500 | Opcional (NULL) |  | Ruta o nombre del archivo Word asociado al término (si aplica). |
| terminos | created_at | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora automática de creación. |
| terminos | estatus_termino | character varying | 30 | Obligatorio (NOT NULL) |  | Estado actual del término |
| terminos | fecha_ingreso | date |  | Opcional (NULL) |  | Fecha en que se registra o ingresa el término al sistema. |
| terminos | fecha_presentacion | date |  | Opcional (NULL) |  | Fecha en que se presenta formalmente la actuación correspondiente. |
| terminos | fecha_vencimiento | date |  | Obligatorio (NOT NULL) |  | Fecha límite fatal para presentar la promoción. |
| terminos | observaciones | text |  | Opcional (NULL) |  | Comentarios u observaciones relevantes. |
| terminos | prioridad | character varying | 20 | Opcional (NULL) |  | Nivel de importancia asignado al término |
| terminos | updated_at | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora de la última modificación. |
| terminos | abogado_responsable_id | integer |  | Opcional (NULL) | FK | Llave Foránea (FK). Relación con la tabla abogado_responsable. |
| terminos | expediente_id | uuid |  | Obligatorio (NOT NULL) | FK | Llave Foránea (FK). Relación con la tabla expediente. |

Tabla: Terminos_presentados

Histórico de términos presentados (sincronizados). Referencia al término original.

| **Nombre Tabla** | **Nombre Columna** | **Tipo de Dato** | **Longitud Max** | **Requisito** | **Tipo de Llave (PK/FK)** | **Descripción Funcional** |
| --- | --- | --- | --- | --- | --- | --- |
| terminos_presentados | id | integer |  | Obligatorio (NOT NULL) | PK | Llave primaria autoincremental del registro. |
| terminos_presentados | acuse_documento | character varying | 500 | Opcional (NULL) |  | Ruta o nombre del archivo que contiene el acuse o comprobante de presentación del término. |
| terminos_presentados | expediente_numero | character varying | 50 | Opcional (NULL) |  | Número identificador del expediente al que corresponde el término presentado. |
| terminos_presentados | fecha_presentacion | date |  | Obligatorio (NOT NULL) |  | Fecha obligatoria en la que se realizó la presentación formal del término. |
| terminos_presentados | sincronizado_at | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora en que el registro fue sincronizado con otro sistema o servicio externo (si aplica). |
| terminos_presentados | termino_id | integer |  | Obligatorio (NOT NULL) | FK | Llave Foránea (FK). Relación con la tabla termino. |

Tabla: Tipos_audiencia

Catálogo de tipos de audiencias ( inicial, intermedia, juicio, etc.)

| **Nombre Tabla** | **Nombre Columna** | **Tipo de Dato** | **Longitud Max** | **Requisito** | **Tipo de Llave (PK/FK)** | **Descripción Funcional** |
| --- | --- | --- | --- | --- | --- | --- |
| tipos_audiencia | id | integer |  | Obligatorio (NOT NULL) | PK | Llave primaria autoincremental del registro. |
| tipos_audiencia | activo | boolean |  | Opcional (NULL) |  | Booleano: TRUE=Visible, FALSE=Deshabilitado. |
| tipos_audiencia | created_at | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora automática de creación. |
| tipos_audiencia | descripcion | text |  | Opcional (NULL) |  | Texto detallado o notas adicionales. |
| tipos_audiencia | nombre | character varying | 100 | Obligatorio (NOT NULL) |  | Nombre descriptivo del registro. |

Tabla: Tipos_expediente

Clasificación de expedientes (Juicio, Recurso, Consulta, etc.).

| **Nombre Tabla** | **Nombre Columna** | **Tipo de Dato** | **Longitud Max** | **Requisito** | **Tipo de Llave (PK/FK)** | **Descripción Funcional** |
| --- | --- | --- | --- | --- | --- | --- |
| tipos_expediente | id | integer |  | Obligatorio (NOT NULL) | PK | Llave primaria autoincremental del registro. |
| tipos_expediente | activo | boolean |  | Opcional (NULL) |  | Booleano: TRUE=Visible, FALSE=Deshabilitado. |
| tipos_expediente | created_at | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora automática de creación. |
| tipos_expediente | descripcion | text |  | Opcional (NULL) |  | Texto detallado o notas adicionales. |
| tipos_expediente | nombre | character varying | 150 | Obligatorio (NOT NULL) |  | Nombre descriptivo del registro. |

Tabla: Usuario_materias

La tabla usuario_materias es una tabla intermedia de tipo relacional cuya función principal es establecer la asociación entre los usuarios del sistema y las materias jurídicas a las que están vinculados

| **Nombre Tabla** | **Nombre Columna** | **Tipo de Dato** | **Longitud Max** | **Requisito** | **Tipo de Llave (PK/FK)** | **Descripción Funcional** |
| --- | --- | --- | --- | --- | --- | --- |
| usuario_materias | usuario_id | integer |  | Obligatorio (NOT NULL) | PK | Llave Foránea (FK). Relación con la tabla usuario. |
| usuario_materias | materia_id | integer |  | Obligatorio (NOT NULL) | FK | Llave Foránea (FK). Relación con la tabla materia. |

Tabla:Usuarios

Almacena la información de las personas que tienen acceso al sistema. Gestiona credenciales, datos personales, estatus y su relación con roles y gerencias, permitiendo el control de acceso y trazabilidad de acciones

| **Nombre Tabla** | **Nombre Columna** | **Tipo de Dato** | **Longitud Max** | **Requisito** | **Tipo de Llave (PK/FK)** | **Descripción Funcional** |
| --- | --- | --- | --- | --- | --- | --- |
| usuarios | id | integer |  | Obligatorio (NOT NULL) | PK | Llave primaria autoincremental del registro. |
| usuarios | activo | boolean |  | Opcional (NULL) |  | Booleano: TRUE=Visible, FALSE=Deshabilitado. |
| usuarios | created_at | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora automática de creación. |
| usuarios | email | character varying | 100 | Obligatorio (NOT NULL) |  | Correo electrónico institucional. Se usa para iniciar sesión. |
| usuarios | nombre_completo | character varying | 200 | Obligatorio (NOT NULL) |  | Nombre completo del usuario registrado en el sistema. |
| usuarios | password_hash | character varying | 255 | Obligatorio (NOT NULL) |  | Contraseña encriptada (no guardar en texto plano). |
| usuarios | rol | character varying | 50 | Obligatorio (NOT NULL) |  | Perfil o tipo de usuario que define sus permisos dentro del sistema |
| usuarios | ultimo_acceso | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora del último ingreso del usuario al sistema. |
| usuarios | updated_at | timestamp without time zone |  | Opcional (NULL) |  | Fecha y hora de la última modificación. |
| usuarios | gerencia_id | integer |  | Opcional (NULL) | FK | Llave Foránea (FK). Relación con la tabla gerencia. |
