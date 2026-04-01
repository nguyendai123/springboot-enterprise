package com.enterprise.app.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

// ── Notification ─────────────────────────────────────────────────────────
@Data
@Builder
public class NotificationDto {
    private String        id;
    private String        userId;
    private String        type;   // EMAIL | SMS | PUSH | IN_APP
    private String        title;
    private String        message;
    private boolean       read;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
}
