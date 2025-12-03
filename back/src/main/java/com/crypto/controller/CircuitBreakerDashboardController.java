package com.crypto.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * ✅ Dashboard para monitoramento de Circuit Breakers
 */
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

            for (CircuitBreaker cb : circuitBreakerRegistry.getAllCircuitBreakers()) {
                Map<String, Object> cbInfo = buildCircuitBreakerInfo(cb);
                circuitBreakers.add(cbInfo);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("timestamp", Instant.now());
            response.put("totalCircuitBreakers", circuitBreakers.size());
            response.put("circuitBreakers", circuitBreakers);

            long openCount = circuitBreakers.stream()
                    .filter(cb -> "OPEN".equals(cb.get("state")) ||
                            "FORCED_OPEN".equals(cb.get("state")))
                    .count();

            response.put("healthStatus", openCount > 0 ? "DEGRADED" : "HEALTHY");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro ao obter status dos Circuit Breakers: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{name}")
    public ResponseEntity<?> getCircuitBreakerByName(@PathVariable String name) {
        try {
            Optional<CircuitBreaker> cbOpt = circuitBreakerRegistry
                    .getAllCircuitBreakers()
                    .stream()
                    .filter(cb -> cb.getName().equals(name))
                    .findFirst();

            if (cbOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> cbInfo = buildCircuitBreakerInfo(cbOpt.get());

            return ResponseEntity.ok(cbInfo);

        } catch (Exception e) {
            log.error("Erro ao obter Circuit Breaker '{}': {}", name, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{name}/transition/{state}")
    public ResponseEntity<?> transitionCircuitBreaker(
            @PathVariable String name,
            @PathVariable String state) {

        try {
            Optional<CircuitBreaker> cbOpt = circuitBreakerRegistry
                    .getAllCircuitBreakers()
                    .stream()
                    .filter(cb -> cb.getName().equals(name))
                    .findFirst();

            if (cbOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            CircuitBreaker cb = cbOpt.get();

            switch (state.toUpperCase()) {
                case "CLOSE" -> {
                    cb.transitionToClosedState();
                    log.info("Circuit Breaker '{}' fechado manualmente", name);
                }
                case "OPEN" -> {
                    cb.transitionToOpenState();
                    log.warn("Circuit Breaker '{}' aberto manualmente", name);
                }
                case "HALF_OPEN" -> {
                    cb.transitionToHalfOpenState();
                    log.info("Circuit Breaker '{}' em half-open manualmente", name);
                }
                case "FORCE_OPEN" -> {
                    cb.transitionToForcedOpenState();
                    log.warn("Circuit Breaker '{}' forçado aberto manualmente", name);
                }
                case "DISABLE" -> {
                    cb.transitionToDisabledState();
                    log.warn("Circuit Breaker '{}' desabilitado manualmente", name);
                }
                default -> {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Estado inválido: " + state));
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Circuit Breaker transicionado para: " + state,
                    "circuitBreaker", buildCircuitBreakerInfo(cb)
            ));

        } catch (Exception e) {
            log.error("Erro ao transicionar Circuit Breaker '{}': {}",
                    name, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{name}/reset")
    public ResponseEntity<?> resetCircuitBreaker(@PathVariable String name) {
        try {
            Optional<CircuitBreaker> cbOpt = circuitBreakerRegistry
                    .getAllCircuitBreakers()
                    .stream()
                    .filter(cb -> cb.getName().equals(name))
                    .findFirst();

            if (cbOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            CircuitBreaker cb = cbOpt.get();
            cb.reset();

            log.info("Circuit Breaker '{}' resetado", name);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Circuit Breaker resetado",
                    "circuitBreaker", buildCircuitBreakerInfo(cb)
            ));

        } catch (Exception e) {
            log.error("Erro ao resetar Circuit Breaker '{}': {}", name, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/metrics")
    public ResponseEntity<?> getAllMetrics() {
        try {
            Map<String, Object> allMetrics = new HashMap<>();

            for (CircuitBreaker cb : circuitBreakerRegistry.getAllCircuitBreakers()) {
                CircuitBreaker.Metrics metrics = cb.getMetrics();

                Map<String, Object> cbMetrics = new HashMap<>();
                cbMetrics.put("failureRate", metrics.getFailureRate());
                cbMetrics.put("slowCallRate", metrics.getSlowCallRate());
                cbMetrics.put("bufferedCalls", metrics.getNumberOfBufferedCalls());
                cbMetrics.put("failedCalls", metrics.getNumberOfFailedCalls());
                cbMetrics.put("successfulCalls", metrics.getNumberOfSuccessfulCalls());
                cbMetrics.put("slowCalls", metrics.getNumberOfSlowCalls());
                cbMetrics.put("notPermittedCalls",
                        metrics.getNumberOfNotPermittedCalls());

                allMetrics.put(cb.getName(), cbMetrics);
            }

            return ResponseEntity.ok(Map.of(
                    "timestamp", Instant.now(),
                    "metrics", allMetrics
            ));

        } catch (Exception e) {
            log.error("Erro ao obter métricas: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> buildCircuitBreakerInfo(CircuitBreaker cb) {
        CircuitBreaker.Metrics metrics = cb.getMetrics();

        Map<String, Object> info = new HashMap<>();
        info.put("name", cb.getName());
        info.put("state", cb.getState().name());

        Map<String, Object> metricsMap = new HashMap<>();
        metricsMap.put("failureRate", String.format("%.2f%%", metrics.getFailureRate()));
        metricsMap.put("slowCallRate", String.format("%.2f%%", metrics.getSlowCallRate()));
        metricsMap.put("bufferedCalls", metrics.getNumberOfBufferedCalls());
        metricsMap.put("failedCalls", metrics.getNumberOfFailedCalls());
        metricsMap.put("successfulCalls", metrics.getNumberOfSuccessfulCalls());
        metricsMap.put("slowCalls", metrics.getNumberOfSlowCalls());
        metricsMap.put("notPermittedCalls", metrics.getNumberOfNotPermittedCalls());

        info.put("metrics", metricsMap);

        CircuitBreaker.Config config = cb.getCircuitBreakerConfig();
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("failureRateThreshold", config.getFailureRateThreshold());
        configMap.put("slowCallRateThreshold", config.getSlowCallRateThreshold());
        configMap.put("slowCallDurationThreshold",
                config.getSlowCallDurationThreshold().toMillis() + "ms");
        configMap.put("permittedNumberOfCallsInHalfOpenState",
                config.getPermittedNumberOfCallsInHalfOpenState());
        configMap.put("minimumNumberOfCalls", config.getMinimumNumberOfCalls());
        configMap.put("slidingWindowSize", config.getSlidingWindowSize());
        configMap.put("waitDurationInOpenState",
                config.getWaitDurationInOpenState().toMillis() + "ms");

        info.put("config", configMap);

        String healthStatus = switch (cb.getState()) {
            case CLOSED, DISABLED -> "HEALTHY";
            case HALF_OPEN -> "RECOVERING";
            case OPEN, FORCED_OPEN -> "UNHEALTHY";
        };

        info.put("healthStatus", healthStatus);

        return info;
    }
}