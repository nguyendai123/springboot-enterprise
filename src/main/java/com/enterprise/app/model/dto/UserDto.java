package com.enterprise.app.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

// ── User DTO ─────────────────────────────────────────────────────────────
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    private String          id;
    private String          username;
    private String          email;
    private String          firstName;
    private String          lastName;
    private String          phoneNumber;
    private String          avatarUrl;
    private String          status;
    private List<String>    roles;
    private boolean         emailVerified;
    private LocalDateTime   lastLoginAt;
    private LocalDateTime   createdAt;
    private LocalDateTime   updatedAt;
}
