package com.crypto.exception;

public class ConfigurationException extends RuntimeException {
    private final String configKey;

    public ConfigurationException(String message, String configKey) {
        super(message);
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }
}