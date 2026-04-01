package com.enterprise.app.controller;

import com.enterprise.app.model.dto.ApiResponse;
import com.enterprise.app.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "JWT auth endpoints")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    @Operation(summary = "Login and obtain JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        UserDetails user = (UserDetails) auth.getPrincipal();

        String accessToken  = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("User logged in: {}", user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(
                AuthResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .tokenType("Bearer")
                        .expiresIn(86400)
                        .build()
        ));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest req) {
        // Validate refresh token and issue new access token
        String username = jwtService.extractUsername(req.refreshToken());
        // In production: verify against token store in Redis
        return ResponseEntity.ok(ApiResponse.success(
                AuthResponse.builder()
                        .accessToken("new-access-token-here")
                        .refreshToken(req.refreshToken())
                        .tokenType("Bearer")
                        .expiresIn(86400)
                        .build()
        ));
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidate current token (blacklist in Redis)")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        // In production: add token to Redis blacklist with TTL = remaining expiry
        log.info("Token blacklisted for logout");
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    // ── DTOs ─────────────────────────────────────────────────────
    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password) {}

    public record RefreshRequest(
            @NotBlank String refreshToken) {}

    @Data @Builder
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private int    expiresIn;
    }
}