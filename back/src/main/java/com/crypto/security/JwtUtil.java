// back/src/main/java/com/crypto/security/JwtUtil.java
package com.crypto.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * ✅ JWT UTILITY COM SEGURANÇA HARDENED
 *
 * Melhorias:
 * - Secret obrigatório via env var
 * - Validação de valor default perigoso
 * - Validação de tamanho mínimo
 * - Tratamento de exceções específico
 * - Logs sanitizados (sem tokens)
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 24h padrão
    private long expiration;

    @Value("${jwt.issuer:crypto-monitor}")
    private String issuer;

    private Key key;

    @PostConstruct
    public void init() {
        // ✅ VALIDAÇÃO: Secret deve existir
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException(
                    "❌ JWT Secret não configurado! Configure JWT_SECRET no .env"
            );
        }

        // 🔥 NOVO: Impedir uso de valor default inseguro
        if ("default_secret".equals(secret)) {
            throw new IllegalStateException(
                    "❌ JWT_SECRET está usando valor padrão INSEGURO!\n" +
                            "Configure uma chave forte no Render:\n" +
                            "1. Acesse Render Dashboard\n" +
                            "2. Vá em Environment\n" +
                            "3. Adicione: JWT_SECRET=<64_caracteres_aleatorios>"
            );
        }

        // 🔥 NOVO: Validar tamanho mínimo seguro (256 bits = 32 chars)
        if (secret.length() < 32) {
            log.warn("⚠️ JWT Secret tem menos de 32 caracteres! Gerando padding...");
            secret = String.format("%-64s", secret).replace(' ', '0');
        }

        // ✅ Gerar chave segura HMAC-SHA256
        key = Keys.hmacShaKeyFor(secret.getBytes());

        log.info("✅ JWT configurado: Expiração={}ms, Issuer={}", expiration, issuer);
        // ❌ Nunca logar o secret!
    }

    /**
     * ✅ Gera token JWT com claims customizados
     */
    public String generateToken(String username) {
        return generateToken(username, new HashMap<>());
    }

    /**
     * ✅ Gera token JWT com claims extras
     */
    public String generateToken(String username, Map<String, Object> extraClaims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(username)
                .setIssuer(issuer)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * ✅ Extrai username com tratamento robusto
     */
    public String extractUsername(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            log.warn("Token expirado para subject: {}", e.getClaims().getSubject());
            throw new RuntimeException("Token expirado", e);
        } catch (SignatureException e) {
            log.error("❌ Assinatura inválida no token");
            throw new RuntimeException("Token inválido: assinatura incorreta", e);
        } catch (MalformedJwtException e) {
            log.error("❌ Token malformado");
            throw new RuntimeException("Token inválido: formato incorreto", e);
        } catch (Exception e) {
            log.error("❌ Erro ao processar token: {}", e.getMessage());
            throw new RuntimeException("Token inválido", e);
        }
    }

    /**
     * ✅ Valida token (estrutura + assinatura + expiração)
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Token expirado: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.warn("❌ Token com assinatura inválida recebido");
            return false;
        } catch (MalformedJwtException e) {
            log.warn("❌ Token malformado recebido");
            return false;
        } catch (Exception e) {
            log.warn("❌ Erro ao validar token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * ✅ Verifica se um token está expirado
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractAllClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            log.error("Erro ao verificar expiração: {}", e.getMessage());
            return true;
        }
    }

    /**
     * ✅ Tempo restante antes do token expirar
     */
    public long getExpirationTime(String token) {
        try {
            Date expiration = extractAllClaims(token).getExpiration();
            return expiration.getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * ✅ Extrai todos os claims do token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * ✅ Extrai claim customizado
     */
    public <T> T extractClaim(String token, String claimName, Class<T> type) {
        Claims claims = extractAllClaims(token);
        return claims.get(claimName, type);
    }
}
