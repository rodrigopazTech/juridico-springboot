-- Verificacion de solo lectura del esquema requerido por las migraciones 02 a 07.
-- No crea, modifica ni elimina datos u objetos.

SELECT current_database() AS base_datos,
       current_user AS usuario_revision,
       current_setting('server_version') AS version_postgresql;

WITH expected(table_name) AS (
    VALUES
        ('usuarios'),
        ('manual_recovery_codes'),
        ('recordatorios'),
        ('perfiles_alerta_termino'),
        ('perfiles_recordatorio_audiencia'),
        ('asuntos_independientes'),
        ('configuraciones_correo_smtp')
)
SELECT 'TABLA' AS tipo,
       table_name AS objeto,
       CASE WHEN to_regclass('public.' || table_name) IS NOT NULL THEN 'OK' ELSE 'FALTA' END AS resultado
FROM expected
ORDER BY table_name;

WITH expected(table_name, column_name) AS (
    VALUES
        ('usuarios', 'email'),
        ('manual_recovery_codes', 'codigo_hash'),
        ('manual_recovery_codes', 'fecha_expiracion'),
        ('recordatorios', 'recordatorio_revision'),
        ('tipos_termino', 'codigo'),
        ('tipos_termino', 'modo_plazo'),
        ('tipos_termino', 'perfil_alerta_id'),
        ('terminos', 'asunto_independiente_id'),
        ('terminos', 'estado_operativo'),
        ('terminos', 'estado_acuse'),
        ('audiencias', 'asunto_independiente_id'),
        ('audiencias', 'estado_operativo'),
        ('audiencias', 'estado_acta'),
        ('configuraciones_correo_smtp', 'password_cifrado'),
        ('configuraciones_correo_smtp', 'correo_prueba'),
        ('configuraciones_correo_smtp', 'proxima_fecha_cambio'),
        ('configuraciones_correo_smtp', 'estado'),
        ('configuraciones_correo_smtp', 'fecha_activacion'),
        ('configuraciones_correo_smtp', 'version')
)
SELECT 'COLUMNA' AS tipo,
       e.table_name || '.' || e.column_name AS objeto,
       CASE WHEN c.column_name IS NOT NULL THEN 'OK' ELSE 'FALTA' END AS resultado
FROM expected e
LEFT JOIN information_schema.columns c
       ON c.table_schema = 'public'
      AND c.table_name = e.table_name
      AND c.column_name = e.column_name
ORDER BY e.table_name, e.column_name;

SELECT 'REGLA' AS tipo,
       'usuarios.email permite NULL' AS objeto,
       CASE WHEN c.is_nullable = 'YES' THEN 'OK' ELSE 'FALTA' END AS resultado
FROM (VALUES (1)) expected(dummy)
LEFT JOIN information_schema.columns c
       ON c.table_schema = 'public'
      AND c.table_name = 'usuarios'
      AND c.column_name = 'email';

WITH expected(trigger_name) AS (
    VALUES
        ('trg_expedientes_realtime'),
        ('trg_notificaciones_realtime'),
        ('trg_terminos_realtime'),
        ('trg_audiencias_realtime'),
        ('trg_documentos_realtime'),
        ('trg_carpetas_realtime'),
        ('trg_comentarios_realtime'),
        ('trg_actividad_expedientes_realtime'),
        ('trg_actividad_usuarios_realtime'),
        ('trg_organos_realtime'),
        ('trg_tipos_audiencia_realtime'),
        ('trg_tipos_expediente_realtime'),
        ('trg_materias_realtime'),
        ('trg_cat_estados_realtime')
)
SELECT 'TRIGGER' AS tipo,
       e.trigger_name AS objeto,
       CASE WHEN EXISTS (
           SELECT 1
           FROM information_schema.triggers t
           WHERE t.trigger_schema = 'public'
             AND t.trigger_name = e.trigger_name
       ) THEN 'OK' ELSE 'FALTA' END AS resultado
FROM expected e
ORDER BY e.trigger_name;

WITH expected(index_name) AS (
    VALUES
        ('idx_manual_recovery_user'),
        ('idx_manual_recovery_expiration'),
        ('idx_asuntos_indep_folio'),
        ('idx_terminos_asunto_indep'),
        ('idx_audiencias_asunto_indep'),
        ('idx_config_correo_estado'),
        ('idx_config_correo_creacion'),
        ('uq_config_correo_unica_activa')
)
SELECT 'INDICE' AS tipo,
       e.index_name AS objeto,
       CASE WHEN i.indexname IS NOT NULL THEN 'OK' ELSE 'FALTA' END AS resultado
FROM expected e
LEFT JOIN pg_indexes i
       ON i.schemaname = 'public'
      AND i.indexname = e.index_name
ORDER BY e.index_name;

SELECT 'FUNCION' AS tipo,
       'public.fn_notify_realtime()' AS objeto,
       CASE WHEN to_regprocedure('public.fn_notify_realtime()') IS NOT NULL THEN 'OK' ELSE 'FALTA' END AS resultado;
