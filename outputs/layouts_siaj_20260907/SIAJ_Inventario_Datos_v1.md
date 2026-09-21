# SIAJ: inventario físico de referencia

Esquema local; no certifica esquema desplegado. PK/FK/UNIQUE/CHECK conservados para trazabilidad técnica. No es una plantilla de carga SQL.

## actividad_expedientes

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero 64 bits | No aplica | Sí (BD) | id bigint NOT NULL |
| descripcion | Texto | Sin límite declarado | No | descripcion text |
| fecha_registro | Fecha y hora | No aplica | No | fecha_registro timestamp(6) without time zone |
| tipo_icono | Texto | 255 | No | tipo_icono character varying(255) |
| titulo | Texto | 255 | Sí (BD) | titulo character varying(255) NOT NULL |
| expediente_id | UUID | No aplica | Sí (BD) | expediente_id uuid NOT NULL |
| usuario_id | Entero | No aplica | No | usuario_id integer |

- `actividad_expedientes_pkey PRIMARY KEY (id)`
- `fkjnko884f21s9u9qrnx65c9mt1 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`
- `fkpxpfk1j1efqgo8c0abu3rmj0x FOREIGN KEY (expediente_id) REFERENCES public.expedientes(id)`
## actividad_usuarios

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero 64 bits | No aplica | Sí (BD) | id bigint NOT NULL |
| descripcion | Texto | Sin límite declarado | No | descripcion text |
| fecha_registro | Fecha y hora | No aplica | Sí (BD) | fecha_registro timestamp(6) without time zone NOT NULL |
| tipo_icono | Texto | 50 | No | tipo_icono character varying(50) |
| titulo | Texto | 255 | Sí (BD) | titulo character varying(255) NOT NULL |
| usuario_id | Entero | No aplica | Sí (BD) | usuario_id integer NOT NULL |

- `actividad_usuarios_pkey PRIMARY KEY (id)`
- `fkiwarg06waxi0wm0nmfyaphl7k FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`
## audiencia_contextos

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero 64 bits | No aplica | Sí (BD) | id bigint NOT NULL |
| descripcion | Texto | Sin límite declarado | Sí (BD) | descripcion text NOT NULL |
| numero_expediente_snapshot | Texto | 50 | No | numero_expediente_snapshot character varying(50) |
| organo_jurisdiccional_texto | Texto | Sin límite declarado | No | organo_jurisdiccional_texto text |
| origen | Texto | 30 | Sí (BD) | origen character varying(30) NOT NULL |
| partes | Texto | Sin límite declarado | No | partes text |
| sede | Texto | 150 | No | sede character varying(150) |
| gerencia_id | Entero | No aplica | Sí (BD) | gerencia_id integer NOT NULL |
| materia_id | Entero | No aplica | Sí (BD) | materia_id integer NOT NULL |
| organo_jurisdiccional_id | Entero | No aplica | No | organo_jurisdiccional_id integer |

- `audiencia_contextos_pkey PRIMARY KEY (id)`
- `fkbdu4j8d2ejho98oi08369endx FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id)`
- `fkp1paxjyvpxulyqnukqmd0d13c FOREIGN KEY (organo_jurisdiccional_id) REFERENCES public.organos_jurisdiccionales(id)`
- `fktfcon5wnhwo0ph22a2wt5y6im FOREIGN KEY (materia_id) REFERENCES public.materias(id)`
- `CONSTRAINT audiencia_contextos_origen_check CHECK (((origen)::text = ANY ((ARRAY['EXPEDIENTE'::character varying, 'ASUNTO_UNICO'::character varying])::text[])))`
## audiencias

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero | No aplica | Sí (BD) | id integer NOT NULL |
| acta_documento | Texto | 500 | No | acta_documento character varying(500) |
| created_at | Fecha y hora | No aplica | No | created_at timestamp(6) without time zone |
| es_virtual | Booleano | No aplica | No | es_virtual boolean |
| estatus_audiencia | Texto | 30 | No | estatus_audiencia character varying(30) |
| fecha_audiencia | Fecha | No aplica | Sí (BD) | fecha_audiencia date NOT NULL |
| fecha_desahogo | Fecha | No aplica | No | fecha_desahogo date |
| hora_audiencia | Hora | No aplica | Sí (BD) | hora_audiencia time(6) without time zone NOT NULL |
| observaciones | Texto | Sin límite declarado | No | observaciones text |
| sala_lugar | Texto | Sin límite declarado | No | sala_lugar text |
| updated_at | Fecha y hora | No aplica | No | updated_at timestamp(6) without time zone |
| url_reunion | Texto | Sin límite declarado | No | url_reunion text |
| abogado_comparece_id | Entero | No aplica | No | abogado_comparece_id integer |
| expediente_id | UUID | No aplica | Sí (BD) | expediente_id uuid NOT NULL |
| tipo_audiencia_id | Entero | No aplica | No | tipo_audiencia_id integer |
| contexto_id | Entero 64 bits | No aplica | No | contexto_id bigint |
| usuario_delegado_id | Entero | No aplica | No | usuario_delegado_id integer |

- `audiencias_pkey PRIMARY KEY (id)`
- `uk6yco257iom67jk0pilkinvda3 UNIQUE (contexto_id)`
- `fk4csl8qte7scamolg89kofnal3 FOREIGN KEY (expediente_id) REFERENCES public.expedientes(id)`
- `fk7wchckhcv7ce6x99xekj5e9dx FOREIGN KEY (tipo_audiencia_id) REFERENCES public.tipos_audiencia(id)`
- `fkgsv5padg0wab4wkm8no4mfx59 FOREIGN KEY (usuario_delegado_id) REFERENCES public.usuarios(id)`
- `fkgy6f4qct07n42s9vecon4e792 FOREIGN KEY (contexto_id) REFERENCES public.audiencia_contextos(id)`
- `fkpkiu3klwkc0lr9f9ebbpwsqc1 FOREIGN KEY (abogado_comparece_id) REFERENCES public.usuarios(id)`
- `CONSTRAINT audiencias_estatus_audiencia_check CHECK (((estatus_audiencia)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'CON_ACTA'::character varying, 'CONCLUIDA'::character varying])::text[])))`
## audiencias_desahogadas

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero | No aplica | Sí (BD) | id integer NOT NULL |
| expediente_numero | Texto | 50 | No | expediente_numero character varying(50) |
| fecha_audiencia | Fecha | No aplica | Sí (BD) | fecha_audiencia date NOT NULL |
| fecha_desahogo | Fecha | No aplica | Sí (BD) | fecha_desahogo date NOT NULL |
| hora_audiencia | Hora | No aplica | Sí (BD) | hora_audiencia time(6) without time zone NOT NULL |
| sincronizado_at | Fecha y hora | No aplica | No | sincronizado_at timestamp(6) without time zone |
| audiencia_id | Entero | No aplica | Sí (BD) | audiencia_id integer NOT NULL |

