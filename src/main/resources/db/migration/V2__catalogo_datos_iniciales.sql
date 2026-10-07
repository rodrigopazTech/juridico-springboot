-- ============================================================================
-- V2__catalogo_datos_iniciales.sql
-- Datos iniciales indispensables e idempotentes para Reglas de Alerta y Recordatorios
-- ============================================================================

INSERT INTO public.perfiles_alerta_termino (
    codigo, nombre, descripcion, modo, amarillo_dias, rojo_dias, version, estado, vigente_desde, created_at, updated_at
)
VALUES
    ('PLAZO_CORTO', 'Plazo corto', 'Perfil inicial para plazos calculados por el sistema.', 'DIAS_RESTANTES', 2, 1, 1, 'PUBLICADO', CURRENT_DATE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('PLAZO_MEDIO', 'Plazo medio', 'Perfil inicial para plazos calculados por el sistema.', 'DIAS_RESTANTES', 5, 2, 1, 'PUBLICADO', CURRENT_DATE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (codigo, version) DO NOTHING;

INSERT INTO public.perfiles_alerta_termino (
    codigo, nombre, descripcion, modo, amarillo_porcentaje, amarillo_minimo_dias, amarillo_maximo_dias, rojo_porcentaje, rojo_minimo_dias, rojo_maximo_dias, version, estado, vigente_desde, created_at, updated_at
)
VALUES
    ('PLAZO_VARIABLE_ESTANDAR', 'Plazo variable estándar', 'Perfil proporcional limitado para vencimientos informados al registrar.', 'PROPORCIONAL_LIMITADO', 50, 3, 10, 20, 1, 4, 1, 'PUBLICADO', CURRENT_DATE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (codigo, version) DO NOTHING;

INSERT INTO public.perfiles_recordatorio_audiencia (
    codigo, nombre, descripcion, anticipacion_amarilla_horas, anticipacion_roja_horas, recordatorios_minutos, version, estado, vigente_desde, created_at, updated_at
)
VALUES
    ('AUDIENCIA_ESTANDAR', 'Audiencia estándar', 'Perfil inicial de proximidad para audiencias.', 72, 24, '10080,4320,1440,120', 1, 'PUBLICADO', CURRENT_DATE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (codigo, version) DO NOTHING;
