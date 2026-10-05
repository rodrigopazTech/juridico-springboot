package com.juridico.sistema_juridico.config;

import java.sql.Connection;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ajustes idempotentes para instalaciones creadas antes del modelo versionado
 * de reglas de alerta.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class DatabaseSchemaCompatibilityInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaCompatibilityInitializer.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaCompatibilityInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (!"PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())) {
                return;
            }
        }

        eliminarUnicidadLegacyDeCodigo("perfiles_alerta_termino");
        eliminarUnicidadLegacyDeCodigo("perfiles_recordatorio_audiencia");
        jdbcTemplate.execute("ALTER TABLE tipos_termino ALTER COLUMN dias_predeterminados DROP NOT NULL");
        reemplazarCheckEnum("termino_archivos", "tipo_documento", "ck_termino_archivos_tipo_documento",
                "tipo_documento IN ('BORRADOR','ACUSE','NOTA_INFORMATIVA')");
        reemplazarCheckEnum("audiencias", "estatus_audiencia", "ck_audiencias_estatus_audiencia",
                "estatus_audiencia IN ('PENDIENTE','CON_ACTA','SUSPENDIDA','CONCLUIDA')");
        reemplazarCheck("terminos", "ck_terminos_origen_exclusivo",
                "(expediente_id IS NOT NULL) <> (asunto_independiente_id IS NOT NULL)");
        reemplazarCheck("audiencias", "ck_audiencias_origen_exclusivo",
                "(expediente_id IS NOT NULL) <> (asunto_independiente_id IS NOT NULL)");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_asunto_ind_referencia_gerencia "
                + "ON asuntos_independientes (gerencia_id, lower(referencia))");
    }

    private void eliminarUnicidadLegacyDeCodigo(String tabla) {
        List<String> restricciones = jdbcTemplate.queryForList("""
                SELECT tc.constraint_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                  ON tc.constraint_catalog = kcu.constraint_catalog
                 AND tc.constraint_schema = kcu.constraint_schema
                 AND tc.constraint_name = kcu.constraint_name
                WHERE tc.constraint_schema = current_schema()
                  AND tc.table_name = ?
                  AND tc.constraint_type = 'UNIQUE'
                GROUP BY tc.constraint_name
                HAVING COUNT(*) = 1 AND MAX(kcu.column_name) = 'codigo'
                """, String.class, tabla);

        for (String restriccion : restricciones) {
            String identificadorSeguro = restriccion.replace("\"", "\"\"");
            jdbcTemplate.execute("ALTER TABLE " + tabla + " DROP CONSTRAINT \"" + identificadorSeguro + "\"");
            log.info("Restricción antigua {} eliminada de {}.", restriccion, tabla);
        }
    }

    private void reemplazarCheckEnum(String tabla, String columna, String nombre, String condicion) {
        List<String> restricciones = jdbcTemplate.queryForList("""
                SELECT c.conname
                FROM pg_constraint c
                JOIN pg_class t ON t.oid = c.conrelid
                JOIN pg_namespace n ON n.oid = t.relnamespace
                WHERE n.nspname = current_schema()
                  AND t.relname = ?
                  AND c.contype = 'c'
                  AND pg_get_constraintdef(c.oid) ILIKE ?
                """, String.class, tabla, "%" + columna + "%");

        for (String restriccion : restricciones) {
            String identificadorSeguro = restriccion.replace("\"", "\"\"");
            jdbcTemplate.execute("ALTER TABLE " + tabla + " DROP CONSTRAINT \"" + identificadorSeguro + "\"");
        }
        jdbcTemplate.execute("ALTER TABLE " + tabla + " ADD CONSTRAINT " + nombre + " CHECK (" + condicion + ")");
    }

    private void reemplazarCheck(String tabla, String nombre, String condicion) {
        jdbcTemplate.execute("ALTER TABLE " + tabla + " DROP CONSTRAINT IF EXISTS " + nombre);
        jdbcTemplate.execute("ALTER TABLE " + tabla + " ADD CONSTRAINT " + nombre
                + " CHECK (" + condicion + ")");
    }
}
