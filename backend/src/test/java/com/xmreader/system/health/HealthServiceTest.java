package com.xmreader.system.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class HealthServiceTest {

    @Test
    void checkReportsHealthyDatabaseAndApplication() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        HealthService healthService = new HealthService(jdbcTemplate, "test-version");

        HealthResponse response = healthService.check();

        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.components()).containsEntry("application", "UP");
        assertThat(response.components()).containsEntry("database", "UP");
        assertThat(response.version()).isEqualTo("test-version");
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void checkRejectsUnexpectedDatabaseResult() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(0);
        HealthService healthService = new HealthService(jdbcTemplate, "test-version");

        assertThatThrownBy(healthService::check)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Database health check returned an unexpected result");
    }

    @Test
    void checkRejectsNullDatabaseResult() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(null);
        HealthService healthService = new HealthService(jdbcTemplate, "test-version");

        assertThatThrownBy(healthService::check)
                .isInstanceOf(IllegalStateException.class);
    }
}
