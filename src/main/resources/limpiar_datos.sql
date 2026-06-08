-- =============================================================================
-- SCRIPT DE LIMPIEZA DE DATOS OPERATIVOS (SISTEMA JURÍDICO)
-- =============================================================================
-- Este script vacía las tablas transaccionales y de migración.
-- CONSERVA: Usuarios, Gerencias, Materias y sus relaciones.
-- =============================================================================

BEGIN;

-- Desactivar temporalmente los triggers/restricciones (opcional, para evitar colisiones de FK durante el proceso)
SET CONSTRAINTS ALL DEFERRED;

-- 1. LIMPIEZA DE DATOS RELACIONADOS CON TÉRMINOS
TRUNCATE TABLE terminos_presentados RESTART IDENTITY CASCADE;
TRUNCATE TABLE termino_archivos RESTART IDENTITY CASCADE;
TRUNCATE TABLE terminos RESTART IDENTITY CASCADE;
TRUNCATE TABLE termino_contextos RESTART IDENTITY CASCADE;

-- 2. LIMPIEZA DE DATOS RELACIONADOS CON AUDIENCIAS
TRUNCATE TABLE audiencias_desahogadas RESTART IDENTITY CASCADE;
TRUNCATE TABLE audiencias RESTART IDENTITY CASCADE;

-- 3. LIMPIEZA DE GESTOR DOCUMENTAL (ASOCIADOS A EXPEDIENTES)
TRUNCATE TABLE documentos RESTART IDENTITY CASCADE;
TRUNCATE TABLE carpetas RESTART IDENTITY CASCADE;

-- 4. LIMPIEZA DE EXPEDIENTES Y BITÁCORAS
TRUNCATE TABLE colaboradores_expediente RESTART IDENTITY CASCADE;
TRUNCATE TABLE expediente_etiquetas RESTART IDENTITY CASCADE;
TRUNCATE TABLE actividad_expedientes RESTART IDENTITY CASCADE;
TRUNCATE TABLE bitacora_datos RESTART IDENTITY CASCADE;
TRUNCATE TABLE expedientes RESTART IDENTITY CASCADE;

-- 5. LIMPIEZA DE NOTIFICACIONES, RECORDATORIOS Y CALENDARIO
TRUNCATE TABLE notificaciones RESTART IDENTITY CASCADE;
TRUNCATE TABLE recordatorios RESTART IDENTITY CASCADE;
TRUNCATE TABLE eventos_calendario RESTART IDENTITY CASCADE;

-- 6. LIMPIEZA DE HISTORIAL Y CONFIGURACIONES DE MIGRACIÓN
TRUNCATE TABLE registros_migracion RESTART IDENTITY CASCADE;
TRUNCATE TABLE catalogos_migracion RESTART IDENTITY CASCADE;
TRUNCATE TABLE lotes_migracion RESTART IDENTITY CASCADE;

-- 7. LIMPIEZA DE CATÁLOGOS DINÁMICOS / SECUNDARIOS (Opcional)
-- Descomenta estas líneas si deseas borrar también los catálogos secundarios
-- que se importaron desde la migración de datos.
-- TRUNCATE TABLE organos_jurisdiccionales RESTART IDENTITY CASCADE;
-- TRUNCATE TABLE tipos_audiencia RESTART IDENTITY CASCADE;
-- TRUNCATE TABLE etiquetas RESTART IDENTITY CASCADE;

COMMIT;

-- =============================================================================
-- VERIFICACIÓN DE TABLAS QUE FUERON PRESERVADAS
-- =============================================================================
-- SELECT COUNT(*) FROM usuarios;
-- SELECT COUNT(*) FROM gerencias;
-- SELECT COUNT(*) FROM materias;
-- SELECT COUNT(*) FROM usuario_materias;
-- SELECT COUNT(*) FROM usuario_permisos_adicionales;
-- =============================================================================
