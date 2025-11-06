// back/src/main/java/com/crypto/security/RateLimitFilter.java
package com.crypto.security;

import com.crypto.config.RateLimitConfig;
import com.crypto.config.RateLimitConfig.BucketType;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.micrometer.core.instrument.Counter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * ✅ SPRINT 1 - RATE LIMITING FILTER
 *
 * Filtro que aplica rate limiting antes de qualquer processamento.
 *
 * Retorna:
 * - 429 Too Many Requests se limite excedido
 * - Headers:
 *   - X-Rate-Limit-Remaining: Tokens restantes
 *   - X-Rate-Limit-Retry-After: Segundos para retry
 */
@Slf4j
@Component
@Order(1) // Executar ANTES do JwtAuthenticationFilter
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;

    // ✅ SPRINT 1: Métricas
    @Autowired(required = false)
    private Counter rateLimitHitsCounter;

    @Autowired(required = false)
    private Counter apiRequestsCounter;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip = getClientIp(request);

        // ✅ Determinar tipo de bucket baseado no endpoint
        BucketType bucketType = determineBucketType(path);

        // ✅ Resolver bucket para este IP + tipo
        String bucketKey = ip + ":" + bucketType;
        Bucket bucket = rateLimitConfig.resolveBucket(bucketKey, bucketType);

        // ✅ Tentar consumir 1 token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // ✅ Requisição permitida
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));

            log.debug("✅ Rate limit OK: {} {} (IP: {}, Remaining: {})",
                    request.getMethod(), path, ip, probe.getRemainingTokens());

            filterChain.doFilter(request, response);
        } else {
            // ❌ Limite excedido
            long waitForRefill = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.addHeader("X-Rate-Limit-Retry-After", String.valueOf(waitForRefill));
            response.addHeader("X-Rate-Limit-Remaining", "0");

            String errorMessage = String.format(
                    "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Try again in %d seconds.\",\"type\":\"%s\"}",
                    waitForRefill, bucketType
            );

            response.getWriter().write(errorMessage);

            log.warn("⚠️ RATE LIMIT EXCEEDED: {} {} (IP: {}, Type: {}, Retry in: {}s)",
                    request.getMethod(), path, ip, bucketType, waitForRefill);
        }
    }

    /**
     * ✅ Determinar tipo de bucket baseado no path
     */
    private BucketType determineBucketType(String path) {
        if (path.startsWith("/crypto-monitor/api/auth/")) {
            return BucketType.AUTH;
        }

        if (path.startsWith("/crypto-monitor/api/admin/")) {
            return BucketType.ADMIN;
        }

        return BucketType.API;
    }

    /**
     * ✅ Extrair IP real do cliente (considerando proxies)
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Pegar o primeiro IP (cliente real)
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * ✅ Não aplicar rate limit em:
     * - Health checks
     * - Actuator endpoints (métricas)
     * - Arquivos estáticos
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.startsWith("/crypto-monitor/actuator/health") ||
                path.startsWith("/crypto-monitor/actuator/prometheus") ||
                path.endsWith(".css") ||
                path.endsWith(".js") ||
                path.endsWith(".png") ||
                path.endsWith(".jpg") ||
                path.endsWith(".ico");
    }
}