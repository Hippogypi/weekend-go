package com.weekendgo.health;

import java.time.Instant;

public record HealthStatus(
        String status,
        String service,
        Instant timestamp
) {

    public static HealthStatus up(String service) {
        return new HealthStatus("UP", service, Instant.now());
    }
}
