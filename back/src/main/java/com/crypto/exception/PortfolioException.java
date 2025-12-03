package com.crypto.exception;

public class PortfolioException extends RuntimeException {

    public enum ErrorType {
        INSUFFICIENT_BALANCE,
        INVALID_QUANTITY,
        CRYPTO_NOT_FOUND,
        TRANSACTION_FAILED
    }

    private final ErrorType errorType;

    public PortfolioException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public PortfolioException(String message, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}