- `audiencias_desahogadas_pkey PRIMARY KEY (id)`
- `fkq40pjs1rrg3mj5qdcl74hawwa FOREIGN KEY (audiencia_id) REFERENCES public.audiencias(id)`
## auditoria_eventos

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero 64 bits | No aplica | Sí (BD) | id bigint NOT NULL |
| accion | Texto | 120 | Sí (BD) | accion character varying(120) NOT NULL |
| codigo_http | Entero | No aplica | No | codigo_http integer |
| descripcion | Texto | Sin límite declarado | No | descripcion text |
| detalle_json | Texto | Sin límite declarado | No | detalle_json text |
| email | Texto | 150 | No | email character varying(150) |
| entidad_id | Texto | 120 | No | entidad_id character varying(120) |
| entidad_tipo | Texto | 80 | No | entidad_tipo character varying(80) |
| fecha | Fecha y hora | No aplica | Sí (BD) | fecha timestamp(6) without time zone NOT NULL |
| ip | Texto | 80 | No | ip character varying(80) |
| metodo_http | Texto | 12 | No | metodo_http character varying(12) |
| modulo | Texto | 80 | No | modulo character varying(80) |
| resultado | Texto | 30 | Sí (BD) | resultado character varying(30) NOT NULL |
| rol | Texto | 50 | No | rol character varying(50) |
| ruta | Texto | 500 | No | ruta character varying(500) |
| session_id | Texto | 255 | No | session_id character varying(255) |
| user_agent | Texto | Sin límite declarado | No | user_agent text |
| usuario_id | Entero | No aplica | No | usuario_id integer |

- `auditoria_eventos_pkey PRIMARY KEY (id)`
- `fk5vp43v3gpt1fcpqr9jg6v5cqf FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`
## auditoria_sesiones

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero 64 bits | No aplica | Sí (BD) | id bigint NOT NULL |
| email | Texto | 150 | No | email character varying(150) |
| estado | Texto | 30 | Sí (BD) | estado character varying(30) NOT NULL |
| fecha_cierre | Fecha y hora | No aplica | No | fecha_cierre timestamp(6) without time zone |
| fecha_inicio | Fecha y hora | No aplica | Sí (BD) | fecha_inicio timestamp(6) without time zone NOT NULL |
| fecha_ultima_actividad | Fecha y hora | No aplica | No | fecha_ultima_actividad timestamp(6) without time zone |
| ip | Texto | 80 | No | ip character varying(80) |
| motivo_cierre | Texto | 80 | No | motivo_cierre character varying(80) |
| rol | Texto | 50 | No | rol character varying(50) |
| session_id | Texto | 255 | Sí (BD) | session_id character varying(255) NOT NULL |
| user_agent | Texto | Sin límite declarado | No | user_agent text |
| usuario_id | Entero | No aplica | No | usuario_id integer |

- `auditoria_sesiones_pkey PRIMARY KEY (id)`
- `fkpxnctjgkcgh29p5ux063wxj06 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`
## bitacora_datos

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero 64 bits | No aplica | Sí (BD) | id bigint NOT NULL |
| campo | Texto | 100 | Sí (BD) | campo character varying(100) NOT NULL |
| fecha | Fecha y hora | No aplica | Sí (BD) | fecha timestamp(6) without time zone NOT NULL |
| valor_anterior | Texto | Sin límite declarado | No | valor_anterior text |
| valor_nuevo | Texto | Sin límite declarado | No | valor_nuevo text |
| expediente_id | UUID | No aplica | Sí (BD) | expediente_id uuid NOT NULL |
| usuario_id | Entero | No aplica | No | usuario_id integer |

- `bitacora_datos_pkey PRIMARY KEY (id)`
- `fka4jw9flw2hdgx2eufguvxbvtk FOREIGN KEY (expediente_id) REFERENCES public.expedientes(id)`
- `fkd2e3cn3q5cbep5g7f8tm3t660 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`
## bk_auth_gerencias_20260727_131223_usuario_gerencias

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| usuario_id | Entero | No aplica | No | usuario_id integer |
| gerencia_id | Entero | No aplica | No | gerencia_id integer |


## bk_auth_gerencias_20260727_131223_usuario_materias

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| usuario_id | Entero | No aplica | No | usuario_id integer |
| materia_id | Entero | No aplica | No | materia_id integer |


## bk_auth_gerencias_20260727_131223_usuarios

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero | No aplica | No | id integer |
| activo | Booleano | No aplica | No | activo boolean |
| created_at | Fecha y hora | No aplica | No | created_at timestamp(6) without time zone |
| email | Texto | 100 | No | email character varying(100) |
| force_password_change | Booleano | No aplica | No | force_password_change boolean |
| nombre_completo | Texto | 200 | No | nombre_completo character varying(200) |
| password_hash | Texto | 255 | No | password_hash character varying(255) |
| password_updated_at | Fecha y hora | No aplica | No | password_updated_at timestamp(6) without time zone |
| rol | Texto | 50 | No | rol character varying(50) |
| ultimo_acceso | Fecha y hora | No aplica | No | ultimo_acceso timestamp(6) without time zone |
| updated_at | Fecha y hora | No aplica | No | updated_at timestamp(6) without time zone |
| gerencia_id | Entero | No aplica | No | gerencia_id integer |
| creado_por_id | Entero | No aplica | No | creado_por_id integer |


## carpetas

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | UUID | No aplica | Sí (BD) | id uuid NOT NULL |
| es_sistema | Booleano | No aplica | Sí (BD) | es_sistema boolean NOT NULL |
| expediente_id | UUID | No aplica | Sí (BD) | expediente_id uuid NOT NULL |
| fecha_creacion | Fecha y hora | No aplica | Sí (BD) | fecha_creacion timestamp(6) without time zone NOT NULL |
| nombre | Texto | 255 | Sí (BD) | nombre character varying(255) NOT NULL |
| orden | Entero | No aplica | Sí (BD) | orden integer NOT NULL |
| carpeta_padre_id | UUID | No aplica | No | carpeta_padre_id uuid |
| usuario_creador_id | Entero | No aplica | No | usuario_creador_id integer |

- `carpetas_pkey PRIMARY KEY (id)`
- `fk7oriagmf0utemgy3ub8iy7myt FOREIGN KEY (usuario_creador_id) REFERENCES public.usuarios(id)`
- `fkel65g9qwjoep94aixiveex7fm FOREIGN KEY (carpeta_padre_id) REFERENCES public.carpetas(id)`
## cat_estados

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero | No aplica | Sí (BD) | id integer NOT NULL |
| activo | Booleano | No aplica | No | activo boolean |
| created_at | Fecha y hora | No aplica | No | created_at timestamp(6) without time zone |
| nombre | Texto | 255 | No | nombre character varying(255) |
| gerencia_id | Entero | No aplica | No | gerencia_id integer |
| usuario_id | Entero | No aplica | No | usuario_id integer |

- `cat_estados_pkey PRIMARY KEY (id)`
- `uknst6hp4h4jox86rou3fpxfmd0 UNIQUE (nombre)`
- `fks243iav8cbu2feurmh2qjaiy6 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`
- `fksvk5hct1ehr5jejuecfr59pih FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id)`
## cat_prioridad

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero | No aplica | Sí (BD) | id integer NOT NULL |
| activo | Booleano | No aplica | No | activo boolean |
| color | Texto | 100 | No | color character varying(100) |
| created_at | Fecha y hora | No aplica | No | created_at timestamp(6) without time zone |
| nombre | Texto | 50 | Sí (BD) | nombre character varying(50) NOT NULL |
| updated_at | Fecha y hora | No aplica | No | updated_at timestamp(6) without time zone |

