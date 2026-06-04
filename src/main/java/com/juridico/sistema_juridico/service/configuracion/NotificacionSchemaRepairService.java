package com.juridico.sistema_juridico.service.configuracion;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificacionSchemaRepairService implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public NotificacionSchemaRepairService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        asegurarColumnaEventoEnNotificaciones();
    }

    private void asegurarColumnaEventoEnNotificaciones() {
        Boolean existe = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name = 'notificaciones'
                      AND column_name = 'evento'
                )
                """, Boolean.class);

        if (Boolean.TRUE.equals(existe)) {
            return;
        }

        jdbcTemplate.execute("ALTER TABLE notificaciones ADD COLUMN evento varchar(80)");

        jdbcTemplate.execute("""
                UPDATE notificaciones
                SET evento = CASE UPPER(COALESCE(tipo, ''))
                    WHEN 'EXPEDIENTE' THEN 'EXPEDIENTE_NUEVO'
                    WHEN 'ASIGNACION' THEN 'EXPEDIENTE_REASIGNADO'
                    WHEN 'OBSERVACION' THEN 'EXPEDIENTE_OBSERVACION_NUEVA'
                    WHEN 'TERMINO' THEN 'TERMINO_NUEVO'
                    WHEN 'AUDIENCIA' THEN 'AUDIENCIA_NUEVA'
                    WHEN 'SISTEMA' THEN 'SEGURIDAD_PASSWORD_POR_VENCER'
                    ELSE 'SEGURIDAD_PASSWORD_POR_VENCER'
                END
                WHERE evento IS NULL
                """);

        jdbcTemplate.execute("""
                ALTER TABLE notificaciones
                ALTER COLUMN evento SET NOT NULL
                """);
    }
}
