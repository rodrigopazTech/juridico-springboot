--
-- PostgreSQL database dump
--


-- Dumped from database version 18.1 (Debian 18.1-1.pgdg13+2)
-- Dumped by pg_dump version 18.1 (Debian 18.1-1.pgdg13+2)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- Name: fn_notify_expedientes_realtime(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_notify_expedientes_realtime() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_payload TEXT;
    v_usuario_modificador TEXT;
BEGIN
    -- Captura el usuario que realiza la acci├│n en orden de prioridad:
    -- A) Variable de sesi├│n personalizada de la app
    -- B) El abogado responsable asignado en el registro
    -- C) El usuario de la sesi├│n de base de datos activa
    v_usuario_modificador := COALESCE(
        NULLIF(current_setting('app.current_user', true), ''),
        NEW.abogado_responsable_nombre,
        session_user::text
    );

    -- Empaquetado nativo JSON con tipado robusto
    v_payload := json_build_object(
        'id', NEW.id,
        'etapa_procesal', NEW.etapa_procesal,
        'usuario_modificador', v_usuario_modificador,
        'fecha', COALESCE(NEW.updated_at, NEW.created_at, CURRENT_TIMESTAMP),
        'operacion', TG_OP -- Devuelve 'INSERT' o 'UPDATE'
    )::text;

    -- Emisi├│n del evento al canal
    PERFORM pg_notify('canal_notificaciones_realtime', v_payload);

    -- En triggers AFTER, el valor de retorno es ignorado pero por buenas pr├ícticas se devuelve NEW
    RETURN NEW;
END;
$$;


--
-- Name: fn_notify_realtime(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_notify_realtime() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_payload TEXT;
    v_usuario_modificador TEXT;
    r RECORD;
BEGIN
    IF TG_OP = 'DELETE' THEN
        r := OLD;
    ELSE
        r := NEW;
    END IF;

    v_usuario_modificador := COALESCE(
        NULLIF(current_setting('app.current_user', true), ''),
        session_user::text
    );

    IF TG_TABLE_NAME = 'expedientes' THEN
        v_payload := json_build_object(
            'tabla',               TG_TABLE_NAME,
            'id',                  r.id,
            'etapa_procesal',      r.etapa_procesal,
            'titulo',              r.numero,
            'mensaje',             r.descripcion,
            'usuario_modificador', COALESCE(r.abogado_responsable_nombre, v_usuario_modificador),
            'fecha',               COALESCE(r.updated_at, r.created_at, CURRENT_TIMESTAMP),
            'operacion',           TG_OP
        )::text;

    ELSIF TG_TABLE_NAME = 'notificaciones' THEN
        v_payload := json_build_object(
            'tabla',               TG_TABLE_NAME,
            'id',                  r.id::text,
            'usuario_id',          r.usuario_id,
            'etapa_procesal',      COALESCE(r.tipo, 'ALERTA'),
            'prioridad',           (SELECT nombre FROM cat_prioridad WHERE id = r.prioridad_id),
            'titulo',              r.titulo,
            'mensaje',             r.mensaje,
            'usuario_modificador', v_usuario_modificador,
            'fecha',               COALESCE(r.created_at, CURRENT_TIMESTAMP),
            'operacion',           TG_OP
        )::text;

    ELSIF TG_TABLE_NAME = 'terminos' THEN
        v_payload := json_build_object(
            'tabla',               TG_TABLE_NAME,
            'id',                  r.id,
            'etapa_procesal',      r.estatus_termino,
            'titulo',              r.asunto,
            'mensaje',             r.observaciones,
            'usuario_modificador', v_usuario_modificador,
            'fecha',               COALESCE(r.updated_at, r.created_at, CURRENT_TIMESTAMP),
            'operacion',           TG_OP
        )::text;

    ELSIF TG_TABLE_NAME = 'audiencias' THEN
        v_payload := json_build_object(
            'tabla',               TG_TABLE_NAME,
            'id',                  r.id,
            'estatus',             r.estatus_audiencia,
            'fecha',               COALESCE(r.updated_at, r.created_at, CURRENT_TIMESTAMP),
            'operacion',           TG_OP
        )::text;

    ELSIF TG_TABLE_NAME = 'documentos' THEN
        v_payload := json_build_object(
            'tabla',               TG_TABLE_NAME,
            'id',                  r.id,
            'expediente_id',       r.expediente_id,
            'carpeta_id',          r.carpeta_id,
            'nombre',              r.nombre_original,
            'usuario_modificador', v_usuario_modificador,
            'fecha',               COALESCE(r.updated_at, r.fecha_subida, CURRENT_TIMESTAMP),
            'operacion',           TG_OP
        )::text;

    ELSIF TG_TABLE_NAME = 'carpetas' THEN
        v_payload := json_build_object(
            'tabla',               TG_TABLE_NAME,
            'id',                  r.id,
            'expediente_id',       r.expediente_id,
            'nombre',              r.nombre,
            'usuario_modificador', v_usuario_modificador,
            'fecha',               COALESCE(r.fecha_creacion, CURRENT_TIMESTAMP),
            'operacion',           TG_OP
        )::text;

    ELSIF TG_TABLE_NAME = 'comentarios' THEN
        v_payload := json_build_object(
            'tabla',               TG_TABLE_NAME,
            'id',                  r.id::text,
            'entidad_tipo',        r.entidad_tipo,
            'expediente_id',       CASE WHEN r.entidad_tipo = 'EXPEDIENTE' THEN r.entidad_id ELSE NULL END,
            'comentario',          r.comentario,
            'usuario_nombre',      r.usuario_nombre,
            'fecha',               COALESCE(r.created_at, CURRENT_TIMESTAMP),
            'operacion',           TG_OP
        )::text;

    ELSIF TG_TABLE_NAME = 'actividad_expedientes' THEN
        v_payload := json_build_object(
            'tabla',               TG_TABLE_NAME,
            'id',                  r.id::text,
            'expediente_id',       r.expediente_id,
            'titulo',              r.titulo,
            'descripcion',         r.descripcion,
            'fecha',               COALESCE(r.fecha_registro, CURRENT_TIMESTAMP),
            'operacion',           TG_OP
        )::text;

    ELSIF TG_TABLE_NAME = 'actividad_usuarios' THEN
        v_payload := json_build_object(
            'tabla',               TG_TABLE_NAME,
            'id',                  r.id::text,
            'usuario_id',          r.usuario_id,
            'titulo',              r.titulo,
            'descripcion',         r.descripcion,
            'fecha',               COALESCE(r.fecha_registro, CURRENT_TIMESTAMP),
            'operacion',           TG_OP
        )::text;

    ELSIF TG_TABLE_NAME = 'organos_jurisdiccionales' THEN
        v_payload := json_build_object(
            'tabla',               TG_TABLE_NAME,
            'id',                  r.id,
            'nombre',              r.nombre,
            'gerencia_id',         r.gerencia_id,
            'usuario_modificador', v_usuario_modificador,
            'fecha',               COALESCE(r.updated_at, r.created_at, CURRENT_TIMESTAMP),
            'operacion',           TG_OP
        )::text;

    ELSIF TG_TABLE_NAME = 'tipos_audiencia' THEN
        v_payload := json_build_object(
            'tabla',               TG_TABLE_NAME,
            'id',                  r.id,
            'nombre',              r.nombre,
            'gerencia_id',         r.gerencia_id,
            'usuario_modificador', v_usuario_modificador,
            'fecha',               COALESCE(r.created_at, CURRENT_TIMESTAMP),
            'operacion',           TG_OP
        )::text;

    ELSIF TG_TABLE_NAME = 'cat_tipo_expediente' THEN
        v_payload := json_build_object(
            'tabla',               TG_TABLE_NAME,
            'id',                  r.id,
            'nombre',              r.nombre,
            'usuario_modificador', v_usuario_modificador,
            'fecha',               COALESCE(r.updated_at, r.created_at, CURRENT_TIMESTAMP),
            'operacion',           TG_OP
        )::text;

    ELSIF TG_TABLE_NAME = 'materias' THEN
        v_payload := json_build_object(
            'tabla',               TG_TABLE_NAME,
            'id',                  r.id,
            'nombre',              r.nombre,
            'gerencia_id',         r.gerencia_id,
            'usuario_modificador', v_usuario_modificador,
            'fecha',               COALESCE(r.updated_at, r.created_at, CURRENT_TIMESTAMP),
            'operacion',           TG_OP
        )::text;

    ELSIF TG_TABLE_NAME = 'cat_estados' THEN
        v_payload := json_build_object(
            'tabla',               TG_TABLE_NAME,
            'id',                  r.id,
            'nombre',              r.nombre,
            'gerencia_id',         r.gerencia_id,
            'usuario_modificador', v_usuario_modificador,
            'fecha',               COALESCE(r.created_at, CURRENT_TIMESTAMP),
            'operacion',           TG_OP
        )::text;

    END IF;

    PERFORM pg_notify('canal_notificaciones_realtime', v_payload);

    RETURN r;
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: actividad_expedientes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.actividad_expedientes (
    id bigint NOT NULL,
    descripcion text,
    fecha_registro timestamp(6) without time zone,
    tipo_icono character varying(255),
    titulo character varying(255) NOT NULL,
    expediente_id uuid NOT NULL,
    usuario_id integer,
    etapa_procesal character varying(30),
    etapa_catalogo_id integer,
    etapa_periodo_id bigint,
    CONSTRAINT actividad_expedientes_etapa_procesal_check CHECK (((etapa_procesal)::text = ANY ((ARRAY['TRAMITE'::character varying, 'LAUDO'::character varying, 'FIRME'::character varying, 'CONCLUIDO'::character varying])::text[])))
);


--
-- Name: actividad_expedientes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.actividad_expedientes ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.actividad_expedientes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: actividad_usuarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.actividad_usuarios (
    id bigint NOT NULL,
    descripcion text,
    fecha_registro timestamp(6) without time zone NOT NULL,
    tipo_icono character varying(50),
    titulo character varying(255) NOT NULL,
    usuario_id integer NOT NULL
);


--
-- Name: actividad_usuarios_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.actividad_usuarios ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.actividad_usuarios_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: asuntos_independientes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.asuntos_independientes (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    descripcion text NOT NULL,
    estado character varying(30) NOT NULL,
    fecha_referencia date NOT NULL,
    fecha_vinculacion timestamp(6) without time zone,
    folio_siaj character varying(50) NOT NULL,
    motivo_vinculacion text,
    parte_interesada character varying(255) NOT NULL,
    referencia character varying(150) NOT NULL,
    tipo_referencia character varying(30) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    expediente_destino_id uuid,
    gerencia_id integer NOT NULL,
    materia_id integer NOT NULL,
    responsable_id integer NOT NULL,
    vinculado_por_id integer,
    CONSTRAINT asuntos_independientes_estado_check CHECK (((estado)::text = ANY ((ARRAY['ACTIVO'::character varying, 'VINCULADO'::character varying, 'INACTIVO'::character varying])::text[]))),
    CONSTRAINT asuntos_independientes_tipo_referencia_check CHECK (((tipo_referencia)::text = ANY ((ARRAY['OFICIO'::character varying, 'FOLIO'::character varying, 'SOLICITUD'::character varying, 'NOTIFICACION'::character varying, 'CITATORIO'::character varying, 'DOCUMENTO_ORIGEN'::character varying, 'EXPEDIENTE_EXTERNO'::character varying, 'OTRO'::character varying])::text[])))
);


--
-- Name: asuntos_independientes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.asuntos_independientes ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.asuntos_independientes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: audiencia_contextos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audiencia_contextos (
    id bigint NOT NULL,
    descripcion text NOT NULL,
    numero_expediente_snapshot character varying(50),
    organo_jurisdiccional_texto text,
    origen character varying(30) NOT NULL,
    partes text,
    sede character varying(150),
    gerencia_id integer NOT NULL,
    materia_id integer NOT NULL,
    organo_jurisdiccional_id integer,
    CONSTRAINT audiencia_contextos_origen_check CHECK (((origen)::text = ANY ((ARRAY['EXPEDIENTE'::character varying, 'ASUNTO_UNICO'::character varying])::text[])))
);


--
-- Name: audiencia_contextos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.audiencia_contextos ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.audiencia_contextos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: audiencias; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audiencias (
    id integer NOT NULL,
    acta_documento character varying(500),
    created_at timestamp(6) without time zone,
    es_virtual boolean,
    estatus_audiencia character varying(30),
    fecha_audiencia date NOT NULL,
    fecha_desahogo date,
    hora_audiencia time(6) without time zone NOT NULL,
    observaciones text,
    sala_lugar text,
    updated_at timestamp(6) without time zone,
    url_reunion text,
    abogado_comparece_id integer,
    expediente_id uuid NOT NULL,
    tipo_audiencia_id integer,
    contexto_id bigint,
    usuario_delegado_id integer,
    nota_informativa_documento character varying(500),
    recordatorio_amarillo_horas integer,
    recordatorio_perfil_nombre character varying(120),
    recordatorio_perfil_version integer,
    recordatorio_rojo_horas integer,
    resultado text,
    resultado_registrado_en timestamp(6) without time zone,
    resultado_registrado_por character varying(180),
    resultado_registrado_por_id integer,
    asunto_independiente_id bigint,
    CONSTRAINT ck_audiencias_estatus_audiencia CHECK (((estatus_audiencia)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'CON_ACTA'::character varying, 'SUSPENDIDA'::character varying, 'CONCLUIDA'::character varying])::text[]))),
    CONSTRAINT ck_audiencias_origen_exclusivo CHECK (((expediente_id IS NOT NULL) <> (asunto_independiente_id IS NOT NULL)))
);


