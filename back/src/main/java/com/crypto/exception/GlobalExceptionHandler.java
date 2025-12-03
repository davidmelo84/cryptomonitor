package com.crypto.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String msg = error.getDefaultMessage();
            errors.put(field, msg);
        });

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                "Dados de entrada inválidos",
                errors,
                request
        );
    }

    // ✅ NOVO: Email Service Exception
    @ExceptionHandler(EmailServiceException.class)
    public ResponseEntity<Map<String, Object>> handleEmailServiceException(
            EmailServiceException ex, WebRequest request) {

        log.error("Erro no serviço de email: {}", ex.getMessage(), ex);

        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Email Service Error",
                "Falha ao enviar email: " + ex.getMessage(),
                null,
                request
        );
    }

    // ✅ NOVO: API Communication Exception
    @ExceptionHandler(ApiCommunicationException.class)
    public ResponseEntity<Map<String, Object>> handleApiCommunicationException(
            ApiCommunicationException ex, WebRequest request) {

        log.error("Erro de comunicação com API externa - Status: {}",
                ex.getStatusCode(), ex);

        HttpStatus status = ex.getStatusCode() == 429
                ? HttpStatus.TOO_MANY_REQUESTS
                : HttpStatus.BAD_GATEWAY;

        return buildResponse(
                status,
                "API Communication Error",
                ex.getMessage(),
                Map.of("statusCode", ex.getStatusCode()),
                request
        );
    }

    // ✅ NOVO: Cache Exception
    @ExceptionHandler(CacheException.class)
    public ResponseEntity<Map<String, Object>> handleCacheException(
            CacheException ex, WebRequest request) {

        log.error("Erro no sistema de cache: {}", ex.getMessage(), ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Cache Error",
                "Erro temporário no cache - os dados podem estar desatualizados",
                null,
                request
        );
    }

    // ✅ NOVO: Portfolio Exception
    @ExceptionHandler(PortfolioException.class)
    public ResponseEntity<Map<String, Object>> handlePortfolioException(
            PortfolioException ex, WebRequest request) {

        log.warn("Erro no portfolio - Tipo: {}, Mensagem: {}",
                ex.getErrorType(), ex.getMessage());

        HttpStatus status = switch (ex.getErrorType()) {
            case INSUFFICIENT_BALANCE, INVALID_QUANTITY -> HttpStatus.BAD_REQUEST;
            case CRYPTO_NOT_FOUND -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        return buildResponse(
                status,
                "Portfolio Error",
                ex.getMessage(),
                Map.of("errorType", ex.getErrorType().name()),
                request
        );
    }

    // ✅ NOVO: Trading Bot Exception
    @ExceptionHandler(TradingBotException.class)
    public ResponseEntity<Map<String, Object>> handleTradingBotException(
            TradingBotException ex, WebRequest request) {

        log.warn("Erro no trading bot - Tipo: {}, Mensagem: {}",
                ex.getErrorType(), ex.getMessage());

        HttpStatus status = switch (ex.getErrorType()) {
            case BOT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case BOT_ALREADY_RUNNING, BOT_CONFIGURATION_INVALID -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED_ACCESS -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        return buildResponse(
                status,
                "Trading Bot Error",
                ex.getMessage(),
                Map.of("errorType", ex.getErrorType().name()),
                request
        );
    }

    // ✅ NOVO: Configuration Exception
    @ExceptionHandler(ConfigurationException.class)
    public ResponseEntity<Map<String, Object>> handleConfigurationException(
            ConfigurationException ex, WebRequest request) {

        log.error("Erro de configuração - Chave: {}, Mensagem: {}",
                ex.getConfigKey(), ex.getMessage());

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Configuration Error",
                ex.getMessage(),
                Map.of("configKey", ex.getConfigKey()),
                request
        );
    }

    @ExceptionHandler({
            UserNotFoundException.class,
            CryptoNotFoundException.class
    })
    public ResponseEntity<Map<String, Object>> handleNotFound(
            RuntimeException ex, WebRequest request) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Not Found",
                ex.getMessage(),
                null,
                request
        );
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(
            RateLimitExceededException ex, WebRequest request) {

        return buildResponse(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                ex.getMessage(),
                null,
                request
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {

        log.error("Runtime exception não tratada:", ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "Erro inesperado no servidor",
                null,
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(
            Exception ex, WebRequest request) {

        log.error("Exceção não tratada:", ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "Ocorreu um erro inesperado",
                null,
                request
        );
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String error,
            String message,
            Object errors,
            WebRequest request
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));

        if (errors != null) {
            body.put("details", errors);
        }

        return ResponseEntity.status(status).body(body);
    }
}