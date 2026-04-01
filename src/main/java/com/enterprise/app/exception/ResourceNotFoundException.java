package com.enterprise.app.exception;

// ── Resource Not Found ─────────────────────────────────────────────────────
public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException(String resource, String identifier) {
        super(resource + " not found: " + identifier);
    }
}