- `cat_prioridad_pkey PRIMARY KEY (id)`
- `ukp238eocvbwiipffxptyq0uq07 UNIQUE (nombre)`
## cat_tipo_expediente

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero | No aplica | Sí (BD) | id integer NOT NULL |
| activo | Booleano | No aplica | No | activo boolean |
| created_at | Fecha y hora | No aplica | No | created_at timestamp(6) without time zone |
| descripcion | Texto | Sin límite declarado | No | descripcion text |
| nombre | Texto | 100 | Sí (BD) | nombre character varying(100) NOT NULL |
| updated_at | Fecha y hora | No aplica | No | updated_at timestamp(6) without time zone |
| gerencia_id | Entero | No aplica | No | gerencia_id integer |
| usuario_id | Entero | No aplica | No | usuario_id integer |

- `cat_tipo_expediente_pkey PRIMARY KEY (id)`
- `uk9cslxj82xdh4mfg9baolqj7xu UNIQUE (nombre)`
- `fk72utcny5h8l8oeddw7p2ducwb FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`
- `fkk792qaf01pxxgna4hd5yakesh FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id)`
## catalogos_migracion

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero 64 bits | No aplica | Sí (BD) | id bigint NOT NULL |
| estatus | Texto | 20 | Sí (BD) | estatus character varying(20) NOT NULL |
| id_mapeado | Entero 64 bits | No aplica | No | id_mapeado bigint |
| nuevo_valor | Texto | 255 | No | nuevo_valor character varying(255) |
| tipo_catalogo | Texto | 50 | Sí (BD) | tipo_catalogo character varying(50) NOT NULL |
| valor_original | Texto | 255 | Sí (BD) | valor_original character varying(255) NOT NULL |
| lote_id | Entero 64 bits | No aplica | Sí (BD) | lote_id bigint NOT NULL |

- `catalogos_migracion_pkey PRIMARY KEY (id)`
- `fkbcm7hlj12jkry23uud6vp5b7q FOREIGN KEY (lote_id) REFERENCES public.lotes_migracion(id)`
## colaboradores_expediente

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero | No aplica | Sí (BD) | id integer NOT NULL |
| fecha_expiracion | Fecha y hora | No aplica | Sí (BD) | fecha_expiracion timestamp(6) without time zone NOT NULL |
| motivo | Texto | Sin límite declarado | No | motivo text |
| permiso_nivel | Texto | 255 | No | permiso_nivel character varying(255) |
| expediente_id | UUID | No aplica | Sí (BD) | expediente_id uuid NOT NULL |
| usuario_id | Entero | No aplica | Sí (BD) | usuario_id integer NOT NULL |

- `colaboradores_expediente_pkey PRIMARY KEY (id)`
- `fkfmyo4j16g3shpn1jqc846sril FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`
- `fkmajsh6pilw2o7d2lusf1yfewt FOREIGN KEY (expediente_id) REFERENCES public.expedientes(id)`
## comentarios

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero | No aplica | Sí (BD) | id integer NOT NULL |
| comentario | Texto | Sin límite declarado | Sí (BD) | comentario text NOT NULL |
| created_at | Fecha y hora | No aplica | No | created_at timestamp(6) without time zone |
| entidad_id | Texto | 50 | Sí (BD) | entidad_id character varying(50) NOT NULL |
| entidad_tipo | Texto | 30 | Sí (BD) | entidad_tipo character varying(30) NOT NULL |
| etapa_procesal | Texto | 30 | No | etapa_procesal character varying(30) |
| usuario_nombre | Texto | 255 | No | usuario_nombre character varying(255) |
| usuario_id | Entero | No aplica | No | usuario_id integer |

- `comentarios_pkey PRIMARY KEY (id)`
- `fkdts62yj83qe3k748cgcjvm48r FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`
- `CONSTRAINT comentarios_etapa_procesal_check CHECK (((etapa_procesal)::text = ANY ((ARRAY['TRAMITE'::character varying, 'LAUDO'::character varying, 'FIRME'::character varying, 'CONCLUIDO'::character varying])::text[])))`
## configuraciones_alerta

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero | No aplica | Sí (BD) | id integer NOT NULL |
| activa | Booleano | No aplica | Sí (BD) | activa boolean NOT NULL |
| asunto_id | Texto | 100 | No | asunto_id character varying(100) |
| asunto_tipo | Texto | 30 | No | asunto_tipo character varying(30) |
| created_at | Fecha y hora | No aplica | Sí (BD) | created_at timestamp(6) without time zone NOT NULL |
| dias_anticipacion | Entero | No aplica | Sí (BD) | dias_anticipacion integer NOT NULL |
| nivel | Texto | 30 | Sí (BD) | nivel character varying(30) NOT NULL |
| obligatoria | Booleano | No aplica | Sí (BD) | obligatoria boolean NOT NULL |
| tipo_evento | Texto | 30 | Sí (BD) | tipo_evento character varying(30) NOT NULL |
| updated_at | Fecha y hora | No aplica | Sí (BD) | updated_at timestamp(6) without time zone NOT NULL |
| gerencia_id | Entero | No aplica | No | gerencia_id integer |
| usuario_id | Entero | No aplica | No | usuario_id integer |

- `configuraciones_alerta_pkey PRIMARY KEY (id)`
- `fkjuixqrp11exyu0hxda62mjaly FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id)`
- `fkp2ofjofsj1ohidlux3q1jylxr FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`
- `CONSTRAINT configuraciones_alerta_nivel_check CHECK (((nivel)::text = ANY ((ARRAY['SISTEMA'::character varying, 'GERENCIA'::character varying, 'USUARIO'::character varying, 'ASUNTO'::character varying])::text[]))),`
- `CONSTRAINT configuraciones_alerta_tipo_evento_check CHECK (((tipo_evento)::text = ANY ((ARRAY['TERMINO'::character varying, 'AUDIENCIA'::character varying])::text[])))`
## configuraciones_semaforo

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero | No aplica | Sí (BD) | id integer NOT NULL |
| amarillo_hasta | Entero | No aplica | Sí (BD) | amarillo_hasta integer NOT NULL |
| created_at | Fecha y hora | No aplica | Sí (BD) | created_at timestamp(6) without time zone NOT NULL |
| nivel | Texto | 30 | Sí (BD) | nivel character varying(30) NOT NULL |
| rojo_hasta | Entero | No aplica | Sí (BD) | rojo_hasta integer NOT NULL |
| tipo_evento | Texto | 30 | Sí (BD) | tipo_evento character varying(30) NOT NULL |
| updated_at | Fecha y hora | No aplica | Sí (BD) | updated_at timestamp(6) without time zone NOT NULL |
| gerencia_id | Entero | No aplica | No | gerencia_id integer |

