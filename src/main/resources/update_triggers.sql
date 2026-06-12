-- 1. Actualizar la función de notificación en tiempo real
CREATE OR REPLACE FUNCTION public.fn_notify_realtime()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
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
            'fecha',               COALESCE(r.updated_at, r.created_at, CURRENT_TIMESTAMP),
            'operacion',           TG_OP
        )::text;

    ELSIF TG_TABLE_NAME = 'tipos_expediente' THEN
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
$function$;

-- 2. Crear disparadores (triggers) si no existen
DROP TRIGGER IF EXISTS trg_expedientes_realtime ON expedientes;
CREATE TRIGGER trg_expedientes_realtime
AFTER INSERT OR UPDATE ON expedientes
FOR EACH ROW EXECUTE FUNCTION fn_notify_realtime();

DROP TRIGGER IF EXISTS trg_notificaciones_realtime ON notificaciones;
CREATE TRIGGER trg_notificaciones_realtime
AFTER INSERT ON notificaciones
FOR EACH ROW EXECUTE FUNCTION fn_notify_realtime();

DROP TRIGGER IF EXISTS trg_terminos_realtime ON terminos;
CREATE TRIGGER trg_terminos_realtime
AFTER INSERT OR UPDATE OR DELETE ON terminos
FOR EACH ROW EXECUTE FUNCTION fn_notify_realtime();

DROP TRIGGER IF EXISTS trg_audiencias_realtime ON audiencias;
CREATE TRIGGER trg_audiencias_realtime
AFTER INSERT OR UPDATE OR DELETE ON audiencias
FOR EACH ROW EXECUTE FUNCTION fn_notify_realtime();

DROP TRIGGER IF EXISTS trg_documentos_realtime ON documentos;
CREATE TRIGGER trg_documentos_realtime
AFTER INSERT OR UPDATE OR DELETE ON documentos
FOR EACH ROW EXECUTE FUNCTION fn_notify_realtime();

DROP TRIGGER IF EXISTS trg_carpetas_realtime ON carpetas;
CREATE TRIGGER trg_carpetas_realtime
AFTER INSERT OR UPDATE OR DELETE ON carpetas
FOR EACH ROW EXECUTE FUNCTION fn_notify_realtime();

DROP TRIGGER IF EXISTS trg_comentarios_realtime ON comentarios;
CREATE TRIGGER trg_comentarios_realtime
AFTER INSERT OR UPDATE OR DELETE ON comentarios
FOR EACH ROW EXECUTE FUNCTION fn_notify_realtime();

DROP TRIGGER IF EXISTS trg_actividad_expedientes_realtime ON actividad_expedientes;
CREATE TRIGGER trg_actividad_expedientes_realtime
AFTER INSERT OR UPDATE OR DELETE ON actividad_expedientes
FOR EACH ROW EXECUTE FUNCTION fn_notify_realtime();

DROP TRIGGER IF EXISTS trg_actividad_usuarios_realtime ON actividad_usuarios;
CREATE TRIGGER trg_actividad_usuarios_realtime
AFTER INSERT OR UPDATE OR DELETE ON actividad_usuarios
FOR EACH ROW EXECUTE FUNCTION fn_notify_realtime();

DROP TRIGGER IF EXISTS trg_organos_realtime ON organos_jurisdiccionales;
CREATE TRIGGER trg_organos_realtime
AFTER INSERT OR UPDATE OR DELETE ON organos_jurisdiccionales
FOR EACH ROW EXECUTE FUNCTION fn_notify_realtime();

DROP TRIGGER IF EXISTS trg_tipos_audiencia_realtime ON tipos_audiencia;
CREATE TRIGGER trg_tipos_audiencia_realtime
AFTER INSERT OR UPDATE OR DELETE ON tipos_audiencia
FOR EACH ROW EXECUTE FUNCTION fn_notify_realtime();

DROP TRIGGER IF EXISTS trg_tipos_expediente_realtime ON tipos_expediente;
CREATE TRIGGER trg_tipos_expediente_realtime
AFTER INSERT OR UPDATE OR DELETE ON tipos_expediente
FOR EACH ROW EXECUTE FUNCTION fn_notify_realtime();

DROP TRIGGER IF EXISTS trg_materias_realtime ON materias;
CREATE TRIGGER trg_materias_realtime
AFTER INSERT OR UPDATE OR DELETE ON materias
FOR EACH ROW EXECUTE FUNCTION fn_notify_realtime();

DROP TRIGGER IF EXISTS trg_cat_estados_realtime ON cat_estados;
CREATE TRIGGER trg_cat_estados_realtime
AFTER INSERT OR UPDATE OR DELETE ON cat_estados
FOR EACH ROW EXECUTE FUNCTION fn_notify_realtime();