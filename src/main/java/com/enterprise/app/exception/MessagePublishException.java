package com.enterprise.app.exception;

// ── Messaging ─────────────────────────────────────────────────────────────
public class MessagePublishException extends AppException {
    public MessagePublishException(String topic, Throwable cause) {
        super("Failed to publish message to: " + topic, cause);
    }
}
