package com.atharva.dealership.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationError.class)
    public ResponseEntity<String> handleAuthenticationError(AuthenticationError error) {
        log.warn("Authentication error: {}", error.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error.getMessage());
    }

    @ExceptionHandler(EmailAlreadyExistsError.class)
    public ResponseEntity<String> handleEmailAlreadyExists(EmailAlreadyExistsError error) {
        log.warn("Email conflict: {}", error.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error.getMessage());
    }

    @ExceptionHandler(ValidationError.class)
    public ResponseEntity<String> handleValidationError(ValidationError error) {
        log.warn("Validation error: {}", error.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException error) {
        log.warn("Request failed with status {}: {}", error.getStatusCode(), error.getReason());
        return ResponseEntity.status(error.getStatusCode()).body(error.getReason());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleHttpMessageNotReadable(HttpMessageNotReadableException error) {
        log.warn("Malformed request body: {}", error.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Request body is missing or malformed.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException error) {
        log.warn("Request parameter type mismatch: {}", error.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Request parameter is invalid.");
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<String> handleDataAccessError(DataAccessException error) {
        log.error("Vehicle data access error", error);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Vehicle data could not be retrieved.");
    }
}