--
-- Name: audiencias_desahogadas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audiencias_desahogadas (
    id integer NOT NULL,
    expediente_numero character varying(50),
    fecha_audiencia date NOT NULL,
    fecha_desahogo date NOT NULL,
    hora_audiencia time(6) without time zone NOT NULL,
    sincronizado_at timestamp(6) without time zone,
    audiencia_id integer NOT NULL
);


--
-- Name: audiencias_desahogadas_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.audiencias_desahogadas ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.audiencias_desahogadas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: audiencias_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.audiencias ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.audiencias_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: auditoria_eventos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auditoria_eventos (
    id bigint NOT NULL,
    accion character varying(120) NOT NULL,
    codigo_http integer,
    descripcion text,
    detalle_json text,
    email character varying(150),
    entidad_id character varying(120),
    entidad_tipo character varying(80),
    fecha timestamp(6) without time zone NOT NULL,
    ip character varying(80),
    metodo_http character varying(12),
    modulo character varying(80),
    resultado character varying(30) NOT NULL,
    rol character varying(50),
    ruta character varying(500),
    session_id character varying(255),
    user_agent text,
    usuario_id integer
);


--
-- Name: auditoria_eventos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.auditoria_eventos ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.auditoria_eventos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: auditoria_sesiones; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auditoria_sesiones (
    id bigint NOT NULL,
    email character varying(150),
    estado character varying(30) NOT NULL,
    fecha_cierre timestamp(6) without time zone,
    fecha_inicio timestamp(6) without time zone NOT NULL,
    fecha_ultima_actividad timestamp(6) without time zone,
    ip character varying(80),
    motivo_cierre character varying(80),
    rol character varying(50),
    session_id character varying(255) NOT NULL,
    user_agent text,
    usuario_id integer
);


--
-- Name: auditoria_sesiones_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.auditoria_sesiones ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.auditoria_sesiones_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: bitacora_datos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bitacora_datos (
    id bigint NOT NULL,
    campo character varying(100) NOT NULL,
    fecha timestamp(6) without time zone NOT NULL,
    valor_anterior text,
    valor_nuevo text,
    expediente_id uuid NOT NULL,
    usuario_id integer,
    etapa_procesal character varying(30),
    etapa_catalogo_id integer,
    etapa_periodo_id bigint,
    CONSTRAINT bitacora_datos_etapa_procesal_check CHECK (((etapa_procesal)::text = ANY ((ARRAY['TRAMITE'::character varying, 'LAUDO'::character varying, 'FIRME'::character varying, 'CONCLUIDO'::character varying])::text[])))
);


--
-- Name: bitacora_datos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.bitacora_datos ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.bitacora_datos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: bk_auth_gerencias_20260727_131223_usuario_gerencias; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bk_auth_gerencias_20260727_131223_usuario_gerencias (
    usuario_id integer,
    gerencia_id integer
);


--
-- Name: bk_auth_gerencias_20260727_131223_usuario_materias; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bk_auth_gerencias_20260727_131223_usuario_materias (
    usuario_id integer,
    materia_id integer
);


--
-- Name: bk_auth_gerencias_20260727_131223_usuarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bk_auth_gerencias_20260727_131223_usuarios (
    id integer,
    activo boolean,
    created_at timestamp(6) without time zone,
    email character varying(100),
    force_password_change boolean,
    nombre_completo character varying(200),
    password_hash character varying(255),
    password_updated_at timestamp(6) without time zone,
    rol character varying(50),
    ultimo_acceso timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    gerencia_id integer,
    creado_por_id integer
);


--
-- Name: calendario_versiones; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.calendario_versiones (
    id integer NOT NULL,
    fechas_inhabiles text NOT NULL,
    motivo_cambio character varying(300) NOT NULL,
    publicado_en timestamp(6) without time zone NOT NULL,
    publicado_por character varying(180) NOT NULL,
    version integer NOT NULL
);


--
-- Name: calendario_versiones_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.calendario_versiones ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.calendario_versiones_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: carpetas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.carpetas (
    id uuid NOT NULL,
    es_sistema boolean NOT NULL,
    expediente_id uuid NOT NULL,
    fecha_creacion timestamp(6) without time zone NOT NULL,
    nombre character varying(255) NOT NULL,
    orden integer NOT NULL,
    carpeta_padre_id uuid,
    usuario_creador_id integer
);


--
-- Name: cat_estados; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cat_estados (
    id integer NOT NULL,
    activo boolean,
    created_at timestamp(6) without time zone,
    nombre character varying(255),
    gerencia_id integer,
    usuario_id integer
);


--
-- Name: cat_estados_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.cat_estados ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.cat_estados_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: cat_etapas_procesales; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cat_etapas_procesales (
    id integer NOT NULL,
    activo boolean,
    cierra_expediente boolean,
    clave character varying(80) NOT NULL,
    created_at timestamp(6) without time zone,
    descripcion text,
    etapa_legacy character varying(30),
    nombre character varying(120) NOT NULL,
    orden integer NOT NULL,
    updated_at timestamp(6) without time zone,
    materia_id integer NOT NULL,
    parent_id integer,
    usuario_id integer,
    CONSTRAINT cat_etapas_procesales_etapa_legacy_check CHECK (((etapa_legacy)::text = ANY ((ARRAY['TRAMITE'::character varying, 'LAUDO'::character varying, 'FIRME'::character varying, 'CONCLUIDO'::character varying])::text[])))
);


--
-- Name: cat_etapas_procesales_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.cat_etapas_procesales ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.cat_etapas_procesales_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: cat_prioridad; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cat_prioridad (
    id integer NOT NULL,
    activo boolean,
    color character varying(100),
    created_at timestamp(6) without time zone,
    nombre character varying(50) NOT NULL,
    updated_at timestamp(6) without time zone
);


--
-- Name: cat_prioridad_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.cat_prioridad ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.cat_prioridad_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: cat_tipo_expediente; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cat_tipo_expediente (
    id integer NOT NULL,
    activo boolean,
    created_at timestamp(6) without time zone,
    descripcion text,
    nombre character varying(100) NOT NULL,
    updated_at timestamp(6) without time zone,
    gerencia_id integer,
    usuario_id integer
);


--
-- Name: cat_tipo_expediente_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.cat_tipo_expediente ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.cat_tipo_expediente_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: catalogos_migracion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.catalogos_migracion (
    id bigint NOT NULL,
    estatus character varying(20) NOT NULL,
    id_mapeado bigint,
    nuevo_valor character varying(255),
    tipo_catalogo character varying(50) NOT NULL,
    valor_original character varying(255) NOT NULL,
    lote_id bigint NOT NULL
);


--
-- Name: catalogos_migracion_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.catalogos_migracion ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.catalogos_migracion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: colaboradores_expediente; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.colaboradores_expediente (
    id integer NOT NULL,
    fecha_expiracion timestamp(6) without time zone NOT NULL,
    motivo text,
    permiso_nivel character varying(255),
    expediente_id uuid NOT NULL,
    usuario_id integer NOT NULL
);


--
-- Name: colaboradores_expediente_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.colaboradores_expediente ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.colaboradores_expediente_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: comentarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.comentarios (
    id integer NOT NULL,
    comentario text NOT NULL,
    created_at timestamp(6) without time zone,
    entidad_id character varying(50) NOT NULL,
    entidad_tipo character varying(30) NOT NULL,
    etapa_procesal character varying(30),
    usuario_nombre character varying(255),
    usuario_id integer,
    etapa_catalogo_id integer,
    CONSTRAINT comentarios_etapa_procesal_check CHECK (((etapa_procesal)::text = ANY ((ARRAY['TRAMITE'::character varying, 'LAUDO'::character varying, 'FIRME'::character varying, 'CONCLUIDO'::character varying])::text[])))
);


--
-- Name: comentarios_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.comentarios ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.comentarios_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: configuraciones_alerta; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.configuraciones_alerta (
    id integer NOT NULL,
    activa boolean NOT NULL,
    asunto_id character varying(100),
    asunto_tipo character varying(30),
    created_at timestamp(6) without time zone NOT NULL,
    dias_anticipacion integer NOT NULL,
    nivel character varying(30) NOT NULL,
    obligatoria boolean NOT NULL,
    tipo_evento character varying(30) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    gerencia_id integer,
    usuario_id integer,
    CONSTRAINT configuraciones_alerta_nivel_check CHECK (((nivel)::text = ANY ((ARRAY['SISTEMA'::character varying, 'GERENCIA'::character varying, 'USUARIO'::character varying, 'ASUNTO'::character varying])::text[]))),
    CONSTRAINT configuraciones_alerta_tipo_evento_check CHECK (((tipo_evento)::text = ANY ((ARRAY['TERMINO'::character varying, 'AUDIENCIA'::character varying])::text[])))
);


--
-- Name: configuraciones_alerta_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.configuraciones_alerta ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.configuraciones_alerta_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: configuraciones_semaforo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.configuraciones_semaforo (
    id integer NOT NULL,
    amarillo_hasta integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    nivel character varying(30) NOT NULL,
    rojo_hasta integer NOT NULL,
    tipo_evento character varying(30) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    gerencia_id integer,
    CONSTRAINT configuraciones_semaforo_nivel_check CHECK (((nivel)::text = ANY ((ARRAY['SISTEMA'::character varying, 'GERENCIA'::character varying, 'USUARIO'::character varying, 'ASUNTO'::character varying])::text[]))),
    CONSTRAINT configuraciones_semaforo_tipo_evento_check CHECK (((tipo_evento)::text = ANY ((ARRAY['TERMINO'::character varying, 'AUDIENCIA'::character varying])::text[])))
);


--
-- Name: configuraciones_semaforo_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.configuraciones_semaforo ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.configuraciones_semaforo_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: dias_inhabiles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dias_inhabiles (
    id integer NOT NULL,
    activo boolean NOT NULL,
    descripcion character varying(160) NOT NULL,
    fecha date NOT NULL
);


--
-- Name: dias_inhabiles_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.dias_inhabiles ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.dias_inhabiles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: documentos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.documentos (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    descripcion text,
    expediente_id uuid NOT NULL,
    fecha_subida timestamp(6) without time zone NOT NULL,
    nombre_archivo character varying(255) NOT NULL,
    nombre_original character varying(255) NOT NULL,
    ruta_archivo character varying(500) NOT NULL,
    tamanio_bytes bigint NOT NULL,
    tipo_mime character varying(100),
    updated_at timestamp(6) without time zone,
    carpeta_id uuid,
    usuario_subida_id integer,
    carpeta_original_id uuid,
    carpeta_original_ruta character varying(1024),
    eliminado boolean DEFAULT false NOT NULL,
    eliminado_definitivo boolean DEFAULT false NOT NULL,
    fecha_eliminacion timestamp(6) without time zone,
    fecha_eliminacion_definitiva timestamp(6) without time zone,
    usuario_eliminacion_id integer
);


