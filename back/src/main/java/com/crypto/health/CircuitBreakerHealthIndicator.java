package com.crypto.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * ✅ Health Indicator para monitorar Circuit Breakers
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CircuitBreakerHealthIndicator implements HealthIndicator {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();
            boolean allHealthy = true;

            for (CircuitBreaker cb : circuitBreakerRegistry.getAllCircuitBreakers()) {
                String name = cb.getName();
                CircuitBreaker.State state = cb.getState();
                CircuitBreaker.Metrics metrics = cb.getMetrics();

                Map<String, Object> cbDetails = new HashMap<>();
                cbDetails.put("state", state.name());
                cbDetails.put("failureRate", String.format("%.2f%%",
                        metrics.getFailureRate()));
                cbDetails.put("slowCallRate", String.format("%.2f%%",
                        metrics.getSlowCallRate()));
                cbDetails.put("numberOfBufferedCalls",
                        metrics.getNumberOfBufferedCalls());
                cbDetails.put("numberOfFailedCalls",
                        metrics.getNumberOfFailedCalls());
                cbDetails.put("numberOfSuccessfulCalls",
                        metrics.getNumberOfSuccessfulCalls());
                cbDetails.put("numberOfSlowCalls",
                        metrics.getNumberOfSlowCalls());

                details.put(name, cbDetails);

                if (state == CircuitBreaker.State.OPEN ||
                        state == CircuitBreaker.State.FORCED_OPEN) {
                    allHealthy = false;
                    log.warn("Circuit Breaker '{}' está ABERTO - failureRate: {}%",
                            name, metrics.getFailureRate());
                }
            }

            details.put("timestamp", Instant.now());

            if (allHealthy) {
                return Health.up().withDetails(details).build();
            } else {
                return Health.status("DEGRADED").withDetails(details).build();
            }

        } catch (Exception e) {
            log.error("Erro ao verificar Circuit Breakers: {}", e.getMessage());
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}