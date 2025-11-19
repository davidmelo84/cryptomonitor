package com.crypto.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * ✅ TESTES UNITÁRIOS - JwtUtil
 *
 * Testa geração e validação de tokens JWT
 */
@DisplayName("JwtUtil - Testes de Autenticação")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String testSecret = "test-secret-key-minimum-32-characters-required";
    private final long testExpiration = 3600000L; // 1 hora

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();

        // Injetar valores de teste usando reflection
        ReflectionTestUtils.setField(jwtUtil, "secret", testSecret);
        ReflectionTestUtils.setField(jwtUtil, "expiration", testExpiration);
        ReflectionTestUtils.setField(jwtUtil, "issuer", "crypto-monitor-test");

        // Inicializar JwtUtil
        jwtUtil.init();
    }

    @Test
    @DisplayName("Deve gerar token válido")
    void shouldGenerateValidToken() {
        String username = "testuser";

        String token = jwtUtil.generateToken(username);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("Deve extrair username corretamente")
    void shouldExtractUsername() {
        String username = "testuser";
        String token = jwtUtil.generateToken(username);

        String extracted = jwtUtil.extractUsername(token);

        assertThat(extracted).isEqualTo(username);
    }

    @Test
    @DisplayName("Deve validar token válido")
    void shouldValidateValidToken() {
        String token = jwtUtil.generateToken("testuser");

        boolean isValid = jwtUtil.validateToken(token);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar token malformado")
    void shouldRejectMalformedToken() {
        String malformedToken = "invalid.token.here";

        boolean isValid = jwtUtil.validateToken(malformedToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar token com assinatura inválida")
    void shouldRejectInvalidSignature() {
        String token = jwtUtil.generateToken("testuser");

        // Modificar última parte (signature)
        String[] parts = token.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + ".invalid";

        boolean isValid = jwtUtil.validateToken(tamperedToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Deve gerar token com claims customizados")
    void shouldGenerateTokenWithCustomClaims() {
        String username = "testuser";
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "ADMIN");
        claims.put("email", "test@example.com");

        String token = jwtUtil.generateToken(username, claims);

        assertThat(token).isNotNull();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo(username);

        String role = jwtUtil.extractClaim(token, "role", String.class);
        assertThat(role).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Deve verificar expiração")
    void shouldCheckExpiration() {
        String token = jwtUtil.generateToken("testuser");

        boolean isExpired = jwtUtil.isTokenExpired(token);

        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("Deve retornar tempo de expiração positivo")
    void shouldReturnPositiveExpirationTime() {
        String token = jwtUtil.generateToken("testuser");

        long expirationTime = jwtUtil.getExpirationTime(token);

        assertThat(expirationTime).isPositive();
        assertThat(expirationTime).isLessThanOrEqualTo(testExpiration);
    }

    @Test
    @DisplayName("Deve lançar exceção para token expirado ao extrair username")
    void shouldThrowExceptionForExpiredToken() {
        // Criar JwtUtil com expiração curta
        JwtUtil shortExpirationJwt = new JwtUtil();
        ReflectionTestUtils.setField(shortExpirationJwt, "secret", testSecret);
        ReflectionTestUtils.setField(shortExpirationJwt, "expiration", 1L); // 1ms
        ReflectionTestUtils.setField(shortExpirationJwt, "issuer", "test");
        shortExpirationJwt.init();

        String token = shortExpirationJwt.generateToken("testuser");

        // Aguardar expiração
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThatThrownBy(() -> shortExpirationJwt.extractUsername(token))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Token expirado");
    }

    @Test
    @DisplayName("Deve validar issuer do token")
    void shouldValidateIssuer() {
        String token = jwtUtil.generateToken("testuser");

        // Token válido deve passar na validação
        assertThat(jwtUtil.validateToken(token)).isTrue();

        // Token de outro issuer deve falhar
        JwtUtil differentIssuer = new JwtUtil();
        ReflectionTestUtils.setField(differentIssuer, "secret", testSecret);
        ReflectionTestUtils.setField(differentIssuer, "expiration", testExpiration);
        ReflectionTestUtils.setField(differentIssuer, "issuer", "different-issuer");
        differentIssuer.init();

        String tokenDifferentIssuer = differentIssuer.generateToken("testuser");

        // Validar com JwtUtil original (issuer diferente)
        assertThat(jwtUtil.validateToken(tokenDifferentIssuer)).isFalse();
    }
}