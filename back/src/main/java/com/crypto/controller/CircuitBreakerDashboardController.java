package com.crypto.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.time.Instant;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/circuit-breaker")
@RequiredArgsConstructor
public class CircuitBreakerDashboardController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @GetMapping("/status")
    public ResponseEntity<?> getCircuitBreakerStatus() {
        try {
            List<Map<String, Object>> circuitBreakers = new ArrayList<>();

            circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
                circuitBreakers.add(buildCircuitBreakerInfo(cb));
            });

            Map<String, Object> response = new HashMap<>();
            response.put("timestamp", Instant.now().toString());
            response.put("totalCircuitBreakers", circuitBreakers.size());
            response.put("circuitBreakers", circuitBreakers);

            long openCount = circuitBreakers.stream()
                    .filter(cb -> {
                        String state = (String) cb.get("state");
                        return "OPEN".equals(state) || "FORCED_OPEN".equals(state);
                    })
                    .count();

            response.put("healthStatus", openCount > 0 ? "DEGRADED" : "HEALTHY");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro ao obter status: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{name}")
    public ResponseEntity<?> getCircuitBreakerByName(@PathVariable String name) {
        try {
            Optional<CircuitBreaker> cbOpt = findByName(name);
            if (cbOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(buildCircuitBreakerInfo(cbOpt.get()));
        } catch (Exception e) {
            log.error("Erro ao obter CB: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{name}/transition/{state}")
    public ResponseEntity<?> transitionCircuitBreaker(
            @PathVariable String name,
            @PathVariable String state) {
        try {
            Optional<CircuitBreaker> cbOpt = findByName(name);
            if (cbOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            CircuitBreaker cb = cbOpt.get();

            switch (state.toUpperCase()) {
                case "CLOSE" -> cb.transitionToClosedState();
                case "OPEN" -> cb.transitionToOpenState();
                case "HALF_OPEN" -> cb.transitionToHalfOpenState();
                case "FORCE_OPEN" -> cb.transitionToForcedOpenState();
                case "DISABLE" -> cb.transitionToDisabledState();
                default -> {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Estado inválido: " + state));
                }
            }

            log.info("CB '{}' -> {}", name, state);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Transicionado para: " + state,
                    "circuitBreaker", buildCircuitBreakerInfo(cb)
            ));

        } catch (Exception e) {
            log.error("Erro ao transicionar: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{name}/reset")
    public ResponseEntity<?> resetCircuitBreaker(@PathVariable String name) {
        try {
            Optional<CircuitBreaker> cbOpt = findByName(name);
            if (cbOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            CircuitBreaker cb = cbOpt.get();
            cb.reset();
            log.info("CB '{}' resetado", name);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Circuit Breaker resetado",
                    "circuitBreaker", buildCircuitBreakerInfo(cb)
            ));

        } catch (Exception e) {
            log.error("Erro ao resetar: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/metrics")
    public ResponseEntity<?> getAllMetrics() {
        try {
            Map<String, Object> allMetrics = new HashMap<>();

            circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
                var metrics = cb.getMetrics();
                Map<String, Object> cbMetrics = new HashMap<>();
                cbMetrics.put("failureRate", String.format("%.2f%%", metrics.getFailureRate()));
                cbMetrics.put("slowCallRate", String.format("%.2f%%", metrics.getSlowCallRate()));
                cbMetrics.put("bufferedCalls", metrics.getNumberOfBufferedCalls());
                cbMetrics.put("failedCalls", metrics.getNumberOfFailedCalls());
                cbMetrics.put("successfulCalls", metrics.getNumberOfSuccessfulCalls());
                cbMetrics.put("slowCalls", metrics.getNumberOfSlowCalls());
                cbMetrics.put("notPermittedCalls", metrics.getNumberOfNotPermittedCalls());
                allMetrics.put(cb.getName(), cbMetrics);
            });

            return ResponseEntity.ok(Map.of(
                    "timestamp", Instant.now().toString(),
                    "metrics", allMetrics
            ));

        } catch (Exception e) {
            log.error("Erro ao obter métricas: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // MÉTODOS AUXILIARES
    // ========================================

    private Optional<CircuitBreaker> findByName(String name) {
        return circuitBreakerRegistry.getAllCircuitBreakers()
                .stream()
                .filter(cb -> cb.getName().equals(name))
                .findFirst();
    }

    private Map<String, Object> buildCircuitBreakerInfo(CircuitBreaker cb) {
        Map<String, Object> info = new HashMap<>();

        try {
            info.put("name", cb.getName());
            info.put("state", cb.getState().name());

            // Métricas
            var metrics = cb.getMetrics();
            Map<String, Object> metricsMap = new HashMap<>();
            metricsMap.put("failureRate", String.format("%.2f%%", metrics.getFailureRate()));
            metricsMap.put("slowCallRate", String.format("%.2f%%", metrics.getSlowCallRate()));
            metricsMap.put("bufferedCalls", metrics.getNumberOfBufferedCalls());
            metricsMap.put("failedCalls", metrics.getNumberOfFailedCalls());
            metricsMap.put("successfulCalls", metrics.getNumberOfSuccessfulCalls());
            metricsMap.put("slowCalls", metrics.getNumberOfSlowCalls());
            metricsMap.put("notPermittedCalls", metrics.getNumberOfNotPermittedCalls());
            info.put("metrics", metricsMap);

            // Configuração (usando var para evitar problemas com Config)
            var cbConfig = cb.getCircuitBreakerConfig();
            Map<String, Object> configMap = new HashMap<>();
            configMap.put("failureRateThreshold",
                    String.format("%.2f%%", cbConfig.getFailureRateThreshold()));
            configMap.put("slowCallRateThreshold",
                    String.format("%.2f%%", cbConfig.getSlowCallRateThreshold()));
            configMap.put("slowCallDurationThreshold",
                    cbConfig.getSlowCallDurationThreshold().toMillis() + "ms");
            configMap.put("permittedNumberOfCallsInHalfOpenState",
                    cbConfig.getPermittedNumberOfCallsInHalfOpenState());
            configMap.put("minimumNumberOfCalls",
                    cbConfig.getMinimumNumberOfCalls());
            configMap.put("slidingWindowSize",
                    cbConfig.getSlidingWindowSize());
            configMap.put("waitDurationInOpenState",
                    cbConfig.getMaxWaitDurationInHalfOpenState().toMillis() + "ms");
            info.put("config", configMap);

            // Status de saúde
            String healthStatus = switch (cb.getState()) {
                case CLOSED, DISABLED -> "HEALTHY";
                case HALF_OPEN -> "RECOVERING";
                case OPEN, FORCED_OPEN -> "UNHEALTHY";
                default -> "UNKNOWN";
            };
            info.put("healthStatus", healthStatus);

        } catch (Exception e) {
            log.error("Erro ao construir info: {}", e.getMessage());
            info.put("error", e.getMessage());
        }

        return info;
    }
}