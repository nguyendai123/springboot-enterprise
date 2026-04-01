package com.enterprise.app.exception;

// ── Authentication ────────────────────────────────────────────────────────
public class InvalidTokenException extends AppException {
    public InvalidTokenException(String message) { super(message); }
}
