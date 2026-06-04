package com.workflowdemo.backend.health;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
class HealthController {

    @GetMapping
    HealthResponse health() {
        return new HealthResponse("ok", Instant.now());
    }

    record HealthResponse(String status, Instant timestamp) {
    }
}
