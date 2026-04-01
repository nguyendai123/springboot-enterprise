package com.enterprise.app.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// ── Business / Domain ────────────────────────────────────────────────────
@Getter
public class BusinessException extends AppException {
    private final HttpStatus status;

    public BusinessException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

}
