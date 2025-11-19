package com.crypto.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * ✅ MDC (Mapped Diagnostic Context) Filter
 *
 * Adiciona informações de contexto a TODOS os logs da requisição:
 * - requestId: UUID único por request
 * - username: usuário autenticado (se houver)
 * - clientIp: IP do cliente
 * - method: HTTP method
 * - uri: URI da requisição
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            // 1. Gerar Request ID único
            String requestId = UUID.randomUUID().toString().substring(0, 8);
            MDC.put("requestId", requestId);

            // 2. Extrair IP do cliente
            String clientIp = extractClientIp(httpRequest);
            MDC.put("clientIp", clientIp);

            // 3. Adicionar método HTTP e URI
            MDC.put("method", httpRequest.getMethod());
            MDC.put("uri", httpRequest.getRequestURI());

            // 4. Tentar extrair username (se autenticado)
            String username = extractUsername(httpRequest);
            if (username != null) {
                MDC.put("username", username);
            }

            log.debug("🔍 Request iniciado: {} {} (RequestID: {})",
                    httpRequest.getMethod(),
                    httpRequest.getRequestURI(),
                    requestId);

            chain.doFilter(request, response);

        } finally {
            // ✅ CRÍTICO: Limpar MDC após a requisição
            MDC.clear();
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip != null && !ip.isEmpty()) {
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty()) {
            return ip;
        }

        return request.getRemoteAddr();
    }

    private String extractUsername(HttpServletRequest request) {
        try {
            var authentication = org.springframework.security.core.context.SecurityContextHolder
                    .getContext()
                    .getAuthentication();

            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();

                if (principal instanceof org.springframework.security.core.userdetails.User) {
                    return ((org.springframework.security.core.userdetails.User) principal).getUsername();
                }

                return principal.toString();
            }
        } catch (Exception e) {
            log.trace("Não foi possível extrair username: {}", e.getMessage());
        }

        return null;
    }

    @Override
    public void init(FilterConfig filterConfig) {
        log.info("✅ MdcFilter inicializado");
    }

    @Override
    public void destroy() {
        log.info("🔌 MdcFilter destruído");
    }
}