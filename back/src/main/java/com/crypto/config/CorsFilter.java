// back/src/main/java/com/crypto/config/CorsFilter.java
package com.crypto.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * ✅ FILTRO CORS ADICIONAL
 *
 * Garante que CORS funcione ANTES de qualquer outro filtro
 * (incluindo Security e RateLimit)
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;

        String origin = request.getHeader("Origin");

        // ✅ Permitir Vercel e localhost
        if (origin != null && (
                origin.contains("vercel.app") ||
                        origin.contains("localhost") ||
                        origin.contains("127.0.0.1")
        )) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        }

        // ✅ Headers CORS
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH, HEAD");
        response.setHeader("Access-Control-Allow-Headers", "*");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "7200");
        response.setHeader("Access-Control-Expose-Headers", "Authorization, Content-Type, X-Total-Count");

        // ✅ CRÍTICO: Responder OPTIONS imediatamente
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.debug("✅ OPTIONS preflight: {} {}", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_OK);
            return; // NÃO continuar o filter chain
        }

        chain.doFilter(req, res);
    }

    @Override
    public void init(FilterConfig filterConfig) {
        log.info("✅ CORS Filter inicializado (HIGHEST_PRECEDENCE)");
    }

    @Override
    public void destroy() {
        log.info("🔌 CORS Filter destruído");
    }
}