--
-- Name: documentos_expediente; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.documentos_expediente (
    id integer CONSTRAINT documentos_expediente_id_not_null1 NOT NULL,
    descripcion text,
    mime_type character varying(100),
    nombre_archivo character varying(300) NOT NULL,
    ruta_archivo character varying(500) NOT NULL,
    tamanio_bytes bigint,
    tipo_documento character varying(50),
    uploaded_at timestamp(6) without time zone,
    expediente_id uuid NOT NULL,
    uploaded_by integer
);


--
-- Name: documentos_expediente_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.documentos_expediente ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.documentos_expediente_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: eventos_calendario; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.eventos_calendario (
    id integer NOT NULL,
    categoria character varying(30) NOT NULL,
    color character varying(7),
    created_at timestamp(6) without time zone,
    descripcion text,
    entidad_id integer,
    entidad_tipo character varying(30),
    fecha_fin timestamp(6) without time zone,
    fecha_inicio timestamp(6) without time zone NOT NULL,
    titulo character varying(200) NOT NULL,
    todo_el_dia boolean,
    usuario_id integer
);


--
-- Name: eventos_calendario_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.eventos_calendario ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.eventos_calendario_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: expediente_etapa_periodos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.expediente_etapa_periodos (
    id bigint NOT NULL,
    etapa_procesal character varying(30) NOT NULL,
    fecha_fin timestamp(6) without time zone,
    fecha_inicio timestamp(6) without time zone NOT NULL,
    motivo_cambio text,
    etapa_catalogo_id integer,
    expediente_id uuid NOT NULL,
    usuario_fin_id integer,
    usuario_inicio_id integer,
    CONSTRAINT expediente_etapa_periodos_etapa_procesal_check CHECK (((etapa_procesal)::text = ANY ((ARRAY['TRAMITE'::character varying, 'LAUDO'::character varying, 'FIRME'::character varying, 'CONCLUIDO'::character varying])::text[])))
);


--
-- Name: expediente_etapa_periodos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.expediente_etapa_periodos ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.expediente_etapa_periodos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: expedientes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.expedientes (
    id uuid NOT NULL,
    abogado_responsable_nombre character varying(200),
    created_at timestamp(6) without time zone,
    descripcion text NOT NULL,
    etapa_procesal character varying(30) NOT NULL,
    numero character varying(50) NOT NULL,
    organo_jurisdiccional_texto text,
    partes text,
    sede character varying(150),
    updated_at timestamp(6) without time zone,
    abogado_responsable_id integer,
    gerencia_id integer NOT NULL,
    materia_id integer NOT NULL,
    organo_jurisdiccional_id integer,
    prioridad_id integer,
    tipo_expediente_id integer,
    es_temporal boolean,
    etapa_catalogo_id integer,
    CONSTRAINT expedientes_etapa_procesal_check CHECK (((etapa_procesal)::text = ANY ((ARRAY['TRAMITE'::character varying, 'LAUDO'::character varying, 'FIRME'::character varying, 'CONCLUIDO'::character varying])::text[])))
);


--
-- Name: gerencias; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gerencias (
    id integer NOT NULL,
    activo boolean,
    created_at timestamp(6) without time zone,
    descripcion text,
    nombre character varying(200) NOT NULL,
    updated_at timestamp(6) without time zone
);


--
-- Name: gerencias_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.gerencias ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.gerencias_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: lotes_migracion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.lotes_migracion (
    id bigint NOT NULL,
    estatus character varying(20) NOT NULL,
    fecha_creacion timestamp(6) without time zone NOT NULL,
    paso_actual integer NOT NULL,
    gerencia_global_id integer,
    usuario_responsable_id integer
);


--
-- Name: lotes_migracion_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.lotes_migracion ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.lotes_migracion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: manual_recovery_codes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.manual_recovery_codes (
    id bigint NOT NULL,
    activo boolean NOT NULL,
    codigo_hash character varying(255) NOT NULL,
    fecha_creacion timestamp(6) without time zone NOT NULL,
    fecha_expiracion timestamp(6) without time zone NOT NULL,
    fecha_uso timestamp(6) without time zone,
    motivo character varying(500) NOT NULL,
    origen character varying(50) NOT NULL,
    usado boolean NOT NULL,
    emitido_por_usuario_id integer NOT NULL,
    usuario_id integer NOT NULL
);


--
-- Name: manual_recovery_codes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.manual_recovery_codes ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.manual_recovery_codes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: materias; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.materias (
    id integer NOT NULL,
    activo boolean,
    created_at timestamp(6) without time zone,
    descripcion text,
    nombre character varying(100) NOT NULL,
    updated_at timestamp(6) without time zone,
    gerencia_id integer NOT NULL,
    usuario_id integer
);


--
-- Name: materias_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.materias ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.materias_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: notificaciones; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notificaciones (
    id integer NOT NULL,
    created_at timestamp(6) without time zone,
    entidad_id character varying(50),
    entidad_tipo character varying(30),
    fecha_leida timestamp(6) without time zone,
    leida boolean,
    mensaje text NOT NULL,
    notificar_en timestamp(6) without time zone NOT NULL,
    tipo character varying(30) NOT NULL,
    titulo character varying(200) NOT NULL,
    prioridad_id integer,
    usuario_id integer NOT NULL,
    evento character varying(80) NOT NULL,
    titulo2 character varying(200)
);


--
-- Name: notificaciones_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.notificaciones ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.notificaciones_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: organos_jurisdiccionales; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.organos_jurisdiccionales (
    id integer NOT NULL,
    activo boolean,
    created_at timestamp(6) without time zone,
    nombre character varying(300) NOT NULL,
    sede character varying(100),
    tipo character varying(50),
    updated_at timestamp(6) without time zone,
    gerencia_id integer,
    usuario_id integer
);


--
-- Name: organos_jurisdiccionales_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.organos_jurisdiccionales ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.organos_jurisdiccionales_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: password_reset_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.password_reset_tokens (
    id bigint NOT NULL,
    activo boolean NOT NULL,
    email character varying(150) NOT NULL,
    fecha_creacion timestamp(6) without time zone NOT NULL,
    fecha_expiracion timestamp(6) without time zone NOT NULL,
    fecha_uso timestamp(6) without time zone,
    token character varying(120) NOT NULL,
    usado boolean NOT NULL,
    usuario_id integer NOT NULL
);


--
-- Name: password_reset_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.password_reset_tokens ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.password_reset_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: perfiles_alerta_termino; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.perfiles_alerta_termino (
    id integer NOT NULL,
    amarillo_dias integer,
    amarillo_maximo_dias integer,
    amarillo_minimo_dias integer,
    amarillo_porcentaje integer,
    codigo character varying(50) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    descripcion text NOT NULL,
    estado character varying(20) NOT NULL,
    modo character varying(30) NOT NULL,
    nombre character varying(120) NOT NULL,
    rojo_dias integer,
    rojo_maximo_dias integer,
    rojo_minimo_dias integer,
    rojo_porcentaje integer,
    updated_at timestamp(6) without time zone NOT NULL,
    version integer NOT NULL,
    vigente_desde date,
    vigente_hasta date,
    CONSTRAINT perfiles_alerta_termino_estado_check CHECK (((estado)::text = ANY ((ARRAY['BORRADOR'::character varying, 'PUBLICADO'::character varying, 'INACTIVO'::character varying])::text[]))),
    CONSTRAINT perfiles_alerta_termino_modo_check CHECK (((modo)::text = ANY ((ARRAY['DIAS_RESTANTES'::character varying, 'PROPORCIONAL_LIMITADO'::character varying])::text[])))
);


--
-- Name: perfiles_alerta_termino_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.perfiles_alerta_termino ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.perfiles_alerta_termino_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: perfiles_recordatorio_audiencia; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.perfiles_recordatorio_audiencia (
    id integer NOT NULL,
    anticipacion_amarilla_horas integer CONSTRAINT perfiles_recordatorio_audie_anticipacion_amarilla_hora_not_null NOT NULL,
    anticipacion_roja_horas integer CONSTRAINT perfiles_recordatorio_audienci_anticipacion_roja_horas_not_null NOT NULL,
    codigo character varying(50) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    descripcion text NOT NULL,
    estado character varying(20) NOT NULL,
    nombre character varying(120) NOT NULL,
    recordatorios_minutos character varying(200) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version integer NOT NULL,
    vigente_desde date,
    vigente_hasta date,
    CONSTRAINT perfiles_recordatorio_audiencia_estado_check CHECK (((estado)::text = ANY ((ARRAY['BORRADOR'::character varying, 'PUBLICADO'::character varying, 'INACTIVO'::character varying])::text[])))
);


--
-- Name: perfiles_recordatorio_audiencia_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.perfiles_recordatorio_audiencia ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.perfiles_recordatorio_audiencia_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: preferencias_notificacion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.preferencias_notificacion (
    id integer NOT NULL,
    notificar_nueva_audiencia boolean,
    notificar_nuevo_termino boolean,
    notificar_reasignacion boolean,
    notificar_termino_vencer boolean,
    usuario_id integer NOT NULL
);


--
-- Name: preferencias_notificacion_evento; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.preferencias_notificacion_evento (
    id integer NOT NULL,
    evento character varying(80) NOT NULL,
    habilitada boolean NOT NULL,
    usuario_id integer NOT NULL,
    CONSTRAINT preferencias_notificacion_evento_evento_check CHECK (((evento)::text = ANY (ARRAY['EXPEDIENTE_NUEVO'::text, 'EXPEDIENTE_REASIGNADO'::text, 'EXPEDIENTE_OBSERVACION_NUEVA'::text, 'TERMINO_ASIGNADO'::text, 'TERMINO_NUEVO'::text, 'TERMINO_CAMBIO_ETAPA'::text, 'TERMINO_VALIDADO_CONCLUIDO'::text, 'TERMINO_RECORDATORIO'::text, 'AUDIENCIA_NUEVA'::text, 'AUDIENCIA_COMPARECENCIA_ASIGNADA'::text, 'AUDIENCIA_DELEGADA'::text, 'AUDIENCIA_RECORDATORIO'::text, 'AUDIENCIA_ACTA_DISPONIBLE'::text, 'AUDIENCIA_VALIDADA_CONCLUIDA'::text, 'SEGURIDAD_PASSWORD_POR_VENCER'::text, 'TAREA_RECORDATORIO'::text])))
);


--
-- Name: preferencias_notificacion_evento_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.preferencias_notificacion_evento ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.preferencias_notificacion_evento_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: preferencias_notificacion_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.preferencias_notificacion ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.preferencias_notificacion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: push_subscriptions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.push_subscriptions (
    id integer NOT NULL,
    usuario_id integer NOT NULL,
    endpoint text NOT NULL,
    p256dh character varying(255) NOT NULL,
    auth character varying(255) NOT NULL,
    device_id character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    session_id character varying(255)
);


--
-- Name: push_subscriptions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.push_subscriptions_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: push_subscriptions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.push_subscriptions_id_seq OWNED BY public.push_subscriptions.id;


--
-- Name: recordatorios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recordatorios (
    id integer NOT NULL,
    completado boolean,
    created_at timestamp(6) without time zone,
    detalles text,
    fecha_recordatorio date NOT NULL,
    hora_recordatorio time(6) without time zone NOT NULL,
    titulo character varying(200) NOT NULL,
    updated_at timestamp(6) without time zone,
    prioridad_id integer,
    usuario_id integer NOT NULL,
    recordatorio_revision integer NOT NULL
);


--
-- Name: recordatorios_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.recordatorios ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.recordatorios_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: registros_migracion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.registros_migracion (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    estatus character varying(20) NOT NULL,
    mensaje_error text,
    numero_expediente_extraido character varying(100),
    payload_json text NOT NULL,
    processed_at timestamp(6) without time zone,
    tipo_dato character varying(50) NOT NULL,
    lote_id bigint
);


