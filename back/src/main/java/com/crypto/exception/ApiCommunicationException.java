package com.crypto.exception;

public class ApiCommunicationException extends RuntimeException {
    private final int statusCode;

    public ApiCommunicationException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public ApiCommunicationException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