- `configuraciones_semaforo_pkey PRIMARY KEY (id)`
- `fkpncb84gi9iflcalxgkyf6chdk FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id)`
- `CONSTRAINT configuraciones_semaforo_nivel_check CHECK (((nivel)::text = ANY ((ARRAY['SISTEMA'::character varying, 'GERENCIA'::character varying, 'USUARIO'::character varying, 'ASUNTO'::character varying])::text[]))),`
- `CONSTRAINT configuraciones_semaforo_tipo_evento_check CHECK (((tipo_evento)::text = ANY ((ARRAY['TERMINO'::character varying, 'AUDIENCIA'::character varying])::text[])))`
## documentos

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | UUID | No aplica | Sí (BD) | id uuid NOT NULL |
| created_at | Fecha y hora | No aplica | No | created_at timestamp(6) without time zone |
| descripcion | Texto | Sin límite declarado | No | descripcion text |
| expediente_id | UUID | No aplica | Sí (BD) | expediente_id uuid NOT NULL |
| fecha_subida | Fecha y hora | No aplica | Sí (BD) | fecha_subida timestamp(6) without time zone NOT NULL |
| nombre_archivo | Texto | 255 | Sí (BD) | nombre_archivo character varying(255) NOT NULL |
| nombre_original | Texto | 255 | Sí (BD) | nombre_original character varying(255) NOT NULL |
| ruta_archivo | Texto | 500 | Sí (BD) | ruta_archivo character varying(500) NOT NULL |
| tamanio_bytes | Entero 64 bits | No aplica | Sí (BD) | tamanio_bytes bigint NOT NULL |
| tipo_mime | Texto | 100 | No | tipo_mime character varying(100) |
| updated_at | Fecha y hora | No aplica | No | updated_at timestamp(6) without time zone |
| carpeta_id | UUID | No aplica | No | carpeta_id uuid |
| usuario_subida_id | Entero | No aplica | No | usuario_subida_id integer |
| carpeta_original_id | UUID | No aplica | No | carpeta_original_id uuid |
| carpeta_original_ruta | Texto | 1024 | No | carpeta_original_ruta character varying(1024) |
| eliminado | Booleano | No aplica | Sí (BD) | eliminado boolean DEFAULT false NOT NULL |
| eliminado_definitivo | Booleano | No aplica | Sí (BD) | eliminado_definitivo boolean DEFAULT false NOT NULL |
| fecha_eliminacion | Fecha y hora | No aplica | No | fecha_eliminacion timestamp(6) without time zone |
| fecha_eliminacion_definitiva | Fecha y hora | No aplica | No | fecha_eliminacion_definitiva timestamp(6) without time zone |
| usuario_eliminacion_id | Entero | No aplica | No | usuario_eliminacion_id integer |

- `documentos_pkey PRIMARY KEY (id)`
- `fkapnjaa6i0jmcmp0adsrclumti FOREIGN KEY (usuario_eliminacion_id) REFERENCES public.usuarios(id)`
- `fki77q0aw3d7cix0fty95p5halt FOREIGN KEY (carpeta_id) REFERENCES public.carpetas(id)`
- `fkou5fwjxrh4strb85eehbd3535 FOREIGN KEY (usuario_subida_id) REFERENCES public.usuarios(id)`
## documentos_expediente

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero | No aplica | Sí (BD) | id integer CONSTRAINT documentos_expediente_id_not_null1 NOT NULL |
| descripcion | Texto | Sin límite declarado | No | descripcion text |
| mime_type | Texto | 100 | No | mime_type character varying(100) |
| nombre_archivo | Texto | 300 | Sí (BD) | nombre_archivo character varying(300) NOT NULL |
| ruta_archivo | Texto | 500 | Sí (BD) | ruta_archivo character varying(500) NOT NULL |
| tamanio_bytes | Entero 64 bits | No aplica | No | tamanio_bytes bigint |
| tipo_documento | Texto | 50 | No | tipo_documento character varying(50) |
| uploaded_at | Fecha y hora | No aplica | No | uploaded_at timestamp(6) without time zone |
| expediente_id | UUID | No aplica | Sí (BD) | expediente_id uuid NOT NULL |
| uploaded_by | Entero | No aplica | No | uploaded_by integer |

- `documentos_expediente_pkey PRIMARY KEY (id)`
- `fk93ari8ojm8gv9htiv787wbxms FOREIGN KEY (expediente_id) REFERENCES public.expedientes(id)`
- `fkmnmduqxe42khc46ssomlsi4ki FOREIGN KEY (uploaded_by) REFERENCES public.usuarios(id)`
## eventos_calendario

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero | No aplica | Sí (BD) | id integer NOT NULL |
| categoria | Texto | 30 | Sí (BD) | categoria character varying(30) NOT NULL |
| color | Texto | 7 | No | color character varying(7) |
| created_at | Fecha y hora | No aplica | No | created_at timestamp(6) without time zone |
| descripcion | Texto | Sin límite declarado | No | descripcion text |
| entidad_id | Entero | No aplica | No | entidad_id integer |
| entidad_tipo | Texto | 30 | No | entidad_tipo character varying(30) |
| fecha_fin | Fecha y hora | No aplica | No | fecha_fin timestamp(6) without time zone |
| fecha_inicio | Fecha y hora | No aplica | Sí (BD) | fecha_inicio timestamp(6) without time zone NOT NULL |
| titulo | Texto | 200 | Sí (BD) | titulo character varying(200) NOT NULL |
| todo_el_dia | Booleano | No aplica | No | todo_el_dia boolean |
| usuario_id | Entero | No aplica | No | usuario_id integer |

- `eventos_calendario_pkey PRIMARY KEY (id)`
- `fkkerk9013d75pe0retvvpoali4 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`
## expedientes

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | UUID | No aplica | Sí (BD) | id uuid NOT NULL |
| abogado_responsable_nombre | Texto | 200 | No | abogado_responsable_nombre character varying(200) |
| created_at | Fecha y hora | No aplica | No | created_at timestamp(6) without time zone |
| descripcion | Texto | Sin límite declarado | Sí (BD) | descripcion text NOT NULL |
| etapa_procesal | Texto | 30 | Sí (BD) | etapa_procesal character varying(30) NOT NULL |
| numero | Texto | 50 | Sí (BD) | numero character varying(50) NOT NULL |
| organo_jurisdiccional_texto | Texto | Sin límite declarado | No | organo_jurisdiccional_texto text |
| partes | Texto | Sin límite declarado | No | partes text |
| sede | Texto | 150 | No | sede character varying(150) |
| updated_at | Fecha y hora | No aplica | No | updated_at timestamp(6) without time zone |
| abogado_responsable_id | Entero | No aplica | No | abogado_responsable_id integer |
| gerencia_id | Entero | No aplica | Sí (BD) | gerencia_id integer NOT NULL |
| materia_id | Entero | No aplica | Sí (BD) | materia_id integer NOT NULL |
| organo_jurisdiccional_id | Entero | No aplica | No | organo_jurisdiccional_id integer |
| prioridad_id | Entero | No aplica | No | prioridad_id integer |
| tipo_expediente_id | Entero | No aplica | No | tipo_expediente_id integer |
| es_temporal | Booleano | No aplica | No | es_temporal boolean |
| etapa_catalogo_id | Entero | No aplica | No | Sólo JPA; no existe en el dump. Etapa de la materia del expediente. |

