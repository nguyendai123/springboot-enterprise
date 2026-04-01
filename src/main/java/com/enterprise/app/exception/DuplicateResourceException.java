package com.enterprise.app.exception;

// ── Duplicate Resource ────────────────────────────────────────────────────
public class DuplicateResourceException extends AppException {
    public DuplicateResourceException(String resource, String field, Object value) {
        super(resource + " already exists with " + field + "='" + value + "'");
    }
}
