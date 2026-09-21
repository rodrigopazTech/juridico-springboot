package com.juridico.sistema_juridico.config;

import java.sql.Connection;

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
 * Repara secuencias que pueden quedar atrasadas después de cargas o respaldos
 * con identificadores explícitos.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseSequenceSynchronizer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSequenceSynchronizer.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseSequenceSynchronizer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String database = connection.getMetaData().getDatabaseProductName();
            if (!"PostgreSQL".equalsIgnoreCase(database)) {
                return;
            }
        }
        Long sequenceValue = jdbcTemplate.queryForObject("""
                SELECT setval(
                    pg_get_serial_sequence('usuarios', 'id'),
                    COALESCE(MAX(id), 1),
                    MAX(id) IS NOT NULL
                )
                FROM usuarios
                """, Long.class);
        log.info("Secuencia de usuarios sincronizada en {}.", sequenceValue);
    }
}