- `expedientes_pkey PRIMARY KEY (id)`
- `fk4ko1kuyjwubjuk8pqc6uj7nkl FOREIGN KEY (organo_jurisdiccional_id) REFERENCES public.organos_jurisdiccionales(id)`
- `fk58skvtk8tcu7wlxc6q6bd9kbt FOREIGN KEY (materia_id) REFERENCES public.materias(id)`
- `fkbichuxp9f2p9t665qhevw9hv FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id)`
- `fkjd2xfb8uvwn3bfcsxc8vy3clj FOREIGN KEY (prioridad_id) REFERENCES public.cat_prioridad(id)`
- `fkof8a43swfcvf2dv5mo6q5thd6 FOREIGN KEY (abogado_responsable_id) REFERENCES public.usuarios(id)`
- `fkq1uvxy2ws1qxvxrgqovf9tq1s FOREIGN KEY (tipo_expediente_id) REFERENCES public.cat_tipo_expediente(id)`
- `CONSTRAINT expedientes_etapa_procesal_check CHECK (((etapa_procesal)::text = ANY ((ARRAY['TRAMITE'::character varying, 'LAUDO'::character varying, 'FIRME'::character varying, 'CONCLUIDO'::character varying])::text[])))`
## gerencias

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero | No aplica | Sí (BD) | id integer NOT NULL |
| activo | Booleano | No aplica | No | activo boolean |
| created_at | Fecha y hora | No aplica | No | created_at timestamp(6) without time zone |
| descripcion | Texto | Sin límite declarado | No | descripcion text |
| nombre | Texto | 200 | Sí (BD) | nombre character varying(200) NOT NULL |
| updated_at | Fecha y hora | No aplica | No | updated_at timestamp(6) without time zone |

- `gerencias_pkey PRIMARY KEY (id)`
- `uk5nv105r1e4j3r6hrfxxa4fxeg UNIQUE (nombre)`
## lotes_migracion

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero 64 bits | No aplica | Sí (BD) | id bigint NOT NULL |
| estatus | Texto | 20 | Sí (BD) | estatus character varying(20) NOT NULL |
| fecha_creacion | Fecha y hora | No aplica | Sí (BD) | fecha_creacion timestamp(6) without time zone NOT NULL |
| paso_actual | Entero | No aplica | Sí (BD) | paso_actual integer NOT NULL |
| gerencia_global_id | Entero | No aplica | No | gerencia_global_id integer |
| usuario_responsable_id | Entero | No aplica | No | usuario_responsable_id integer |

- `lotes_migracion_pkey PRIMARY KEY (id)`
- `fkml1x7t5vox3isbj11ai4dvx8b FOREIGN KEY (gerencia_global_id) REFERENCES public.gerencias(id)`
- `fkt3ttkjr7t5lfi88ketoe4osee FOREIGN KEY (usuario_responsable_id) REFERENCES public.usuarios(id)`
## materias

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero | No aplica | Sí (BD) | id integer NOT NULL |
| activo | Booleano | No aplica | No | activo boolean |
| created_at | Fecha y hora | No aplica | No | created_at timestamp(6) without time zone |
| descripcion | Texto | Sin límite declarado | No | descripcion text |
| nombre | Texto | 100 | Sí (BD) | nombre character varying(100) NOT NULL |
| updated_at | Fecha y hora | No aplica | No | updated_at timestamp(6) without time zone |
| gerencia_id | Entero | No aplica | Sí (BD) | gerencia_id integer NOT NULL |
| usuario_id | Entero | No aplica | No | usuario_id integer |

- `materias_pkey PRIMARY KEY (id)`
- `uk35p3esp62g4obf0xvgxkrgh2e UNIQUE (nombre, gerencia_id)`
- `fk88y1xy72ng7x0aa9orpy942p7 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`
- `fkkf13pj696ynk5lfcetip9d5oy FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id)`
## notificaciones

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero | No aplica | Sí (BD) | id integer NOT NULL |
| created_at | Fecha y hora | No aplica | No | created_at timestamp(6) without time zone |
| entidad_id | Texto | 50 | No | entidad_id character varying(50) |
| entidad_tipo | Texto | 30 | No | entidad_tipo character varying(30) |
| fecha_leida | Fecha y hora | No aplica | No | fecha_leida timestamp(6) without time zone |
| leida | Booleano | No aplica | No | leida boolean |
| mensaje | Texto | Sin límite declarado | Sí (BD) | mensaje text NOT NULL |
| notificar_en | Fecha y hora | No aplica | Sí (BD) | notificar_en timestamp(6) without time zone NOT NULL |
| tipo | Texto | 30 | Sí (BD) | tipo character varying(30) NOT NULL |
| titulo | Texto | 200 | Sí (BD) | titulo character varying(200) NOT NULL |
| prioridad_id | Entero | No aplica | No | prioridad_id integer |
| usuario_id | Entero | No aplica | Sí (BD) | usuario_id integer NOT NULL |
| evento | Texto | 80 | Sí (BD) | evento character varying(80) NOT NULL |
| titulo2 | Texto | 200 | No | titulo2 character varying(200) |

- `notificaciones_pkey PRIMARY KEY (id)`
- `fk1mxbjb81ft61gwlh0kabubndc FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`
- `fkhedeo9jm98y2dq7nbpp9fg7q1 FOREIGN KEY (prioridad_id) REFERENCES public.cat_prioridad(id)`
## organos_jurisdiccionales

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero | No aplica | Sí (BD) | id integer NOT NULL |
| activo | Booleano | No aplica | No | activo boolean |
| created_at | Fecha y hora | No aplica | No | created_at timestamp(6) without time zone |
| nombre | Texto | 300 | Sí (BD) | nombre character varying(300) NOT NULL |
| sede | Texto | 100 | No | sede character varying(100) |
| tipo | Texto | 50 | No | tipo character varying(50) |
| updated_at | Fecha y hora | No aplica | No | updated_at timestamp(6) without time zone |
| gerencia_id | Entero | No aplica | No | gerencia_id integer |
| usuario_id | Entero | No aplica | No | usuario_id integer |

- `organos_jurisdiccionales_pkey PRIMARY KEY (id)`
- `ukmc16d9k9cfsrevrlbjdt9cctb UNIQUE (nombre)`
- `fk139ctye6aj13npxjakubnphj5 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`
- `fkp3dc7r1182p73giiehvc1a3kw FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id)`
## password_reset_tokens

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero 64 bits | No aplica | Sí (BD) | id bigint NOT NULL |
| activo | Booleano | No aplica | Sí (BD) | activo boolean NOT NULL |
| email | Texto | 150 | Sí (BD) | email character varying(150) NOT NULL |
| fecha_creacion | Fecha y hora | No aplica | Sí (BD) | fecha_creacion timestamp(6) without time zone NOT NULL |
| fecha_expiracion | Fecha y hora | No aplica | Sí (BD) | fecha_expiracion timestamp(6) without time zone NOT NULL |
| fecha_uso | Fecha y hora | No aplica | No | fecha_uso timestamp(6) without time zone |
| token | Texto | 120 | Sí (BD) | token character varying(120) NOT NULL |
| usado | Booleano | No aplica | Sí (BD) | usado boolean NOT NULL |
| usuario_id | Entero | No aplica | Sí (BD) | usuario_id integer NOT NULL |

- `idx_password_reset_token UNIQUE (token)`
- `password_reset_tokens_pkey PRIMARY KEY (id)`
- `fkp3log76r68owjybas53j8jaig FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`
## preferencias_notificacion

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero | No aplica | Sí (BD) | id integer NOT NULL |
| notificar_nueva_audiencia | Booleano | No aplica | No | notificar_nueva_audiencia boolean |
| notificar_nuevo_termino | Booleano | No aplica | No | notificar_nuevo_termino boolean |
| notificar_reasignacion | Booleano | No aplica | No | notificar_reasignacion boolean |
| notificar_termino_vencer | Booleano | No aplica | No | notificar_termino_vencer boolean |
| usuario_id | Entero | No aplica | Sí (BD) | usuario_id integer NOT NULL |

