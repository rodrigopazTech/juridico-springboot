CREATE TABLE IF NOT EXISTS configuraciones_correo_smtp (
    id BIGSERIAL PRIMARY KEY,
    password_cifrado TEXT NOT NULL,
    correo_prueba VARCHAR(254) NOT NULL,
    proxima_fecha_cambio DATE NOT NULL,
    estado VARCHAR(20) NOT NULL,
    prueba_exitosa BOOLEAN,
    ultimo_resultado_prueba VARCHAR(500),
    fecha_prueba TIMESTAMP,
    probada_por VARCHAR(120),
    fecha_activacion TIMESTAMP,
    activada_por VARCHAR(120),
    fecha_retiro TIMESTAMP,
    creada_por VARCHAR(120) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_config_correo_estado
        CHECK (estado IN ('PENDIENTE', 'PROBADA', 'ACTIVA', 'ANTERIOR'))
);

CREATE INDEX IF NOT EXISTS idx_config_correo_estado
    ON configuraciones_correo_smtp (estado);

CREATE INDEX IF NOT EXISTS idx_config_correo_creacion
    ON configuraciones_correo_smtp (fecha_creacion);

CREATE UNIQUE INDEX IF NOT EXISTS uq_config_correo_unica_activa
    ON configuraciones_correo_smtp (estado)
    WHERE estado = 'ACTIVA';
