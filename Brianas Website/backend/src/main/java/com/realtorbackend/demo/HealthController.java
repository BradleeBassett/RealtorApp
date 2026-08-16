package com.realtorbackend.demo;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/health/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("backend", "connected");

        try (Connection connection = dataSource.getConnection()) {
            payload.put("database", "connected");
            return ResponseEntity.ok(payload);
        } catch (SQLException e) {
            payload.put("database", "disconnected");
            payload.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(payload);
        }
    }
}