- `preferencias_notificacion_pkey PRIMARY KEY (id)`
- `uke1fpwe1mmm24nh837d91ymwnh UNIQUE (usuario_id)`
- `fk401s19rwxg52aeg4lnbgdno3d FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`
## preferencias_notificacion_evento

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero | No aplica | Sí (BD) | id integer NOT NULL |
| evento | Texto | 80 | Sí (BD) | evento character varying(80) NOT NULL |
| habilitada | Booleano | No aplica | Sí (BD) | habilitada boolean NOT NULL |
| usuario_id | Entero | No aplica | Sí (BD) | usuario_id integer NOT NULL |

- `preferencias_notificacion_evento_pkey PRIMARY KEY (id)`
- `uk_pref_notif_evento_usuario UNIQUE (usuario_id, evento)`
- `fkq824i3pvktkmtaa0m7l1g1ry0 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`
- `CONSTRAINT preferencias_notificacion_evento_evento_check CHECK (((evento)::text = ANY (ARRAY['EXPEDIENTE_NUEVO'::text, 'EXPEDIENTE_REASIGNADO'::text, 'EXPEDIENTE_OBSERVACION_NUEVA'::text, 'TERMINO_ASIGNADO'::text, 'TERMINO_NUEVO'::text, 'TERMINO_CAMBIO_ETAPA'::text, 'TERMINO_VALIDADO_CONCLUIDO'::text, 'TERMINO_RECORDATORIO'::text, 'AUDIENCIA_NUEVA'::text, 'AUDIENCIA_COMPARECENCIA_ASIGNADA'::text, 'AUDIENCIA_DELEGADA'::text, 'AUDIENCIA_RECORDATORIO'::text, 'AUDIENCIA_ACTA_DISPONIBLE'::text, 'AUDIENCIA_VALIDADA_CONCLUIDA'::text, 'SEGURIDAD_PASSWORD_POR_VENCER'::text, 'TAREA_RECORDATORIO'::text])))`
## push_subscriptions

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero | No aplica | Sí (BD) | id integer NOT NULL |
| usuario_id | Entero | No aplica | Sí (BD) | usuario_id integer NOT NULL |
| endpoint | Texto | Sin límite declarado | Sí (BD) | endpoint text NOT NULL |
| p256dh | Texto | 255 | Sí (BD) | p256dh character varying(255) NOT NULL |
| auth | Texto | 255 | Sí (BD) | auth character varying(255) NOT NULL |
| device_id | Texto | 255 | No | device_id character varying(255) |
| created_at | Fecha y hora | No aplica | No | created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP |
| session_id | Texto | 255 | No | session_id character varying(255) |

- `push_subscriptions_pkey PRIMARY KEY (id)`
- `push_subscriptions_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE CASCADE`
## recordatorios

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero | No aplica | Sí (BD) | id integer NOT NULL |
| completado | Booleano | No aplica | No | completado boolean |
| created_at | Fecha y hora | No aplica | No | created_at timestamp(6) without time zone |
| detalles | Texto | Sin límite declarado | No | detalles text |
| fecha_recordatorio | Fecha | No aplica | Sí (BD) | fecha_recordatorio date NOT NULL |
| hora_recordatorio | Hora | No aplica | Sí (BD) | hora_recordatorio time(6) without time zone NOT NULL |
| titulo | Texto | 200 | Sí (BD) | titulo character varying(200) NOT NULL |
| updated_at | Fecha y hora | No aplica | No | updated_at timestamp(6) without time zone |
| prioridad_id | Entero | No aplica | No | prioridad_id integer |
| usuario_id | Entero | No aplica | Sí (BD) | usuario_id integer NOT NULL |

- `recordatorios_pkey PRIMARY KEY (id)`
- `fk476hi0yoplmls066xtrc5otb FOREIGN KEY (prioridad_id) REFERENCES public.cat_prioridad(id)`
- `fkc52iceqg5fu1wn3jrijliai38 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`
## registros_migracion

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero 64 bits | No aplica | Sí (BD) | id bigint NOT NULL |
| created_at | Fecha y hora | No aplica | No | created_at timestamp(6) without time zone |
| estatus | Texto | 20 | Sí (BD) | estatus character varying(20) NOT NULL |
| mensaje_error | Texto | Sin límite declarado | No | mensaje_error text |
| numero_expediente_extraido | Texto | 100 | No | numero_expediente_extraido character varying(100) |
| payload_json | Texto | Sin límite declarado | Sí (BD) | payload_json text NOT NULL |
| processed_at | Fecha y hora | No aplica | No | processed_at timestamp(6) without time zone |
| tipo_dato | Texto | 50 | Sí (BD) | tipo_dato character varying(50) NOT NULL |
| lote_id | Entero 64 bits | No aplica | No | lote_id bigint |

- `registros_migracion_pkey PRIMARY KEY (id)`
- `fk5dyhaybpntdsmofm5ek0c55f4 FOREIGN KEY (lote_id) REFERENCES public.lotes_migracion(id)`
## termino_archivos

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero 64 bits | No aplica | Sí (BD) | id bigint NOT NULL |
| created_at | Fecha y hora | No aplica | Sí (BD) | created_at timestamp(6) without time zone NOT NULL |
| etapa | Texto | 30 | Sí (BD) | etapa character varying(30) NOT NULL |
| nombre_original | Texto | 255 | Sí (BD) | nombre_original character varying(255) NOT NULL |
| ruta_archivo | Texto | 700 | Sí (BD) | ruta_archivo character varying(700) NOT NULL |
| tamanio_bytes | Entero 64 bits | No aplica | No | tamanio_bytes bigint |
| tipo_documento | Texto | 20 | Sí (BD) | tipo_documento character varying(20) NOT NULL |
| tipo_mime | Texto | 120 | No | tipo_mime character varying(120) |
| termino_id | Entero | No aplica | Sí (BD) | termino_id integer NOT NULL |
| usuario_subida_id | Entero | No aplica | No | usuario_subida_id integer |

- `termino_archivos_pkey PRIMARY KEY (id)`
- `fkhgdafpg9sqy4788ma60xfixbx FOREIGN KEY (termino_id) REFERENCES public.terminos(id)`
- `fklrxl92lqj7h66ktvsk2bdw68u FOREIGN KEY (usuario_subida_id) REFERENCES public.usuarios(id)`
- `CONSTRAINT termino_archivos_etapa_check CHECK (((etapa)::text = ANY ((ARRAY['PROYECTISTA'::character varying, 'REVISION'::character varying, 'GERENCIA'::character varying, 'DIRECCION'::character varying, 'LIBERADO'::character varying, 'PRESENTADO'::character varying, 'CONCLUIDO'::character varying])::text[]))),`
- `CONSTRAINT termino_archivos_tipo_documento_check CHECK (((tipo_documento)::text = ANY ((ARRAY['BORRADOR'::character varying, 'ACUSE'::character varying])::text[])))`
## termino_contextos

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero 64 bits | No aplica | Sí (BD) | id bigint NOT NULL |
| descripcion | Texto | Sin límite declarado | Sí (BD) | descripcion text NOT NULL |
| numero_expediente_snapshot | Texto | 50 | No | numero_expediente_snapshot character varying(50) |
| organo_jurisdiccional_texto | Texto | Sin límite declarado | No | organo_jurisdiccional_texto text |
| origen | Texto | 30 | Sí (BD) | origen character varying(30) NOT NULL |
| partes | Texto | Sin límite declarado | No | partes text |
| sede | Texto | 150 | No | sede character varying(150) |
| gerencia_id | Entero | No aplica | Sí (BD) | gerencia_id integer NOT NULL |
| materia_id | Entero | No aplica | Sí (BD) | materia_id integer NOT NULL |
| organo_jurisdiccional_id | Entero | No aplica | No | organo_jurisdiccional_id integer |