--
-- Name: registros_migracion_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.registros_migracion ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.registros_migracion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: suspensiones_plazo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.suspensiones_plazo (
    id integer NOT NULL,
    activo boolean NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    fecha_fin date NOT NULL,
    fecha_inicio date NOT NULL,
    motivo character varying(300) NOT NULL,
    registrado_por character varying(180) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL
);


--
-- Name: suspensiones_plazo_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.suspensiones_plazo ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.suspensiones_plazo_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: termino_archivos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.termino_archivos (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    etapa character varying(30) NOT NULL,
    nombre_original character varying(255) NOT NULL,
    ruta_archivo character varying(700) NOT NULL,
    tamanio_bytes bigint,
    tipo_documento character varying(20) NOT NULL,
    tipo_mime character varying(120),
    termino_id integer NOT NULL,
    usuario_subida_id integer,
    CONSTRAINT ck_termino_archivos_tipo_documento CHECK (((tipo_documento)::text = ANY ((ARRAY['BORRADOR'::character varying, 'ACUSE'::character varying, 'NOTA_INFORMATIVA'::character varying])::text[]))),
    CONSTRAINT termino_archivos_etapa_check CHECK (((etapa)::text = ANY ((ARRAY['PROYECTISTA'::character varying, 'REVISION'::character varying, 'GERENCIA'::character varying, 'DIRECCION'::character varying, 'LIBERADO'::character varying, 'PRESENTADO'::character varying, 'CONCLUIDO'::character varying])::text[])))
);


--
-- Name: termino_archivos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.termino_archivos ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.termino_archivos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: termino_contextos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.termino_contextos (
    id bigint NOT NULL,
    descripcion text NOT NULL,
    numero_expediente_snapshot character varying(50),
    organo_jurisdiccional_texto text,
    origen character varying(30) NOT NULL,
    partes text,
    sede character varying(150),
    gerencia_id integer NOT NULL,
    materia_id integer NOT NULL,
    organo_jurisdiccional_id integer,
    CONSTRAINT termino_contextos_origen_check CHECK (((origen)::text = ANY ((ARRAY['EXPEDIENTE'::character varying, 'ASUNTO_UNICO'::character varying])::text[])))
);


--
-- Name: termino_contextos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.termino_contextos ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.termino_contextos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: terminos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.terminos (
    id integer NOT NULL,
    acuse character varying(500),
    acuse_nombre character varying(500),
    asunto text,
    created_at timestamp(6) without time zone,
    estatus_termino character varying(30) NOT NULL,
    fecha_ingreso date,
    fecha_presentacion date,
    fecha_vencimiento date NOT NULL,
    observaciones text,
    updated_at timestamp(6) without time zone,
    abogado_responsable_id integer,
    expediente_id uuid,
    prioridad_id integer,
    fecha_creacion date,
    contexto_id bigint,
    archivo_word character varying(500),
    prioridad character varying(20),
    alerta_amarillo_dias integer,
    alerta_amarillo_maximo_snapshot integer,
    alerta_amarillo_minimo_snapshot integer,
    alerta_amarillo_porcentaje_snapshot integer,
    alerta_modo character varying(30),
    alerta_perfil_nombre character varying(120),
    alerta_perfil_version integer,
    alerta_rojo_dias integer,
    alerta_rojo_maximo_snapshot integer,
    alerta_rojo_minimo_snapshot integer,
    alerta_rojo_porcentaje_snapshot integer,
    desglose_dias_texto text,
    es_tiempo_dinamico boolean,
    plazo_calendario_fechas text,
    plazo_calendario_version integer,
    plazo_cantidad_snapshot integer,
    plazo_duracion_adoptada bigint,
    plazo_duracion_detectada bigint,
    plazo_excepcion_autor character varying(180),
    plazo_excepcion_autor_id integer,
    plazo_excepcion_fecha timestamp(6) without time zone,
    plazo_excepcion_motivo text,
    plazo_explicacion text,
    plazo_fecha_adoptada date,
    plazo_fecha_calculada date,
    plazo_modo_snapshot character varying(30),
    plazo_regla_computo character varying(120),
    plazo_tipo_dias_snapshot character varying(20),
    tipo_dias_calculado character varying(20),
    tipo_evidencia_presentacion character varying(30),
    asunto_independiente_id bigint,
    tipo_termino_id integer,
    CONSTRAINT ck_terminos_origen_exclusivo CHECK (((expediente_id IS NOT NULL) <> (asunto_independiente_id IS NOT NULL))),
    CONSTRAINT terminos_alerta_modo_check CHECK (((alerta_modo)::text = ANY ((ARRAY['DIAS_RESTANTES'::character varying, 'PROPORCIONAL_LIMITADO'::character varying])::text[]))),
    CONSTRAINT terminos_estatus_termino_check CHECK (((estatus_termino)::text = ANY ((ARRAY['PROYECTISTA'::character varying, 'REVISION'::character varying, 'GERENCIA'::character varying, 'DIRECCION'::character varying, 'LIBERADO'::character varying, 'PRESENTADO'::character varying, 'CONCLUIDO'::character varying])::text[]))),
    CONSTRAINT terminos_plazo_modo_snapshot_check CHECK (((plazo_modo_snapshot)::text = ANY ((ARRAY['CALCULADO'::character varying, 'CAPTURADO'::character varying, 'SIN_VENCIMIENTO'::character varying])::text[]))),
    CONSTRAINT terminos_prioridad_check CHECK (((prioridad)::text = ANY (ARRAY[('ALTA'::character varying)::text, ('MEDIA'::character varying)::text, ('BAJA'::character varying)::text]))),
    CONSTRAINT terminos_tipo_evidencia_presentacion_check CHECK (((tipo_evidencia_presentacion)::text = ANY ((ARRAY['ACUSE'::character varying, 'NOTA_INFORMATIVA'::character varying])::text[])))
);


--
-- Name: terminos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.terminos ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.terminos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: terminos_presentados; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.terminos_presentados (
    id integer NOT NULL,
    acuse_documento character varying(500),
    expediente_numero character varying(50),
    fecha_presentacion date NOT NULL,
    sincronizado_at timestamp(6) without time zone,
    termino_id integer NOT NULL
);


--
-- Name: terminos_presentados_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.terminos_presentados ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.terminos_presentados_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tipos_audiencia; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tipos_audiencia (
    id integer NOT NULL,
    activo boolean,
    created_at timestamp(6) without time zone,
    descripcion text,
    nombre character varying(100) NOT NULL,
    gerencia_id integer,
    usuario_id integer,
    duracion_predeterminada_minutos integer,
    modalidad_predeterminada character varying(20),
    materia_id integer,
    perfil_recordatorio_id integer,
    codigo character varying(50),
    modalidades_permitidas character varying(80) DEFAULT 'PRESENCIAL,VIRTUAL,HIBRIDA'::character varying NOT NULL,
    requiere_acta boolean DEFAULT true NOT NULL,
    permite_excepcion_sin_acta boolean DEFAULT false NOT NULL,
    requiere_validacion_acta boolean DEFAULT true NOT NULL
);


--
-- Name: tipos_audiencia_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tipos_audiencia ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tipos_audiencia_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tipos_termino; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tipos_termino (
    id integer NOT NULL,
    activo boolean,
    ajusta_siguiente_habil boolean DEFAULT false NOT NULL,
    codigo character varying(50) NOT NULL,
    created_at timestamp(6) without time zone,
    descripcion text,
    dias_predeterminados integer,
    modo_plazo character varying(30) NOT NULL,
    nombre character varying(100) NOT NULL,
    permite_excepcion_sin_acuse boolean NOT NULL,
    requiere_acuse boolean NOT NULL,
    requiere_documento_liberado boolean NOT NULL,
    requiere_validacion_acuse boolean NOT NULL,
    tipo_dias character varying(20) NOT NULL,
    updated_at timestamp(6) without time zone,
    gerencia_id integer,
    materia_id integer,
    perfil_alerta_id integer,
    CONSTRAINT tipos_termino_modo_plazo_check CHECK (((modo_plazo)::text = ANY ((ARRAY['CALCULADO'::character varying, 'CAPTURADO'::character varying, 'SIN_VENCIMIENTO'::character varying])::text[])))
);


--
-- Name: tipos_termino_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tipos_termino ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tipos_termino_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: usuario_gerencias; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuario_gerencias (
    usuario_id integer NOT NULL,
    gerencia_id integer NOT NULL
);


--
-- Name: usuario_materias; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuario_materias (
    usuario_id integer NOT NULL,
    materia_id integer NOT NULL
);


--
-- Name: usuario_permisos_adicionales; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuario_permisos_adicionales (
    usuario_id integer NOT NULL,
    permiso character varying(80) NOT NULL,
    CONSTRAINT usuario_permisos_adicionales_permiso_check CHECK (((permiso)::text = ANY ((ARRAY['DASHBOARD_VER'::character varying, 'AGENDA_VER'::character varying, 'CATALOGOS_VER'::character varying, 'USUARIOS_VER'::character varying, 'ADMIN_PANEL'::character varying, 'EXPEDIENTES_VER'::character varying, 'EXPEDIENTES_SUPERVISION'::character varying, 'TERMINOS_VER'::character varying, 'TERMINOS_SUPERVISION'::character varying, 'AUDIENCIAS_VER'::character varying, 'AUDIENCIAS_SUPERVISION'::character varying, 'DOCUMENTOS_SUPERVISION'::character varying, 'ALERTAS_CONFIGURAR'::character varying])::text[])))
);


--
-- Name: usuarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuarios (
    id integer NOT NULL,
    activo boolean,
    created_at timestamp(6) without time zone,
    email character varying(100) NOT NULL,
    force_password_change boolean,
    nombre_completo character varying(200) NOT NULL,
    password_hash character varying(255) NOT NULL,
    password_updated_at timestamp(6) without time zone,
    rol character varying(50) NOT NULL,
    ultimo_acceso timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    gerencia_id integer,
    creado_por_id integer,
    username character varying(80),
    numero_empleado character varying(40),
    microsoft_business_phones character varying(300),
    microsoft_display_name character varying(200),
    microsoft_job_title character varying(160),
    microsoft_last_sync_at timestamp(6) without time zone,
    microsoft_linked_at timestamp(6) without time zone,
    microsoft_login_enabled boolean,
    microsoft_mobile_phone character varying(80),
    microsoft_object_id character varying(80),
    microsoft_office_location character varying(180),
    microsoft_user_principal_name character varying(180),
    puesto character varying(160),
    telefono_institucional character varying(120),
    telefono_movil character varying(80),
    ubicacion_oficina character varying(180),
    security_version integer,
    CONSTRAINT usuarios_rol_check CHECK (((rol)::text = ANY (ARRAY['ADMIN'::text, 'DIRECTOR_PRINCIPAL'::text, 'DIRECTOR'::text, 'SUBDIRECTOR'::text, 'GERENTE'::text, 'JEFE_DEPTO'::text, 'ABOGADO'::text])))
);


--
-- Name: usuarios_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.usuarios ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.usuarios_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: push_subscriptions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.push_subscriptions ALTER COLUMN id SET DEFAULT nextval('public.push_subscriptions_id_seq'::regclass);


--
-- Name: actividad_expedientes actividad_expedientes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.actividad_expedientes
    ADD CONSTRAINT actividad_expedientes_pkey PRIMARY KEY (id);


--
-- Name: actividad_usuarios actividad_usuarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.actividad_usuarios
    ADD CONSTRAINT actividad_usuarios_pkey PRIMARY KEY (id);


--
-- Name: asuntos_independientes asuntos_independientes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asuntos_independientes
    ADD CONSTRAINT asuntos_independientes_pkey PRIMARY KEY (id);


--
-- Name: audiencia_contextos audiencia_contextos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audiencia_contextos
    ADD CONSTRAINT audiencia_contextos_pkey PRIMARY KEY (id);


--
-- Name: audiencias_desahogadas audiencias_desahogadas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audiencias_desahogadas
    ADD CONSTRAINT audiencias_desahogadas_pkey PRIMARY KEY (id);


--
-- Name: audiencias audiencias_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audiencias
    ADD CONSTRAINT audiencias_pkey PRIMARY KEY (id);


--
-- Name: auditoria_eventos auditoria_eventos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditoria_eventos
    ADD CONSTRAINT auditoria_eventos_pkey PRIMARY KEY (id);


