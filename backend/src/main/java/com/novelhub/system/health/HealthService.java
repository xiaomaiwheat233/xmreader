package com.novelhub.system.health;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final String version;

    public HealthService(
            JdbcTemplate jdbcTemplate,
            @Value("${info.app.version:dev}") String version) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = Clock.systemUTC();
        this.version = version;
    }

    public HealthResponse check() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        if (result == null || result != 1) {
            throw new IllegalStateException("Database health check returned an unexpected result");
        }
        return new HealthResponse(
                "UP",
                Map.of("application", "UP", "database", "UP"),
                version,
                Instant.now(clock));
    }
}
