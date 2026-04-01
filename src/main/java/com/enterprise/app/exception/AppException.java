package com.enterprise.app.exception;

import org.springframework.http.HttpStatus;

// ── Base ───────────────────────────────────────────────────────────────────
public class AppException extends RuntimeException {
    public AppException(String message) { super(message); }
    public AppException(String message, Throwable cause) { super(message, cause); }
}

