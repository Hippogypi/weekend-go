package com.weekendgo.health;

import com.weekendgo.common.api.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final String serviceName;

    public HealthController(@Value("${spring.application.name:weekend-go-backend}") String serviceName) {
        this.serviceName = serviceName;
    }

    @GetMapping("/api/health")
    public ApiResponse<HealthStatus> health() {
        return ApiResponse.ok(HealthStatus.up(serviceName));
    }
}
