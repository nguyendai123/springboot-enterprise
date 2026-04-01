package com.enterprise.app.exception;

// ── gRPC ─────────────────────────────────────────────────────────────────
public class GrpcCommunicationException extends AppException {
    public GrpcCommunicationException(String service, Throwable cause) {
        super("gRPC call to service '" + service + "' failed", cause);
    }
}
