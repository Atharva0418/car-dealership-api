package com.atharva.dealership.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationError.class)
    public ResponseEntity<String> handleAuthenticationError(AuthenticationError error) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error.getMessage());
    }

    @ExceptionHandler(EmailAlreadyExistsError.class)
    public ResponseEntity<String> handleEmailAlreadyExists(EmailAlreadyExistsError error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error.getMessage());
    }

    @ExceptionHandler(ValidationError.class)
    public ResponseEntity<String> handleValidationError(ValidationError error) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error.getMessage());
    }
}