--
-- Name: auditoria_sesiones auditoria_sesiones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditoria_sesiones
    ADD CONSTRAINT auditoria_sesiones_pkey PRIMARY KEY (id);


--
-- Name: bitacora_datos bitacora_datos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bitacora_datos
    ADD CONSTRAINT bitacora_datos_pkey PRIMARY KEY (id);


--
-- Name: calendario_versiones calendario_versiones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.calendario_versiones
    ADD CONSTRAINT calendario_versiones_pkey PRIMARY KEY (id);


--
-- Name: carpetas carpetas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carpetas
    ADD CONSTRAINT carpetas_pkey PRIMARY KEY (id);


--
-- Name: cat_estados cat_estados_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cat_estados
    ADD CONSTRAINT cat_estados_pkey PRIMARY KEY (id);


--
-- Name: cat_etapas_procesales cat_etapas_procesales_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cat_etapas_procesales
    ADD CONSTRAINT cat_etapas_procesales_pkey PRIMARY KEY (id);


--
-- Name: cat_prioridad cat_prioridad_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cat_prioridad
    ADD CONSTRAINT cat_prioridad_pkey PRIMARY KEY (id);


--
-- Name: cat_tipo_expediente cat_tipo_expediente_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cat_tipo_expediente
    ADD CONSTRAINT cat_tipo_expediente_pkey PRIMARY KEY (id);


--
-- Name: catalogos_migracion catalogos_migracion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.catalogos_migracion
    ADD CONSTRAINT catalogos_migracion_pkey PRIMARY KEY (id);


--
-- Name: colaboradores_expediente colaboradores_expediente_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.colaboradores_expediente
    ADD CONSTRAINT colaboradores_expediente_pkey PRIMARY KEY (id);


--
-- Name: comentarios comentarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comentarios
    ADD CONSTRAINT comentarios_pkey PRIMARY KEY (id);


--
-- Name: configuraciones_alerta configuraciones_alerta_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.configuraciones_alerta
    ADD CONSTRAINT configuraciones_alerta_pkey PRIMARY KEY (id);


--
-- Name: configuraciones_semaforo configuraciones_semaforo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.configuraciones_semaforo
    ADD CONSTRAINT configuraciones_semaforo_pkey PRIMARY KEY (id);


--
-- Name: dias_inhabiles dias_inhabiles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dias_inhabiles
    ADD CONSTRAINT dias_inhabiles_pkey PRIMARY KEY (id);


--
-- Name: documentos_expediente documentos_expediente_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documentos_expediente
    ADD CONSTRAINT documentos_expediente_pkey PRIMARY KEY (id);


--
-- Name: documentos documentos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documentos
    ADD CONSTRAINT documentos_pkey PRIMARY KEY (id);


--
-- Name: eventos_calendario eventos_calendario_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.eventos_calendario
    ADD CONSTRAINT eventos_calendario_pkey PRIMARY KEY (id);


--
-- Name: expediente_etapa_periodos expediente_etapa_periodos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expediente_etapa_periodos
    ADD CONSTRAINT expediente_etapa_periodos_pkey PRIMARY KEY (id);


--
-- Name: expedientes expedientes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expedientes
    ADD CONSTRAINT expedientes_pkey PRIMARY KEY (id);


--
-- Name: gerencias gerencias_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gerencias
    ADD CONSTRAINT gerencias_pkey PRIMARY KEY (id);


--
-- Name: asuntos_independientes idx_asunto_ind_folio; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asuntos_independientes
    ADD CONSTRAINT idx_asunto_ind_folio UNIQUE (folio_siaj);


--
-- Name: password_reset_tokens idx_password_reset_token; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT idx_password_reset_token UNIQUE (token);


--
-- Name: lotes_migracion lotes_migracion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lotes_migracion
    ADD CONSTRAINT lotes_migracion_pkey PRIMARY KEY (id);


--
-- Name: manual_recovery_codes manual_recovery_codes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.manual_recovery_codes
    ADD CONSTRAINT manual_recovery_codes_pkey PRIMARY KEY (id);


--
-- Name: materias materias_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.materias
    ADD CONSTRAINT materias_pkey PRIMARY KEY (id);


--
-- Name: notificaciones notificaciones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notificaciones
    ADD CONSTRAINT notificaciones_pkey PRIMARY KEY (id);


--
-- Name: organos_jurisdiccionales organos_jurisdiccionales_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organos_jurisdiccionales
    ADD CONSTRAINT organos_jurisdiccionales_pkey PRIMARY KEY (id);


--
-- Name: password_reset_tokens password_reset_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id);


--
-- Name: perfiles_alerta_termino perfiles_alerta_termino_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.perfiles_alerta_termino
    ADD CONSTRAINT perfiles_alerta_termino_pkey PRIMARY KEY (id);


--
-- Name: perfiles_recordatorio_audiencia perfiles_recordatorio_audiencia_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.perfiles_recordatorio_audiencia
    ADD CONSTRAINT perfiles_recordatorio_audiencia_pkey PRIMARY KEY (id);


--
-- Name: preferencias_notificacion_evento preferencias_notificacion_evento_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.preferencias_notificacion_evento
    ADD CONSTRAINT preferencias_notificacion_evento_pkey PRIMARY KEY (id);


--
-- Name: preferencias_notificacion preferencias_notificacion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.preferencias_notificacion
    ADD CONSTRAINT preferencias_notificacion_pkey PRIMARY KEY (id);


--
-- Name: push_subscriptions push_subscriptions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.push_subscriptions
    ADD CONSTRAINT push_subscriptions_pkey PRIMARY KEY (id);


--
-- Name: recordatorios recordatorios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recordatorios
    ADD CONSTRAINT recordatorios_pkey PRIMARY KEY (id);


--
-- Name: registros_migracion registros_migracion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.registros_migracion
    ADD CONSTRAINT registros_migracion_pkey PRIMARY KEY (id);


--
-- Name: suspensiones_plazo suspensiones_plazo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.suspensiones_plazo
    ADD CONSTRAINT suspensiones_plazo_pkey PRIMARY KEY (id);


--
-- Name: termino_archivos termino_archivos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.termino_archivos
    ADD CONSTRAINT termino_archivos_pkey PRIMARY KEY (id);


--
-- Name: termino_contextos termino_contextos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.termino_contextos
    ADD CONSTRAINT termino_contextos_pkey PRIMARY KEY (id);


--
-- Name: terminos terminos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terminos
    ADD CONSTRAINT terminos_pkey PRIMARY KEY (id);


--
-- Name: terminos_presentados terminos_presentados_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terminos_presentados
    ADD CONSTRAINT terminos_presentados_pkey PRIMARY KEY (id);


--
-- Name: tipos_audiencia tipos_audiencia_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_audiencia
    ADD CONSTRAINT tipos_audiencia_pkey PRIMARY KEY (id);


--
-- Name: tipos_termino tipos_termino_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_termino
    ADD CONSTRAINT tipos_termino_pkey PRIMARY KEY (id);


--
-- Name: materias uk35p3esp62g4obf0xvgxkrgh2e; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.materias
    ADD CONSTRAINT uk35p3esp62g4obf0xvgxkrgh2e UNIQUE (nombre, gerencia_id);


--
-- Name: tipos_termino uk3l3g96633585u7bhpyl1cpfvh; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_termino
    ADD CONSTRAINT uk3l3g96633585u7bhpyl1cpfvh UNIQUE (nombre);


--
-- Name: cat_etapas_procesales uk3qgbpwr4fhsrit27tt1rpwpmr; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cat_etapas_procesales
    ADD CONSTRAINT uk3qgbpwr4fhsrit27tt1rpwpmr UNIQUE (materia_id, nombre, parent_id);


--
-- Name: gerencias uk5nv105r1e4j3r6hrfxxa4fxeg; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gerencias
    ADD CONSTRAINT uk5nv105r1e4j3r6hrfxxa4fxeg UNIQUE (nombre);


--
-- Name: audiencias uk6yco257iom67jk0pilkinvda3; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audiencias
    ADD CONSTRAINT uk6yco257iom67jk0pilkinvda3 UNIQUE (contexto_id);


--
-- Name: cat_tipo_expediente uk9cslxj82xdh4mfg9baolqj7xu; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cat_tipo_expediente
    ADD CONSTRAINT uk9cslxj82xdh4mfg9baolqj7xu UNIQUE (nombre);


--
-- Name: calendario_versiones uk_calendario_version; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.calendario_versiones
    ADD CONSTRAINT uk_calendario_version UNIQUE (version);


--
-- Name: perfiles_alerta_termino uk_perfil_alerta_codigo_version; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.perfiles_alerta_termino
    ADD CONSTRAINT uk_perfil_alerta_codigo_version UNIQUE (codigo, version);


--
-- Name: perfiles_recordatorio_audiencia uk_perfil_audiencia_codigo_version; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.perfiles_recordatorio_audiencia
    ADD CONSTRAINT uk_perfil_audiencia_codigo_version UNIQUE (codigo, version);


--
-- Name: preferencias_notificacion_evento uk_pref_notif_evento_usuario; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.preferencias_notificacion_evento
    ADD CONSTRAINT uk_pref_notif_evento_usuario UNIQUE (usuario_id, evento);


--
-- Name: dias_inhabiles uka446v2shx7pudk08vvw6qtvi3; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dias_inhabiles
    ADD CONSTRAINT uka446v2shx7pudk08vvw6qtvi3 UNIQUE (fecha);


--
-- Name: preferencias_notificacion uke1fpwe1mmm24nh837d91ymwnh; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.preferencias_notificacion
    ADD CONSTRAINT uke1fpwe1mmm24nh837d91ymwnh UNIQUE (usuario_id);


--
-- Name: tipos_termino ukeqv9g7pr34c4qpe14s4j8uy4j; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_termino
    ADD CONSTRAINT ukeqv9g7pr34c4qpe14s4j8uy4j UNIQUE (codigo);


--
-- Name: tipos_audiencia ukhwdw390hju3jpv9r2pvbn7eat; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_audiencia
    ADD CONSTRAINT ukhwdw390hju3jpv9r2pvbn7eat UNIQUE (nombre);


--
-- Name: terminos ukjcdu631eit28n3wdtg3y1elgb; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terminos
    ADD CONSTRAINT ukjcdu631eit28n3wdtg3y1elgb UNIQUE (contexto_id);


--
-- Name: usuarios ukkfsp0s1tflm1cwlj8idhqsad0; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT ukkfsp0s1tflm1cwlj8idhqsad0 UNIQUE (email);


--
-- Name: organos_jurisdiccionales ukmc16d9k9cfsrevrlbjdt9cctb; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organos_jurisdiccionales
    ADD CONSTRAINT ukmc16d9k9cfsrevrlbjdt9cctb UNIQUE (nombre);


--
-- Name: cat_estados uknst6hp4h4jox86rou3fpxfmd0; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cat_estados
    ADD CONSTRAINT uknst6hp4h4jox86rou3fpxfmd0 UNIQUE (nombre);


--
-- Name: cat_prioridad ukp238eocvbwiipffxptyq0uq07; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cat_prioridad
    ADD CONSTRAINT ukp238eocvbwiipffxptyq0uq07 UNIQUE (nombre);


--
-- Name: usuarios ukr2mnk6pyydgygq94im0nmaps5; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT ukr2mnk6pyydgygq94im0nmaps5 UNIQUE (microsoft_object_id);


--
-- Name: usuario_gerencias usuario_gerencias_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario_gerencias
    ADD CONSTRAINT usuario_gerencias_pkey PRIMARY KEY (usuario_id, gerencia_id);


--
-- Name: usuario_materias usuario_materias_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario_materias
    ADD CONSTRAINT usuario_materias_pkey PRIMARY KEY (usuario_id, materia_id);


--
-- Name: usuario_permisos_adicionales usuario_permisos_adicionales_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario_permisos_adicionales
    ADD CONSTRAINT usuario_permisos_adicionales_pkey PRIMARY KEY (usuario_id, permiso);


--
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id);


--
-- Name: idx_asunto_ind_estado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_asunto_ind_estado ON public.asuntos_independientes USING btree (estado);


