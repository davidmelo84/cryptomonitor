package com.crypto.util;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;


@Slf4j
public class SensitiveDataMasker {

    // Patterns para detecção
    private static final Pattern JWT_PATTERN = Pattern.compile("eyJ[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
    private static final Pattern API_KEY_PATTERN = Pattern.compile("(sk|pk)_[a-zA-Z0-9]{20,}");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(password|pwd|senha)\\s*[:=]\\s*[\\S]+", Pattern.CASE_INSENSITIVE);


    public static String maskJwt(String jwt) {
        if (jwt == null || jwt.length() < 10) {
            return "***";
        }

        return jwt.substring(0, Math.min(4, jwt.length())) + "...***";
    }


    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }

        String[] parts = email.split("@");
        if (parts.length != 2) {
            return "***";
        }

        String localPart = parts[0];
        String domain = parts[1];

        if (localPart.length() <= 1) {
            return localPart + "***@" + domain;
        }

        return localPart.charAt(0) + "***@" + domain;
    }


    public static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 5) {
            return "***";
        }


        if (apiKey.startsWith("sk_") || apiKey.startsWith("pk_")) {
            return apiKey.substring(0, 3) + "***";
        }

        if (apiKey.contains(".")) {
            String prefix = apiKey.substring(0, apiKey.indexOf("."));
            return prefix + ".***";
        }

        return apiKey.substring(0, Math.min(5, apiKey.length())) + "***";
    }


    public static String maskPassword(String password) {
        return "***";
    }


    public static String maskSensitiveData(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String masked = text;

        masked = JWT_PATTERN.matcher(masked).replaceAll(match -> {
            String token = match.group();
            return maskJwt(token);
        });

        masked = EMAIL_PATTERN.matcher(masked).replaceAll(match -> {
            String email = match.group();
            return maskEmail(email);
        });

        masked = API_KEY_PATTERN.matcher(masked).replaceAll(match -> {
            String apiKey = match.group();
            return maskApiKey(apiKey);
        });

        masked = PASSWORD_PATTERN.matcher(masked).replaceAll(match -> {
            String[] parts = match.group().split("[:=]", 2);
            return parts[0] + "=***";
        });

        return masked;
    }


    public static boolean containsSensitiveData(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        return JWT_PATTERN.matcher(text).find() ||
                EMAIL_PATTERN.matcher(text).find() ||
                API_KEY_PATTERN.matcher(text).find() ||
                PASSWORD_PATTERN.matcher(text).find();
    }


    public static String maskIfSensitive(String text) {
        if (containsSensitiveData(text)) {
            return maskSensitiveData(text);
        }
        return text;
    }

    public static void main(String[] args) {
        System.out.println("JWT: " + maskJwt("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.abc"));
        System.out.println("Email: " + maskEmail("user@example.com"));
        System.out.println("API Key: " + maskApiKey("sk_live_abc123def456"));
        System.out.println("Password: " + maskPassword("mypassword123"));

        String fullText = "Login com user@email.com, senha=mypass123, token=eyJhbc.def.ghi";
        System.out.println("\nTexto completo:\n" + maskSensitiveData(fullText));
    }
}