package com.atharva.dealership.exception;

public class AuthenticationError extends RuntimeException {

    public AuthenticationError(String message) {
        super(message);
    }
}
