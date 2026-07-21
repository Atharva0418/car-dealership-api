package com.atharva.dealership.exception;

public class ValidationError extends RuntimeException {

    public ValidationError(String message) {
        super(message);
    }
}
