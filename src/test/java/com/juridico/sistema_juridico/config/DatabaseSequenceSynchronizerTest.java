package com.juridico.sistema_juridico.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class DatabaseSequenceSynchronizerTest {

    @Test
    void sincronizaLaSecuenciaDeUsuariosEnPostgresql() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(21L);

        new DatabaseSequenceSynchronizer(dataSource, jdbcTemplate).run(null);

        verify(jdbcTemplate).queryForObject(anyString(), eq(Long.class));
        verify(connection).close();
    }

    @Test
    void omiteLaSincronizacionEnBasesQueNoSonPostgresql() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("H2");

        new DatabaseSequenceSynchronizer(dataSource, jdbcTemplate).run(null);

        verifyNoInteractions(jdbcTemplate);
        verify(connection).close();
    }
}
