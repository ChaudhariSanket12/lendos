package com.lendos.identity.dto;

import com.lendos.identity.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

public class IdentityDtos {

    // ── Tenant Registration ─────────────────────────────────────────
    @Getter
    @Setter
    public static class RegisterTenantRequest {

        @NotBlank(message = "Organization name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        private String name;

        @NotBlank(message = "Contact email is required")
        @Email(message = "Invalid email format")
        private String contactEmail;

        @NotBlank(message = "Admin full name is required")
        private String adminFullName;

        @NotBlank(message = "Admin password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String adminPassword;
    }

    // ── Login ───────────────────────────────────────────────────────
    @Getter
    @Setter
    public static class LoginRequest {

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    // ── Auth Response ───────────────────────────────────────────────
    @Getter
    @Builder
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private long expiresIn;
        private UserResponse user;
    }

    // ── Refresh Token Request ───────────────────────────────────────
    @Getter
    @Setter
    public static class RefreshTokenRequest {

        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    // ── Create User ─────────────────────────────────────────────────
    @Getter
    @Setter
    public static class CreateUserRequest {

        @NotBlank(message = "Full name is required")
        private String fullName;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        @NotNull(message = "Role is required")
        private User.Role role;
    }

    // ── Update User Status ──────────────────────────────────────────
    @Getter
    @Setter
    public static class UpdateUserStatusRequest {

        @NotNull(message = "Status is required")
        private User.UserStatus status;
    }

    // ── User Response ───────────────────────────────────────────────
    @Getter
    @Builder
    public static class UserResponse {
        private UUID id;
        private String fullName;
        private String email;
        private User.Role role;
        private User.UserStatus status;
        private UUID tenantId;
        private String tenantName;
        private LocalDateTime createdAt;
    }

    // ── Tenant Response ─────────────────────────────────────────────
    @Getter
    @Builder
    public static class TenantResponse {
        private UUID id;
        private String name;
        private String slug;
        private String firmCode;
        private String contactEmail;
        private String status;
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    public static class FirmCodeResponse {
        private UUID tenantId;
        private String tenantName;
        private String firmCode;
    }
}