--
-- Name: idx_asunto_ind_gerencia; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_asunto_ind_gerencia ON public.asuntos_independientes USING btree (gerencia_id);


--
-- Name: idx_asunto_ind_materia; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_asunto_ind_materia ON public.asuntos_independientes USING btree (materia_id);


--
-- Name: idx_audiencia_contexto_gerencia; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audiencia_contexto_gerencia ON public.audiencia_contextos USING btree (gerencia_id);


--
-- Name: idx_audiencia_contexto_materia; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audiencia_contexto_materia ON public.audiencia_contextos USING btree (materia_id);


--
-- Name: idx_audiencia_contexto_origen; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audiencia_contexto_origen ON public.audiencia_contextos USING btree (origen);


--
-- Name: idx_audiencias_asunto_independiente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audiencias_asunto_independiente ON public.audiencias USING btree (asunto_independiente_id);


--
-- Name: idx_audiencias_expediente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audiencias_expediente ON public.audiencias USING btree (expediente_id);


--
-- Name: idx_audiencias_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audiencias_fecha ON public.audiencias USING btree (fecha_audiencia);


--
-- Name: idx_auditoria_eventos_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auditoria_eventos_fecha ON public.auditoria_eventos USING btree (fecha);


--
-- Name: idx_auditoria_eventos_modulo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auditoria_eventos_modulo ON public.auditoria_eventos USING btree (modulo);


--
-- Name: idx_auditoria_eventos_resultado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auditoria_eventos_resultado ON public.auditoria_eventos USING btree (resultado);


--
-- Name: idx_auditoria_eventos_session; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auditoria_eventos_session ON public.auditoria_eventos USING btree (session_id);


--
-- Name: idx_auditoria_eventos_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auditoria_eventos_usuario ON public.auditoria_eventos USING btree (usuario_id);


--
-- Name: idx_auditoria_sesiones_estado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auditoria_sesiones_estado ON public.auditoria_sesiones USING btree (estado);


--
-- Name: idx_auditoria_sesiones_inicio; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auditoria_sesiones_inicio ON public.auditoria_sesiones USING btree (fecha_inicio);


--
-- Name: idx_auditoria_sesiones_session; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auditoria_sesiones_session ON public.auditoria_sesiones USING btree (session_id);


--
-- Name: idx_auditoria_sesiones_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auditoria_sesiones_usuario ON public.auditoria_sesiones USING btree (usuario_id);


--
-- Name: idx_comentarios_entidad; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_comentarios_entidad ON public.comentarios USING btree (entidad_tipo, entidad_id);


--
-- Name: idx_config_alerta_asunto; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_config_alerta_asunto ON public.configuraciones_alerta USING btree (asunto_tipo, asunto_id);


--
-- Name: idx_config_alerta_gerencia; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_config_alerta_gerencia ON public.configuraciones_alerta USING btree (gerencia_id);


--
-- Name: idx_config_alerta_tipo_activa; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_config_alerta_tipo_activa ON public.configuraciones_alerta USING btree (tipo_evento, activa);


--
-- Name: idx_config_alerta_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_config_alerta_usuario ON public.configuraciones_alerta USING btree (usuario_id);


--
-- Name: idx_documentos_expediente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_documentos_expediente ON public.documentos_expediente USING btree (expediente_id);


--
-- Name: idx_etapa_periodo_activo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_etapa_periodo_activo ON public.expediente_etapa_periodos USING btree (expediente_id, fecha_fin);


--
-- Name: idx_etapa_periodo_etapa; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_etapa_periodo_etapa ON public.expediente_etapa_periodos USING btree (etapa_procesal);


--
-- Name: idx_etapa_periodo_expediente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_etapa_periodo_expediente ON public.expediente_etapa_periodos USING btree (expediente_id);


--
-- Name: idx_etapas_materia; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_etapas_materia ON public.cat_etapas_procesales USING btree (materia_id);


--
-- Name: idx_etapas_orden; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_etapas_orden ON public.cat_etapas_procesales USING btree (materia_id, orden);


--
-- Name: idx_etapas_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_etapas_parent ON public.cat_etapas_procesales USING btree (parent_id);


--
-- Name: idx_eventos_fecha_inicio; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_eventos_fecha_inicio ON public.eventos_calendario USING btree (fecha_inicio);


--
-- Name: idx_eventos_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_eventos_usuario ON public.eventos_calendario USING btree (usuario_id);


--
-- Name: idx_expedientes_abogado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_expedientes_abogado ON public.expedientes USING btree (abogado_responsable_id);


--
-- Name: idx_expedientes_etapa; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_expedientes_etapa ON public.expedientes USING btree (etapa_procesal);


--
-- Name: idx_expedientes_numero; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_expedientes_numero ON public.expedientes USING btree (numero);


--
-- Name: idx_manual_recovery_expiration; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_manual_recovery_expiration ON public.manual_recovery_codes USING btree (fecha_expiracion);


--
-- Name: idx_manual_recovery_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_manual_recovery_user ON public.manual_recovery_codes USING btree (usuario_id);


--
-- Name: idx_password_reset_expiracion; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_password_reset_expiracion ON public.password_reset_tokens USING btree (fecha_expiracion);


--
-- Name: idx_password_reset_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_password_reset_usuario ON public.password_reset_tokens USING btree (usuario_id);


--
-- Name: idx_push_sub_endpoint; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_push_sub_endpoint ON public.push_subscriptions USING btree (endpoint);


--
-- Name: idx_push_sub_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_push_sub_usuario ON public.push_subscriptions USING btree (usuario_id);


--
-- Name: idx_termino_archivos_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_termino_archivos_fecha ON public.termino_archivos USING btree (created_at);


--
-- Name: idx_termino_archivos_termino; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_termino_archivos_termino ON public.termino_archivos USING btree (termino_id);


--
-- Name: idx_termino_archivos_tipo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_termino_archivos_tipo ON public.termino_archivos USING btree (tipo_documento);


--
-- Name: idx_termino_contexto_gerencia; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_termino_contexto_gerencia ON public.termino_contextos USING btree (gerencia_id);


--
-- Name: idx_termino_contexto_materia; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_termino_contexto_materia ON public.termino_contextos USING btree (materia_id);


--
-- Name: idx_termino_contexto_origen; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_termino_contexto_origen ON public.termino_contextos USING btree (origen);


--
-- Name: idx_terminos_asunto_independiente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_terminos_asunto_independiente ON public.terminos USING btree (asunto_independiente_id);


--
-- Name: idx_terminos_expediente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_terminos_expediente ON public.terminos USING btree (expediente_id);


--
-- Name: idx_terminos_fecha_vencimiento; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_terminos_fecha_vencimiento ON public.terminos USING btree (fecha_vencimiento);


--
-- Name: idx_usuarios_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usuarios_email ON public.usuarios USING btree (email);


--
-- Name: idx_usuarios_rol; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usuarios_rol ON public.usuarios USING btree (rol);


--
-- Name: uk_asunto_ind_referencia_gerencia; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_asunto_ind_referencia_gerencia ON public.asuntos_independientes USING btree (gerencia_id, lower((referencia)::text));


--
-- Name: ux_usuarios_numero_empleado_lower; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_usuarios_numero_empleado_lower ON public.usuarios USING btree (lower((numero_empleado)::text)) WHERE (numero_empleado IS NOT NULL);


--
-- Name: ux_usuarios_username_lower; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_usuarios_username_lower ON public.usuarios USING btree (lower((username)::text)) WHERE (username IS NOT NULL);


--
-- Name: actividad_expedientes trg_actividad_expedientes_realtime; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_actividad_expedientes_realtime AFTER INSERT OR DELETE OR UPDATE ON public.actividad_expedientes FOR EACH ROW EXECUTE FUNCTION public.fn_notify_realtime();


--
-- Name: actividad_usuarios trg_actividad_usuarios_realtime; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_actividad_usuarios_realtime AFTER INSERT OR DELETE OR UPDATE ON public.actividad_usuarios FOR EACH ROW EXECUTE FUNCTION public.fn_notify_realtime();


--
-- Name: audiencias trg_audiencias_realtime; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_audiencias_realtime AFTER INSERT OR DELETE OR UPDATE ON public.audiencias FOR EACH ROW EXECUTE FUNCTION public.fn_notify_realtime();


--
-- Name: carpetas trg_carpetas_realtime; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_carpetas_realtime AFTER INSERT OR DELETE OR UPDATE ON public.carpetas FOR EACH ROW EXECUTE FUNCTION public.fn_notify_realtime();


--
-- Name: cat_estados trg_cat_estados_realtime; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_cat_estados_realtime AFTER INSERT OR DELETE OR UPDATE ON public.cat_estados FOR EACH ROW EXECUTE FUNCTION public.fn_notify_realtime();


--
-- Name: comentarios trg_comentarios_realtime; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_comentarios_realtime AFTER INSERT OR DELETE OR UPDATE ON public.comentarios FOR EACH ROW EXECUTE FUNCTION public.fn_notify_realtime();


--
-- Name: documentos trg_documentos_realtime; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_documentos_realtime AFTER INSERT OR DELETE OR UPDATE ON public.documentos FOR EACH ROW EXECUTE FUNCTION public.fn_notify_realtime();


--
-- Name: expedientes trg_expedientes_realtime; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_expedientes_realtime AFTER INSERT OR UPDATE ON public.expedientes FOR EACH ROW EXECUTE FUNCTION public.fn_notify_realtime();


--
-- Name: materias trg_materias_realtime; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_materias_realtime AFTER INSERT OR DELETE OR UPDATE ON public.materias FOR EACH ROW EXECUTE FUNCTION public.fn_notify_realtime();


--
-- Name: notificaciones trg_notificaciones_realtime; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_notificaciones_realtime AFTER INSERT ON public.notificaciones FOR EACH ROW EXECUTE FUNCTION public.fn_notify_realtime();


--
-- Name: organos_jurisdiccionales trg_organos_realtime; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_organos_realtime AFTER INSERT OR DELETE OR UPDATE ON public.organos_jurisdiccionales FOR EACH ROW EXECUTE FUNCTION public.fn_notify_realtime();


--
-- Name: terminos trg_terminos_realtime; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_terminos_realtime AFTER INSERT OR DELETE OR UPDATE ON public.terminos FOR EACH ROW EXECUTE FUNCTION public.fn_notify_realtime();


--
-- Name: tipos_audiencia trg_tipos_audiencia_realtime; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_tipos_audiencia_realtime AFTER INSERT OR DELETE OR UPDATE ON public.tipos_audiencia FOR EACH ROW EXECUTE FUNCTION public.fn_notify_realtime();


--
-- Name: cat_tipo_expediente trg_tipos_expediente_realtime; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_tipos_expediente_realtime AFTER INSERT OR DELETE OR UPDATE ON public.cat_tipo_expediente FOR EACH ROW EXECUTE FUNCTION public.fn_notify_realtime();


--
-- Name: organos_jurisdiccionales fk139ctye6aj13npxjakubnphj5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organos_jurisdiccionales
    ADD CONSTRAINT fk139ctye6aj13npxjakubnphj5 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: tipos_termino fk13dd7osd6svnmd9ofaueuiecm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_termino
    ADD CONSTRAINT fk13dd7osd6svnmd9ofaueuiecm FOREIGN KEY (materia_id) REFERENCES public.materias(id);


--
-- Name: manual_recovery_codes fk19k250it1qgl8ndg35clm76r1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.manual_recovery_codes
    ADD CONSTRAINT fk19k250it1qgl8ndg35clm76r1 FOREIGN KEY (emitido_por_usuario_id) REFERENCES public.usuarios(id);


--
-- Name: terminos_presentados fk1brocrvr9fxbfek662npjjn5l; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terminos_presentados
    ADD CONSTRAINT fk1brocrvr9fxbfek662npjjn5l FOREIGN KEY (termino_id) REFERENCES public.terminos(id);


--
-- Name: tipos_audiencia fk1kml027csdf02fbsgarvpaacn; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_audiencia
    ADD CONSTRAINT fk1kml027csdf02fbsgarvpaacn FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: notificaciones fk1mxbjb81ft61gwlh0kabubndc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notificaciones
    ADD CONSTRAINT fk1mxbjb81ft61gwlh0kabubndc FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: terminos fk2dw1fq3hmoqxkj4o25awskock; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terminos
    ADD CONSTRAINT fk2dw1fq3hmoqxkj4o25awskock FOREIGN KEY (contexto_id) REFERENCES public.termino_contextos(id);


