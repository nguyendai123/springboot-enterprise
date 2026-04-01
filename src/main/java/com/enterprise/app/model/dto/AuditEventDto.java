package com.enterprise.app.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

// ── Audit Event ──────────────────────────────────────────────────────────
@Data
@Builder
public class AuditEventDto {
    private String        id;
    private String        entityType;
    private String        entityId;
    private String        action;
    private String        actorId;
    private String        actorUsername;
    private Object        before;
    private Object        after;
    private String        ipAddress;
    private LocalDateTime occurredAt;
}
