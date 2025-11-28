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


    @ExceptionHandler({
            UserNotFoundException.class,
            CryptoNotFoundException.class
    })
    public ResponseEntity<Map<String, Object>> handleNotFound(RuntimeException ex, WebRequest request) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Not Found",
                ex.getMessage(),
                null,
                request
        );
    }


    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(RateLimitExceededException ex, WebRequest request) {

        return buildResponse(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                ex.getMessage(),
                null,
                request
        );
    }


    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex, WebRequest request) {

        log.error("Runtime exception:", ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                ex.getMessage(),
                null,
                request
        );
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex, WebRequest request) {

        log.error("Unexpected exception:", ex);

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
            body.put("errors", errors);
        }

        return ResponseEntity.status(status).body(body);
    }
}
