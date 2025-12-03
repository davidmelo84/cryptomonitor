package com.crypto.exception;

public class TradingBotException extends RuntimeException {

    public enum ErrorType {
        BOT_NOT_FOUND,
        BOT_ALREADY_RUNNING,
        BOT_CONFIGURATION_INVALID,
        TRADING_EXECUTION_FAILED,
        UNAUTHORIZED_ACCESS
    }

    private final ErrorType errorType;

    public TradingBotException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public TradingBotException(String message, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}