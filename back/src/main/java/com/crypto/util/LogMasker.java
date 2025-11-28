package com.crypto.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class LogMasker {


    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }

        String[] parts = email.split("@");
        if (parts.length != 2 || parts[0].length() < 2) {
            return "***@" + parts[1];
        }

        return parts[0].charAt(0) + "***@" + parts[1];
    }


    public static String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 4) + "...***";
    }


    public static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 15) {
            return "***";
        }

        String prefix = apiKey.substring(0, 5);
        String suffix = apiKey.substring(apiKey.length() - 4);

        return prefix + "..." + suffix;
    }


    public static String maskPassword(String password) {
        return "***";
    }

    public static String maskUsername(String username) {
        if (username == null || username.length() < 3) {
            return "***";
        }

        return username.charAt(0) + "***" +
                username.charAt(username.length() - 1);
    }


    public static String autoMask(String text) {
        if (text == null) return null;

        if (text.contains("@") && text.matches(".*[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}.*")) {
            return maskEmail(text);
        }

        if (text.startsWith("eyJ")) {
            return maskToken(text);
        }

        if (text.startsWith("SG.")) {
            return maskApiKey(text);
        }

        return text;
    }

    public static String maskId(String id) {
        if (id == null || id.length() < 5) {
            return "***";
        }

        return id.substring(0, 2) + "..." + id.substring(id.length() - 2);
    }


    public static String maskId(Long id) {
        if (id == null) {
            return "***";
        }
        return maskId(String.valueOf(id));
    }
}
