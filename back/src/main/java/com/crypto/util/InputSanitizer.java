package com.crypto.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;


@Slf4j
@Component
public class InputSanitizer {

    private static final Pattern SQL_INJECTION = Pattern.compile(
            "('(''|[^'])*')|(;)|(\\b(ALTER|CREATE|DELETE|DROP|EXEC(UTE)?|INSERT( +INTO)?|MERGE|SELECT|UPDATE|UNION( +ALL)?)\\b)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern XSS_PATTERN = Pattern.compile(
            "<script[^>]*>.*?</script>|javascript:|onerror=|onload=|<iframe|<object|<embed",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern PATH_TRAVERSAL = Pattern.compile(
            "\\.\\./|\\.\\\\|%2e%2e|%5c|%2f",
            Pattern.CASE_INSENSITIVE
    );


    public String sanitize(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        String sanitized = input.trim();

        sanitized = sanitized.replace("\0", "");

        sanitized = sanitized.replaceAll("\\p{Cntrl}", "");

        return sanitized;
    }


    public boolean containsSqlInjection(String input) {
        if (input == null) return false;

        boolean detected = SQL_INJECTION.matcher(input).find();

        if (detected) {
            log.warn("⚠️ SQL Injection detectado: {}", maskSensitive(input));
        }

        return detected;
    }


    public boolean containsXss(String input) {
        if (input == null) return false;

        boolean detected = XSS_PATTERN.matcher(input).find();

        if (detected) {
            log.warn("⚠️ XSS detectado: {}", maskSensitive(input));
        }

        return detected;
    }


    public boolean containsPathTraversal(String input) {
        if (input == null) return false;

        boolean detected = PATH_TRAVERSAL.matcher(input).find();

        if (detected) {
            log.warn("⚠️ Path Traversal detectado: {}", maskSensitive(input));
        }

        return detected;
    }


    public boolean isSafe(String input) {
        return !containsSqlInjection(input)
                && !containsXss(input)
                && !containsPathTraversal(input);
    }



    public String validateAndSanitize(String input, String fieldName) {
        if (input == null) {
            return null;
        }

        if (!isSafe(input)) {
            throw new IllegalArgumentException(
                    "Campo '" + fieldName + "' contém caracteres inválidos"
            );
        }

        return sanitize(input);
    }


    public String sanitizeEmail(String email) {
        if (email == null) return null;

        email = sanitize(email).toLowerCase();

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Email inválido");
        }

        return email;
    }


    public String sanitizeUsername(String username) {
        if (username == null) return null;

        username = sanitize(username);

        if (!username.matches("^[a-zA-Z0-9_-]{3,30}$")) {
            throw new IllegalArgumentException(
                    "Username inválido. Use apenas letras, números, _ ou - (3-30 caracteres)"
            );
        }

        return username;
    }


    public String sanitizeCoinSymbol(String symbol) {
        if (symbol == null) return null;

        symbol = sanitize(symbol).toUpperCase();

        if (!symbol.matches("^[A-Z0-9]{2,10}$")) {
            throw new IllegalArgumentException("Coin symbol inválido");
        }

        return symbol;
    }

    public String sanitizeCoinId(String coinId) {
        if (coinId == null) return null;

        coinId = sanitize(coinId).toLowerCase();

        if (!coinId.matches("^[a-z0-9-]{2,50}$")) {
            throw new IllegalArgumentException("Coin ID inválido");
        }

        return coinId;
    }


    private String maskSensitive(String input) {
        if (input == null || input.length() <= 10) {
            return "***";
        }

        return input.substring(0, 5) + "..." + input.substring(input.length() - 5);
    }
}