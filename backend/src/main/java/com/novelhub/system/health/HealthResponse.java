package com.novelhub.system.health;

import java.time.Instant;
import java.util.Map;

public record HealthResponse(
        String status,
        Map<String, String> components,
        String version,
        Instant timestamp) {
}