--
-- Name: expediente_etapa_periodos fk30lp04ltsrai8mxgpvixk5g9e; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expediente_etapa_periodos
    ADD CONSTRAINT fk30lp04ltsrai8mxgpvixk5g9e FOREIGN KEY (etapa_catalogo_id) REFERENCES public.cat_etapas_procesales(id);


--
-- Name: comentarios fk3tlmxqxx2h083fuvalud2fuoo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comentarios
    ADD CONSTRAINT fk3tlmxqxx2h083fuvalud2fuoo FOREIGN KEY (etapa_catalogo_id) REFERENCES public.cat_etapas_procesales(id);


--
-- Name: preferencias_notificacion fk401s19rwxg52aeg4lnbgdno3d; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.preferencias_notificacion
    ADD CONSTRAINT fk401s19rwxg52aeg4lnbgdno3d FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: recordatorios fk476hi0yoplmls066xtrc5otb; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recordatorios
    ADD CONSTRAINT fk476hi0yoplmls066xtrc5otb FOREIGN KEY (prioridad_id) REFERENCES public.cat_prioridad(id);


--
-- Name: audiencias fk4csl8qte7scamolg89kofnal3; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audiencias
    ADD CONSTRAINT fk4csl8qte7scamolg89kofnal3 FOREIGN KEY (expediente_id) REFERENCES public.expedientes(id);


--
-- Name: terminos fk4gjcyyda149hmwyj6v7fjcot2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terminos
    ADD CONSTRAINT fk4gjcyyda149hmwyj6v7fjcot2 FOREIGN KEY (prioridad_id) REFERENCES public.cat_prioridad(id);


--
-- Name: expedientes fk4ko1kuyjwubjuk8pqc6uj7nkl; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expedientes
    ADD CONSTRAINT fk4ko1kuyjwubjuk8pqc6uj7nkl FOREIGN KEY (organo_jurisdiccional_id) REFERENCES public.organos_jurisdiccionales(id);


--
-- Name: asuntos_independientes fk4yq768s251bxbhh51gstprpnf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asuntos_independientes
    ADD CONSTRAINT fk4yq768s251bxbhh51gstprpnf FOREIGN KEY (responsable_id) REFERENCES public.usuarios(id);


--
-- Name: expedientes fk58skvtk8tcu7wlxc6q6bd9kbt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expedientes
    ADD CONSTRAINT fk58skvtk8tcu7wlxc6q6bd9kbt FOREIGN KEY (materia_id) REFERENCES public.materias(id);


--
-- Name: registros_migracion fk5dyhaybpntdsmofm5ek0c55f4; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.registros_migracion
    ADD CONSTRAINT fk5dyhaybpntdsmofm5ek0c55f4 FOREIGN KEY (lote_id) REFERENCES public.lotes_migracion(id);


--
-- Name: auditoria_eventos fk5vp43v3gpt1fcpqr9jg6v5cqf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditoria_eventos
    ADD CONSTRAINT fk5vp43v3gpt1fcpqr9jg6v5cqf FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: asuntos_independientes fk5wnyf2q3mvcx2mt135t5t4om2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asuntos_independientes
    ADD CONSTRAINT fk5wnyf2q3mvcx2mt135t5t4om2 FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id);


--
-- Name: bitacora_datos fk643xokrf9dialnac7r3x9frxv; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bitacora_datos
    ADD CONSTRAINT fk643xokrf9dialnac7r3x9frxv FOREIGN KEY (etapa_catalogo_id) REFERENCES public.cat_etapas_procesales(id);


--
-- Name: termino_contextos fk6bqlx8xoy5y68c306ugj3x9it; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.termino_contextos
    ADD CONSTRAINT fk6bqlx8xoy5y68c306ugj3x9it FOREIGN KEY (materia_id) REFERENCES public.materias(id);


--
-- Name: cat_tipo_expediente fk72utcny5h8l8oeddw7p2ducwb; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cat_tipo_expediente
    ADD CONSTRAINT fk72utcny5h8l8oeddw7p2ducwb FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: actividad_expedientes fk7jid5v6qqsgdhqep27h3eb7rg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.actividad_expedientes
    ADD CONSTRAINT fk7jid5v6qqsgdhqep27h3eb7rg FOREIGN KEY (etapa_periodo_id) REFERENCES public.expediente_etapa_periodos(id);


--
-- Name: carpetas fk7oriagmf0utemgy3ub8iy7myt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carpetas
    ADD CONSTRAINT fk7oriagmf0utemgy3ub8iy7myt FOREIGN KEY (usuario_creador_id) REFERENCES public.usuarios(id);


--
-- Name: audiencias fk7wchckhcv7ce6x99xekj5e9dx; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audiencias
    ADD CONSTRAINT fk7wchckhcv7ce6x99xekj5e9dx FOREIGN KEY (tipo_audiencia_id) REFERENCES public.tipos_audiencia(id);


--
-- Name: materias fk88y1xy72ng7x0aa9orpy942p7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.materias
    ADD CONSTRAINT fk88y1xy72ng7x0aa9orpy942p7 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: documentos_expediente fk93ari8ojm8gv9htiv787wbxms; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documentos_expediente
    ADD CONSTRAINT fk93ari8ojm8gv9htiv787wbxms FOREIGN KEY (expediente_id) REFERENCES public.expedientes(id);


--
-- Name: expediente_etapa_periodos fk9f7v0ctucoj34v5r936hh34ae; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expediente_etapa_periodos
    ADD CONSTRAINT fk9f7v0ctucoj34v5r936hh34ae FOREIGN KEY (usuario_inicio_id) REFERENCES public.usuarios(id);


--
-- Name: bitacora_datos fka4jw9flw2hdgx2eufguvxbvtk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bitacora_datos
    ADD CONSTRAINT fka4jw9flw2hdgx2eufguvxbvtk FOREIGN KEY (expediente_id) REFERENCES public.expedientes(id);


--
-- Name: usuario_gerencias fkai89eqag1l6q65v4f2w54b2gd; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario_gerencias
    ADD CONSTRAINT fkai89eqag1l6q65v4f2w54b2gd FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id);


--
-- Name: documentos fkapnjaa6i0jmcmp0adsrclumti; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documentos
    ADD CONSTRAINT fkapnjaa6i0jmcmp0adsrclumti FOREIGN KEY (usuario_eliminacion_id) REFERENCES public.usuarios(id);


--
-- Name: tipos_audiencia fkas9o0t29o19lv2sg7q576cal9; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_audiencia
    ADD CONSTRAINT fkas9o0t29o19lv2sg7q576cal9 FOREIGN KEY (perfil_recordatorio_id) REFERENCES public.perfiles_recordatorio_audiencia(id);


--
-- Name: catalogos_migracion fkbcm7hlj12jkry23uud6vp5b7q; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.catalogos_migracion
    ADD CONSTRAINT fkbcm7hlj12jkry23uud6vp5b7q FOREIGN KEY (lote_id) REFERENCES public.lotes_migracion(id);


--
-- Name: audiencia_contextos fkbdu4j8d2ejho98oi08369endx; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audiencia_contextos
    ADD CONSTRAINT fkbdu4j8d2ejho98oi08369endx FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id);


--
-- Name: expedientes fkbichuxp9f2p9t665qhevw9hv; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expedientes
    ADD CONSTRAINT fkbichuxp9f2p9t665qhevw9hv FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id);


--
-- Name: usuario_materias fkblixcl3jed4e5u1r4cbuxo233; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario_materias
    ADD CONSTRAINT fkblixcl3jed4e5u1r4cbuxo233 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: asuntos_independientes fkc18g3nphu0so1fbb9eeggk5tt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asuntos_independientes
    ADD CONSTRAINT fkc18g3nphu0so1fbb9eeggk5tt FOREIGN KEY (materia_id) REFERENCES public.materias(id);


--
-- Name: recordatorios fkc52iceqg5fu1wn3jrijliai38; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recordatorios
    ADD CONSTRAINT fkc52iceqg5fu1wn3jrijliai38 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: terminos fkcoe42fxbdjqjek2ks0ghkmowd; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terminos
    ADD CONSTRAINT fkcoe42fxbdjqjek2ks0ghkmowd FOREIGN KEY (expediente_id) REFERENCES public.expedientes(id);


--
-- Name: bitacora_datos fkd2e3cn3q5cbep5g7f8tm3t660; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bitacora_datos
    ADD CONSTRAINT fkd2e3cn3q5cbep5g7f8tm3t660 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: expediente_etapa_periodos fkdr15p138nc736eytkegkknvqg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expediente_etapa_periodos
    ADD CONSTRAINT fkdr15p138nc736eytkegkknvqg FOREIGN KEY (usuario_fin_id) REFERENCES public.usuarios(id);


--
-- Name: comentarios fkdts62yj83qe3k748cgcjvm48r; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comentarios
    ADD CONSTRAINT fkdts62yj83qe3k748cgcjvm48r FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: carpetas fkel65g9qwjoep94aixiveex7fm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carpetas
    ADD CONSTRAINT fkel65g9qwjoep94aixiveex7fm FOREIGN KEY (carpeta_padre_id) REFERENCES public.carpetas(id);


--
-- Name: tipos_termino fkfcsr8rwtfpss71hj964jvbms2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_termino
    ADD CONSTRAINT fkfcsr8rwtfpss71hj964jvbms2 FOREIGN KEY (perfil_alerta_id) REFERENCES public.perfiles_alerta_termino(id);


--
-- Name: tipos_termino fkfk34eqvuyx2gunbal9jc63up9; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_termino
    ADD CONSTRAINT fkfk34eqvuyx2gunbal9jc63up9 FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id);


--
-- Name: colaboradores_expediente fkfmyo4j16g3shpn1jqc846sril; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.colaboradores_expediente
    ADD CONSTRAINT fkfmyo4j16g3shpn1jqc846sril FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: cat_etapas_procesales fkge1qxkyu56geshso8frtqeyam; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cat_etapas_procesales
    ADD CONSTRAINT fkge1qxkyu56geshso8frtqeyam FOREIGN KEY (parent_id) REFERENCES public.cat_etapas_procesales(id);


--
-- Name: usuario_materias fkgmypxwp3xdvi5ckk7xsd6flna; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario_materias
    ADD CONSTRAINT fkgmypxwp3xdvi5ckk7xsd6flna FOREIGN KEY (materia_id) REFERENCES public.materias(id);


--
-- Name: audiencias fkgsv5padg0wab4wkm8no4mfx59; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audiencias
    ADD CONSTRAINT fkgsv5padg0wab4wkm8no4mfx59 FOREIGN KEY (usuario_delegado_id) REFERENCES public.usuarios(id);


--
-- Name: audiencias fkgy6f4qct07n42s9vecon4e792; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audiencias
    ADD CONSTRAINT fkgy6f4qct07n42s9vecon4e792 FOREIGN KEY (contexto_id) REFERENCES public.audiencia_contextos(id);


--
-- Name: asuntos_independientes fkgyhrglkc3qjocvb9g2fwua88t; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asuntos_independientes
    ADD CONSTRAINT fkgyhrglkc3qjocvb9g2fwua88t FOREIGN KEY (expediente_destino_id) REFERENCES public.expedientes(id);


--
-- Name: termino_contextos fkhcuyhk60prwmu8wdx5lq7dw8t; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.termino_contextos
    ADD CONSTRAINT fkhcuyhk60prwmu8wdx5lq7dw8t FOREIGN KEY (organo_jurisdiccional_id) REFERENCES public.organos_jurisdiccionales(id);


--
-- Name: notificaciones fkhedeo9jm98y2dq7nbpp9fg7q1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notificaciones
    ADD CONSTRAINT fkhedeo9jm98y2dq7nbpp9fg7q1 FOREIGN KEY (prioridad_id) REFERENCES public.cat_prioridad(id);


--
-- Name: termino_archivos fkhgdafpg9sqy4788ma60xfixbx; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.termino_archivos
    ADD CONSTRAINT fkhgdafpg9sqy4788ma60xfixbx FOREIGN KEY (termino_id) REFERENCES public.terminos(id);


