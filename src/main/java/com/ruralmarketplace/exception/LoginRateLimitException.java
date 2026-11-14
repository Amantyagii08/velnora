package com.ruralmarketplace.exception;

public class LoginRateLimitException extends RuntimeException {
    public LoginRateLimitException(String message) { super(message); }
}
