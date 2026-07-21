package com.atharva.dealership.exception;

public class EmailAlreadyExistsError extends RuntimeException {

    public EmailAlreadyExistsError(String message) {
        super(message);
    }
}