- `termino_contextos_pkey PRIMARY KEY (id)`
- `fk6bqlx8xoy5y68c306ugj3x9it FOREIGN KEY (materia_id) REFERENCES public.materias(id)`
- `fkhcuyhk60prwmu8wdx5lq7dw8t FOREIGN KEY (organo_jurisdiccional_id) REFERENCES public.organos_jurisdiccionales(id)`
- `fkiyg3nqpnf53flv1ovxvp94t5h FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id)`
- `CONSTRAINT termino_contextos_origen_check CHECK (((origen)::text = ANY ((ARRAY['EXPEDIENTE'::character varying, 'ASUNTO_UNICO'::character varying])::text[])))`
## terminos

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero | No aplica | Sí (BD) | id integer NOT NULL |
| acuse | Texto | 500 | No | acuse character varying(500) |
| acuse_nombre | Texto | 500 | No | acuse_nombre character varying(500) |
| asunto | Texto | Sin límite declarado | No | asunto text |
| created_at | Fecha y hora | No aplica | No | created_at timestamp(6) without time zone |
| estatus_termino | Texto | 30 | Sí (BD) | estatus_termino character varying(30) NOT NULL |
| fecha_ingreso | Fecha | No aplica | No | fecha_ingreso date |
| fecha_presentacion | Fecha | No aplica | No | fecha_presentacion date |
| fecha_vencimiento | Fecha | No aplica | Sí (BD) | fecha_vencimiento date NOT NULL |
| observaciones | Texto | Sin límite declarado | No | observaciones text |
| updated_at | Fecha y hora | No aplica | No | updated_at timestamp(6) without time zone |
| abogado_responsable_id | Entero | No aplica | No | abogado_responsable_id integer |
| expediente_id | UUID | No aplica | No | expediente_id uuid |
| prioridad_id | Entero | No aplica | No | prioridad_id integer |
| fecha_creacion | Fecha | No aplica | No | fecha_creacion date |
| contexto_id | Entero 64 bits | No aplica | No | contexto_id bigint |
| archivo_word | Texto | 500 | No | archivo_word character varying(500) |
| prioridad | Texto | 20 | No | prioridad character varying(20) |
| tipo_termino_id | Entero | No aplica | No | Sólo JPA; no existe en el dump. Validar despliegue y cálculo antes de importar. |
| es_tiempo_dinamico | Booleano | No aplica | No | Sólo JPA; no existe en el dump. Validar despliegue y cálculo antes de importar. |
| tipo_dias_calculado | Texto | 20 | No | Sólo JPA; no existe en el dump. Validar despliegue y cálculo antes de importar. |
| desglose_dias_texto | Texto | Sin límite declarado | No | Sólo JPA; no existe en el dump. Validar despliegue y cálculo antes de importar. |

- `terminos_pkey PRIMARY KEY (id)`
- `ukjcdu631eit28n3wdtg3y1elgb UNIQUE (contexto_id)`
- `fk2dw1fq3hmoqxkj4o25awskock FOREIGN KEY (contexto_id) REFERENCES public.termino_contextos(id)`
- `fk4gjcyyda149hmwyj6v7fjcot2 FOREIGN KEY (prioridad_id) REFERENCES public.cat_prioridad(id)`
- `fkcoe42fxbdjqjek2ks0ghkmowd FOREIGN KEY (expediente_id) REFERENCES public.expedientes(id)`
- `fko59hv5s9ul7bqg35p4oanxlkr FOREIGN KEY (abogado_responsable_id) REFERENCES public.usuarios(id)`
- `CONSTRAINT terminos_estatus_termino_check CHECK (((estatus_termino)::text = ANY ((ARRAY['PROYECTISTA'::character varying, 'REVISION'::character varying, 'GERENCIA'::character varying, 'DIRECCION'::character varying, 'LIBERADO'::character varying, 'PRESENTADO'::character varying, 'CONCLUIDO'::character varying])::text[]))),`
- `CONSTRAINT terminos_prioridad_check CHECK (((prioridad)::text = ANY (ARRAY[('ALTA'::character varying)::text, ('MEDIA'::character varying)::text, ('BAJA'::character varying)::text])))`
## terminos_presentados

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero | No aplica | Sí (BD) | id integer NOT NULL |
| acuse_documento | Texto | 500 | No | acuse_documento character varying(500) |
| expediente_numero | Texto | 50 | No | expediente_numero character varying(50) |
| fecha_presentacion | Fecha | No aplica | Sí (BD) | fecha_presentacion date NOT NULL |
| sincronizado_at | Fecha y hora | No aplica | No | sincronizado_at timestamp(6) without time zone |
| termino_id | Entero | No aplica | Sí (BD) | termino_id integer NOT NULL |

- `terminos_presentados_pkey PRIMARY KEY (id)`
- `fk1brocrvr9fxbfek662npjjn5l FOREIGN KEY (termino_id) REFERENCES public.terminos(id)`
## tipos_audiencia

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero | No aplica | Sí (BD) | id integer NOT NULL |
| activo | Booleano | No aplica | No | activo boolean |
| created_at | Fecha y hora | No aplica | No | created_at timestamp(6) without time zone |
| descripcion | Texto | Sin límite declarado | No | descripcion text |
| nombre | Texto | 100 | Sí (BD) | nombre character varying(100) NOT NULL |
| gerencia_id | Entero | No aplica | No | gerencia_id integer |
| usuario_id | Entero | No aplica | No | usuario_id integer |

- `tipos_audiencia_pkey PRIMARY KEY (id)`
- `ukhwdw390hju3jpv9r2pvbn7eat UNIQUE (nombre)`
- `fk1kml027csdf02fbsgarvpaacn FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`
- `fklhw4n232t69p6wb8mkmubvkl6 FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id)`
## usuario_gerencias

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| usuario_id | Entero | No aplica | Sí (BD) | usuario_id integer NOT NULL |
| gerencia_id | Entero | No aplica | Sí (BD) | gerencia_id integer NOT NULL |

- `usuario_gerencias_pkey PRIMARY KEY (usuario_id, gerencia_id)`
- `fkai89eqag1l6q65v4f2w54b2gd FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id)`
- `fkk2iku7uhxeu1b8r5j2qo8kl82 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`
## usuario_materias

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| usuario_id | Entero | No aplica | Sí (BD) | usuario_id integer NOT NULL |
| materia_id | Entero | No aplica | Sí (BD) | materia_id integer NOT NULL |

- `usuario_materias_pkey PRIMARY KEY (usuario_id, materia_id)`
- `fkblixcl3jed4e5u1r4cbuxo233 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`
- `fkgmypxwp3xdvi5ckk7xsd6flna FOREIGN KEY (materia_id) REFERENCES public.materias(id)`
## usuario_permisos_adicionales

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| usuario_id | Entero | No aplica | Sí (BD) | usuario_id integer NOT NULL |
| permiso | Texto | 80 | Sí (BD) | permiso character varying(80) NOT NULL |

