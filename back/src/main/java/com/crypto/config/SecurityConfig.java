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
                        // Permitir OPTIONS para preflight CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Endpoints públicos
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/crypto/status",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()
                        // Todos os outros precisam autenticação
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Permitir todas as origens do Vercel + localhost
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "https://cryptomonitor-theta.vercel.app",
                "https://www.cryptomonitor-theta.vercel.app",
                "https://*.vercel.app",  // Todos os previews do Vercel
                "http://localhost:*",     // Qualquer porta local
                "http://127.0.0.1:*"
        ));

        // Métodos HTTP permitidos
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // Headers permitidos
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Permitir credenciais (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Expor headers de resposta
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Total-Count"
        ));

        // Cache do preflight (1 hora)
        configuration.setMaxAge(3600L);

        // Aplicar para todos os endpoints
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