--
-- Name: usuarios fkhy8fli5s85u3iut9tvcjggx77; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT fkhy8fli5s85u3iut9tvcjggx77 FOREIGN KEY (creado_por_id) REFERENCES public.usuarios(id);


--
-- Name: documentos fki77q0aw3d7cix0fty95p5halt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documentos
    ADD CONSTRAINT fki77q0aw3d7cix0fty95p5halt FOREIGN KEY (carpeta_id) REFERENCES public.carpetas(id);


--
-- Name: asuntos_independientes fkinx0v2wqy1nqexc6t5ujitga7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asuntos_independientes
    ADD CONSTRAINT fkinx0v2wqy1nqexc6t5ujitga7 FOREIGN KEY (vinculado_por_id) REFERENCES public.usuarios(id);


--
-- Name: actividad_usuarios fkiwarg06waxi0wm0nmfyaphl7k; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.actividad_usuarios
    ADD CONSTRAINT fkiwarg06waxi0wm0nmfyaphl7k FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: termino_contextos fkiyg3nqpnf53flv1ovxvp94t5h; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.termino_contextos
    ADD CONSTRAINT fkiyg3nqpnf53flv1ovxvp94t5h FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id);


--
-- Name: actividad_expedientes fkiyti19r0fan5qf0u7snbxlmbv; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.actividad_expedientes
    ADD CONSTRAINT fkiyti19r0fan5qf0u7snbxlmbv FOREIGN KEY (etapa_catalogo_id) REFERENCES public.cat_etapas_procesales(id);


--
-- Name: expedientes fkjd2xfb8uvwn3bfcsxc8vy3clj; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expedientes
    ADD CONSTRAINT fkjd2xfb8uvwn3bfcsxc8vy3clj FOREIGN KEY (prioridad_id) REFERENCES public.cat_prioridad(id);


--
-- Name: usuario_permisos_adicionales fkjml13xi25qa1awrohharg2hc3; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario_permisos_adicionales
    ADD CONSTRAINT fkjml13xi25qa1awrohharg2hc3 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: actividad_expedientes fkjnko884f21s9u9qrnx65c9mt1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.actividad_expedientes
    ADD CONSTRAINT fkjnko884f21s9u9qrnx65c9mt1 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: configuraciones_alerta fkjuixqrp11exyu0hxda62mjaly; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.configuraciones_alerta
    ADD CONSTRAINT fkjuixqrp11exyu0hxda62mjaly FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id);


--
-- Name: usuario_gerencias fkk2iku7uhxeu1b8r5j2qo8kl82; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario_gerencias
    ADD CONSTRAINT fkk2iku7uhxeu1b8r5j2qo8kl82 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: cat_tipo_expediente fkk792qaf01pxxgna4hd5yakesh; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cat_tipo_expediente
    ADD CONSTRAINT fkk792qaf01pxxgna4hd5yakesh FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id);


--
-- Name: eventos_calendario fkkerk9013d75pe0retvvpoali4; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.eventos_calendario
    ADD CONSTRAINT fkkerk9013d75pe0retvvpoali4 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: materias fkkf13pj696ynk5lfcetip9d5oy; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.materias
    ADD CONSTRAINT fkkf13pj696ynk5lfcetip9d5oy FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id);


--
-- Name: terminos fkkkmekrrglh9b4ikav3m0ni0bn; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terminos
    ADD CONSTRAINT fkkkmekrrglh9b4ikav3m0ni0bn FOREIGN KEY (asunto_independiente_id) REFERENCES public.asuntos_independientes(id);


--
-- Name: cat_etapas_procesales fklb9pgtcvp7e3r0xa0dar7e66o; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cat_etapas_procesales
    ADD CONSTRAINT fklb9pgtcvp7e3r0xa0dar7e66o FOREIGN KEY (materia_id) REFERENCES public.materias(id);


--
-- Name: tipos_audiencia fklhw4n232t69p6wb8mkmubvkl6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_audiencia
    ADD CONSTRAINT fklhw4n232t69p6wb8mkmubvkl6 FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id);


--
-- Name: termino_archivos fklrxl92lqj7h66ktvsk2bdw68u; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.termino_archivos
    ADD CONSTRAINT fklrxl92lqj7h66ktvsk2bdw68u FOREIGN KEY (usuario_subida_id) REFERENCES public.usuarios(id);


--
-- Name: colaboradores_expediente fkmajsh6pilw2o7d2lusf1yfewt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.colaboradores_expediente
    ADD CONSTRAINT fkmajsh6pilw2o7d2lusf1yfewt FOREIGN KEY (expediente_id) REFERENCES public.expedientes(id);


--
-- Name: expedientes fkmb4o0kv5lstsw3v4u18lxl6em; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expedientes
    ADD CONSTRAINT fkmb4o0kv5lstsw3v4u18lxl6em FOREIGN KEY (etapa_catalogo_id) REFERENCES public.cat_etapas_procesales(id);


--
-- Name: audiencias fkmdiq6bskf4fgwb03xajc78erx; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audiencias
    ADD CONSTRAINT fkmdiq6bskf4fgwb03xajc78erx FOREIGN KEY (asunto_independiente_id) REFERENCES public.asuntos_independientes(id);


--
-- Name: lotes_migracion fkml1x7t5vox3isbj11ai4dvx8b; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lotes_migracion
    ADD CONSTRAINT fkml1x7t5vox3isbj11ai4dvx8b FOREIGN KEY (gerencia_global_id) REFERENCES public.gerencias(id);


--
-- Name: documentos_expediente fkmnmduqxe42khc46ssomlsi4ki; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documentos_expediente
    ADD CONSTRAINT fkmnmduqxe42khc46ssomlsi4ki FOREIGN KEY (uploaded_by) REFERENCES public.usuarios(id);


--
-- Name: terminos fko59hv5s9ul7bqg35p4oanxlkr; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terminos
    ADD CONSTRAINT fko59hv5s9ul7bqg35p4oanxlkr FOREIGN KEY (abogado_responsable_id) REFERENCES public.usuarios(id);


--
-- Name: expedientes fkof8a43swfcvf2dv5mo6q5thd6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expedientes
    ADD CONSTRAINT fkof8a43swfcvf2dv5mo6q5thd6 FOREIGN KEY (abogado_responsable_id) REFERENCES public.usuarios(id);


--
-- Name: terminos fkofjahx1l766xihpdmmxav6ia3; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terminos
    ADD CONSTRAINT fkofjahx1l766xihpdmmxav6ia3 FOREIGN KEY (tipo_termino_id) REFERENCES public.tipos_termino(id);


--
-- Name: documentos fkou5fwjxrh4strb85eehbd3535; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documentos
    ADD CONSTRAINT fkou5fwjxrh4strb85eehbd3535 FOREIGN KEY (usuario_subida_id) REFERENCES public.usuarios(id);


--
-- Name: audiencia_contextos fkp1paxjyvpxulyqnukqmd0d13c; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audiencia_contextos
    ADD CONSTRAINT fkp1paxjyvpxulyqnukqmd0d13c FOREIGN KEY (organo_jurisdiccional_id) REFERENCES public.organos_jurisdiccionales(id);


--
-- Name: configuraciones_alerta fkp2ofjofsj1ohidlux3q1jylxr; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.configuraciones_alerta
    ADD CONSTRAINT fkp2ofjofsj1ohidlux3q1jylxr FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: organos_jurisdiccionales fkp3dc7r1182p73giiehvc1a3kw; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organos_jurisdiccionales
    ADD CONSTRAINT fkp3dc7r1182p73giiehvc1a3kw FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id);


--
-- Name: password_reset_tokens fkp3log76r68owjybas53j8jaig; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT fkp3log76r68owjybas53j8jaig FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: cat_etapas_procesales fkpatysimxcmkefbrjdxgmk2jy3; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cat_etapas_procesales
    ADD CONSTRAINT fkpatysimxcmkefbrjdxgmk2jy3 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: audiencias fkpkiu3klwkc0lr9f9ebbpwsqc1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audiencias
    ADD CONSTRAINT fkpkiu3klwkc0lr9f9ebbpwsqc1 FOREIGN KEY (abogado_comparece_id) REFERENCES public.usuarios(id);


--
-- Name: configuraciones_semaforo fkpncb84gi9iflcalxgkyf6chdk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.configuraciones_semaforo
    ADD CONSTRAINT fkpncb84gi9iflcalxgkyf6chdk FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id);


--
-- Name: manual_recovery_codes fkpvpeg5rms1q9k2pghu0bmo5dt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.manual_recovery_codes
    ADD CONSTRAINT fkpvpeg5rms1q9k2pghu0bmo5dt FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: auditoria_sesiones fkpxnctjgkcgh29p5ux063wxj06; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditoria_sesiones
    ADD CONSTRAINT fkpxnctjgkcgh29p5ux063wxj06 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: actividad_expedientes fkpxpfk1j1efqgo8c0abu3rmj0x; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.actividad_expedientes
    ADD CONSTRAINT fkpxpfk1j1efqgo8c0abu3rmj0x FOREIGN KEY (expediente_id) REFERENCES public.expedientes(id);


--
-- Name: expedientes fkq1uvxy2ws1qxvxrgqovf9tq1s; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expedientes
    ADD CONSTRAINT fkq1uvxy2ws1qxvxrgqovf9tq1s FOREIGN KEY (tipo_expediente_id) REFERENCES public.cat_tipo_expediente(id);


--
-- Name: audiencias_desahogadas fkq40pjs1rrg3mj5qdcl74hawwa; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audiencias_desahogadas
    ADD CONSTRAINT fkq40pjs1rrg3mj5qdcl74hawwa FOREIGN KEY (audiencia_id) REFERENCES public.audiencias(id);


--
-- Name: preferencias_notificacion_evento fkq824i3pvktkmtaa0m7l1g1ry0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.preferencias_notificacion_evento
    ADD CONSTRAINT fkq824i3pvktkmtaa0m7l1g1ry0 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: tipos_audiencia fkq8hi3dl6lksatbyjiu7li2ytw; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_audiencia
    ADD CONSTRAINT fkq8hi3dl6lksatbyjiu7li2ytw FOREIGN KEY (materia_id) REFERENCES public.materias(id);


--
-- Name: usuarios fkrel3m11jpih4a77lseyy3h3ad; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT fkrel3m11jpih4a77lseyy3h3ad FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id);


--
-- Name: cat_estados fks243iav8cbu2feurmh2qjaiy6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cat_estados
    ADD CONSTRAINT fks243iav8cbu2feurmh2qjaiy6 FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id);


--
-- Name: expediente_etapa_periodos fks7f29x38cpo16dldbmk1d9ipv; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expediente_etapa_periodos
    ADD CONSTRAINT fks7f29x38cpo16dldbmk1d9ipv FOREIGN KEY (expediente_id) REFERENCES public.expedientes(id);


--
-- Name: cat_estados fksvk5hct1ehr5jejuecfr59pih; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cat_estados
    ADD CONSTRAINT fksvk5hct1ehr5jejuecfr59pih FOREIGN KEY (gerencia_id) REFERENCES public.gerencias(id);


--
-- Name: lotes_migracion fkt3ttkjr7t5lfi88ketoe4osee; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lotes_migracion
    ADD CONSTRAINT fkt3ttkjr7t5lfi88ketoe4osee FOREIGN KEY (usuario_responsable_id) REFERENCES public.usuarios(id);


--
-- Name: bitacora_datos fkt63k51h3gupu56iw9cntnodni; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bitacora_datos
    ADD CONSTRAINT fkt63k51h3gupu56iw9cntnodni FOREIGN KEY (etapa_periodo_id) REFERENCES public.expediente_etapa_periodos(id);


--
-- Name: audiencia_contextos fktfcon5wnhwo0ph22a2wt5y6im; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audiencia_contextos
    ADD CONSTRAINT fktfcon5wnhwo0ph22a2wt5y6im FOREIGN KEY (materia_id) REFERENCES public.materias(id);


--
-- Name: push_subscriptions push_subscriptions_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.push_subscriptions
    ADD CONSTRAINT push_subscriptions_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--
