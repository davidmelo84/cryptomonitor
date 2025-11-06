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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;

    @Autowired(required = false)
    @Qualifier("rateLimitHitsCounter")
    private Counter rateLimitHitsCounter;

    @Autowired(required = false)
    @Qualifier("apiRequestsCounter")
    private Counter apiRequestsCounter;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip = getClientIp(request);

        BucketType bucketType = determineBucketType(path);
        String bucketKey = ip + ":" + bucketType;
        Bucket bucket = rateLimitConfig.resolveBucket(bucketKey, bucketType);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            if (apiRequestsCounter != null) apiRequestsCounter.increment();

            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            log.debug("✅ Rate limit OK: {} {} (IP: {}, Remaining: {})",
                    request.getMethod(), path, ip, probe.getRemainingTokens());

            filterChain.doFilter(request, response);
        } else {
            if (rateLimitHitsCounter != null) rateLimitHitsCounter.increment();

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

    private BucketType determineBucketType(String path) {
        if (path.startsWith("/crypto-monitor/api/auth/")) return BucketType.AUTH;
        if (path.startsWith("/crypto-monitor/api/admin/")) return BucketType.ADMIN;
        return BucketType.API;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) return xForwardedFor.split(",")[0].trim();

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) return xRealIp;

        return request.getRemoteAddr();
    }

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
