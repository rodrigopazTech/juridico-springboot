CREATE TABLE IF NOT EXISTS manual_recovery_codes (
    id BIGSERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuarios(id),
    emitido_por_usuario_id INTEGER NOT NULL REFERENCES usuarios(id),
    codigo_hash VARCHAR(255) NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    origen VARCHAR(50) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion TIMESTAMP NOT NULL,
    fecha_uso TIMESTAMP,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_manual_recovery_user ON manual_recovery_codes(usuario_id);
CREATE INDEX IF NOT EXISTS idx_manual_recovery_expiration ON manual_recovery_codes(fecha_expiracion);
