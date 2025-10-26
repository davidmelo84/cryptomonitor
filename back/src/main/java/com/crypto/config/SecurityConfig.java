// back/src/main/java/com/crypto/config/SecurityConfig.java
package com.crypto.config;

import com.crypto.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * ✅ CONFIGURAÇÃO DE SEGURANÇA CORRIGIDA
 *
 * Melhorias implementadas:
 * - CORS configurável e restrito
 * - Headers de segurança (XSS, Clickjacking, CSP)
 * - BCrypt com força 12
 * - Session stateless (JWT)
 */
@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String[] allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ✅ CORS configurado
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ✅ CSRF desabilitado para API REST (stateless)
                .csrf(csrf -> csrf.disable())

                // ✅ Security Headers - CORRIGIDO
                .headers(headers -> headers
                        // Previne Clickjacking
                        .frameOptions(frame -> frame.deny())

                        // XSS Protection - ✅ MÉTODO CORRETO
                        .xssProtection(xss -> xss
                                .headerValue(org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                        )

                        // Content Security Policy
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                        "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                                        "style-src 'self' 'unsafe-inline'; " +
                                        "img-src 'self' data: https:; " +
                                        "font-src 'self' data:;"
                        ))

                        // Previne MIME sniffing
                        .contentTypeOptions(contentType -> {})
                )

                // ✅ Autorização de rotas
                .authorizeHttpRequests(auth -> auth
                        // Rotas públicas (SEM autenticação)
                        .requestMatchers(
                                "/api/auth/**",           // Login/Register
                                "/api/user",              // Criar usuário
                                "/api/crypto/current/**", // Cotações públicas
                                "/actuator/health",       // Health check
                                "/h2-console/**"          // H2 Console (dev)
                        ).permitAll()

                        // Rotas administrativas (APENAS ADMIN)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Todas as outras rotas exigem autenticação
                        .anyRequest().authenticated()
                )

                // ✅ Session stateless (JWT)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ✅ JWT Filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * ✅ CORS CONFIGURÁVEL (não mais "*")
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Origins configuráveis via application.yml
        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins));

        // Métodos permitidos
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // Headers permitidos
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "X-Requested-With",
                "Accept", "Origin", "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // Headers expostos ao frontend
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization", "Content-Disposition"
        ));

        // Permitir credenciais (cookies, auth headers)
        configuration.setAllowCredentials(true);

        // Cache de preflight (OPTIONS) por 1 hora
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt com força 12 (mais seguro que padrão 10)
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}