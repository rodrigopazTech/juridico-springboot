-- Permite reprogramar una tarea sin borrar el historial de notificaciones previo.
-- Es idempotente: puede ejecutarse más de una vez durante un despliegue.
BEGIN;

ALTER TABLE recordatorios
    ADD COLUMN IF NOT EXISTS recordatorio_revision integer NOT NULL DEFAULT 0;

UPDATE recordatorios
SET recordatorio_revision = 0
WHERE recordatorio_revision IS NULL;

COMMENT ON COLUMN recordatorios.recordatorio_revision IS
    'Incrementa cuando cambia la fecha u hora para permitir un nuevo aviso de la tarea.';

COMMIT;
