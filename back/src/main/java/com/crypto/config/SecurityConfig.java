// back/src/main/java/com/crypto/config/SecurityConfig.java
package com.crypto.config;

import com.crypto.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * ✅ CORREÇÃO CRÍTICA: CORS COMPLETO
 *
 * MUDANÇAS:
 * 1. OPTIONS permitido GLOBALMENTE
 * 2. Origens Vercel corrigidas
 * 3. Headers expostos corretamente
 * 4. Max-age aumentado para 2 horas
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ✅ CRÍTICO: OPTIONS sempre permitido (preflight CORS)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ✅ Endpoints públicos
                        .requestMatchers(
                                "/api/auth/**",              // Login/Registro
                                "/api/user/**",              // Verificação de email
                                "/api/crypto/status",        // Status
                                "/api/crypto/history/**",    // Histórico
                                "/actuator/health",          // Health check
                                "/actuator/info",
                                "/actuator/prometheus",
                                "/ws/**",                    // WebSocket
                                "/topic/**",
                                "/app/**",
                                "/sockjs-node/**"
                        ).permitAll()

                        // 🔒 Todos os outros requerem autenticação
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ ORIGENS PERMITIDAS (Vercel + localhost)
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "https://cryptomonitor-theta.vercel.app",
                "https://www.cryptomonitor-theta.vercel.app",
                "https://*.vercel.app",
                "http://localhost:3000",
                "http://localhost:5173",
                "http://127.0.0.1:*"
        ));

        // ✅ MÉTODOS HTTP
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));

        // ✅ HEADERS PERMITIDOS (todos)
        configuration.setAllowedHeaders(List.of("*"));

        // ✅ CREDENCIAIS
        configuration.setAllowCredentials(true);

        // ✅ HEADERS EXPOSTOS (incluindo Authorization)
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Total-Count",
                "X-Rate-Limit-Remaining",
                "X-Rate-Limit-Retry-After"
        ));

        // ✅ MAX-AGE (2 horas = reduz preflight requests)
        configuration.setMaxAge(7200L);

        // ✅ APLICAR GLOBALMENTE
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}