- `usuario_permisos_adicionales_pkey PRIMARY KEY (usuario_id, permiso)`
- `fkjml13xi25qa1awrohharg2hc3 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id)`
- `CONSTRAINT usuario_permisos_adicionales_permiso_check CHECK (((permiso)::text = ANY ((ARRAY['DASHBOARD_VER'::character varying, 'AGENDA_VER'::character varying, 'CATALOGOS_VER'::character varying, 'USUARIOS_VER'::character varying, 'ADMIN_PANEL'::character varying, 'EXPEDIENTES_VER'::character varying, 'EXPEDIENTES_SUPERVISION'::character varying, 'TERMINOS_VER'::character varying, 'TERMINOS_SUPERVISION'::character varying, 'AUDIENCIAS_VER'::character varying, 'AUDIENCIAS_SUPERVISION'::character varying, 'DOCUMENTOS_SUPERVISION'::character varying, 'ALERTAS_CONFIGURAR'::character varying])::text[])))`
## usuarios

Fuente: db_schema_actual.sql

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| id | Entero | No aplica | Sí (BD) | id integer NOT NULL |
| activo | Booleano | No aplica | No | activo boolean |
| created_at | Fecha y hora | No aplica | No | created_at timestamp(6) without time zone |
| email | Texto | 100 | Sí (BD) | email character varying(100) NOT NULL |
| force_password_change | Booleano | No aplica | No | force_password_change boolean |
| nombre_completo | Texto | 200 | Sí (BD) | nombre_completo character varying(200) NOT NULL |
| password_hash | Texto | 255 | Sí (BD) | password_hash character varying(255) NOT NULL |
| password_updated_at | Fecha y hora | No aplica | No | password_updated_at timestamp(6) without time zone |
| rol | Texto | 50 | Sí (BD) | rol character varying(50) NOT NULL |
| ultimo_acceso | Fecha y hora | No aplica | No | ultimo_acceso timestamp(6) without time zone |
| updated_at | Fecha y hora | No aplica | No | updated_at timestamp(6) without time zone |
| gerencia_id | Entero | No aplica | No | gerencia_id integer |
| creado_por_id | Entero | No aplica | No | creado_por_id integer |
| username | Texto | 80 | No | username character varying(80) |
| numero_empleado | Texto | 40 | No | numero_empleado character varying(40) |
| microsoft_business_phones | Texto | 300 | No | microsoft_business_phones character varying(300) |
| microsoft_display_name | Texto | 200 | No | microsoft_display_name character varying(200) |
| microsoft_job_title | Texto | 160 | No | microsoft_job_title character varying(160) |
| microsoft_last_sync_at | Fecha y hora | No aplica | No | microsoft_last_sync_at timestamp(6) without time zone |
| microsoft_linked_at | Fecha y hora | No aplica | No | microsoft_linked_at timestamp(6) without time zone |
| microsoft_login_enabled | Booleano | No aplica | No | microsoft_login_enabled boolean |
| microsoft_mobile_phone | Texto | 80 | No | microsoft_mobile_phone character varying(80) |
| microsoft_object_id | Texto | 80 | No | microsoft_object_id character varying(80) |
| microsoft_office_location | Texto | 180 | No | microsoft_office_location character varying(180) |
| microsoft_user_principal_name | Texto | 180 | No | microsoft_user_principal_name character varying(180) |
| puesto | Texto | 160 | No | puesto character varying(160) |
| telefono_institucional | Texto | 120 | No | telefono_institucional character varying(120) |
| telefono_movil | Texto | 80 | No | telefono_movil character varying(80) |
| ubicacion_oficina | Texto | 180 | No | ubicacion_oficina character varying(180) |

- `ukkfsp0s1tflm1cwlj8idhqsad0 UNIQUE (email)`
- `ukr2mnk6pyydgygq94im0nmaps5 UNIQUE (microsoft_object_id)`
- `usuarios_pkey PRIMARY KEY (id)`
- `fkhy8fli5s85u3iut9tvcjggx77 FOREIGN KEY (creado_por_id) REFERENCES public.usuarios(id)`
- `fkrel3m11jpih4a77lseyy3h3ad FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id)`
- `CONSTRAINT usuarios_rol_check CHECK (((rol)::text = ANY (ARRAY['ADMIN'::text, 'DIRECTOR_PRINCIPAL'::text, 'DIRECTOR'::text, 'SUBDIRECTOR'::text, 'GERENTE'::text, 'JEFE_DEPTO'::text, 'ABOGADO'::text])))`
## cat_tipos_termino

Fuente: Entity/catalogo/TipoTermino.java

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| nombre | Texto | 100 | Sí (JPA) |  |
| descripcion | Texto | Sin límite declarado | No |  |
| dias_predeterminados | Entero | No aplica | Sí (JPA) |  |
| tipo_dias | Texto | 20 | Sí (JPA) |  |
| semaforo_rojo_dias | Entero | No aplica | No |  |
| semaforo_amarillo_dias | Entero | No aplica | No |  |
| activo | Booleano | No aplica | No |  |
| materia_id | Entero | No aplica | No |  |
| gerencia_id | Entero | No aplica | No |  |

- `PK id INTEGER generado (JPA)`
- `UNIQUE nombre (JPA)`
## cat_etapas_procesales

Fuente: Entity/catalogo/EtapaCatalogo.java

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| nombre | Texto | 120 | Sí (JPA) |  |
| clave | Texto | 80 | No | Se genera desde nombre si se omite. |
| orden | Entero | No aplica | Sí (JPA) |  |
| descripcion | Texto | Sin límite declarado | No |  |
| activo | Booleano | No aplica | No |  |
| cierra_expediente | Booleano | No aplica | No |  |
| etapa_legacy | Texto | 30 | No |  |
| materia_id | Entero | No aplica | Sí (JPA) |  |
| parent_id | Entero | No aplica | No |  |

- `PK id INTEGER generado (JPA)`
- `UNIQUE (materia_id, nombre, parent_id) (JPA); verificar semántica de NULL para raíces`
## expediente_etapa_periodos

Fuente: Entity/expediente/ExpedienteEtapaPeriodo.java

| Campo | Tipo | Máximo | Nulabilidad / obligatorio | Definición observada |
| --- | --- | --- | --- | --- |
| expediente_id | UUID | No aplica | Sí (JPA) |  |
| etapa_procesal | Texto | 30 | Sí (JPA) |  |
| etapa_catalogo_id | Entero | No aplica | No |  |
| fecha_inicio | Fecha y hora | No aplica | Sí (JPA) |  |
| fecha_fin | Fecha y hora | No aplica | No |  |
| usuario_inicio_id | Entero | No aplica | No |  |
| usuario_fin_id | Entero | No aplica | No |  |
| motivo_cambio | Texto | Sin límite declarado | No |  |

- `PK id BIGINT generado (JPA)`

## Índices únicos adicionales

- `CREATE UNIQUE INDEX ux_usuarios_numero_empleado_lower ON public.usuarios USING btree (lower((numero_empleado)::text)) WHERE (numero_empleado IS NOT NULL);`
- `CREATE UNIQUE INDEX ux_usuarios_username_lower ON public.usuarios USING btree (lower((username)::text)) WHERE (username IS NOT NULL);`
