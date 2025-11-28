package com.crypto.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;


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


        if (origin != null && isOriginAllowed(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            log.debug("✅ CORS permitido para: {}", origin);
        } else if (origin != null) {
            log.warn("⚠️ CORS BLOQUEADO para origem suspeita: {}", origin);
        }

        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH, HEAD");
        response.setHeader("Access-Control-Allow-Headers", "*");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "7200");
        response.setHeader("Access-Control-Expose-Headers", "Authorization, Content-Type, X-Total-Count");


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


    private boolean isOriginAllowed(String origin) {
        if (origin == null || origin.isEmpty()) {
            return false;
        }


        return origin.equals("https://cryptomonitor-theta.vercel.app") ||
                origin.endsWith(".vercel.app") ||
                origin.startsWith("http://localhost:") ||
                origin.startsWith("http://127.0.0.1:");
    }
}
