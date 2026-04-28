package com.lendos.identity.controller;

import com.lendos.identity.dto.IdentityDtos;
import com.lendos.identity.security.LendosUserDetails;
import com.lendos.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Manage users within a tenant")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user's profile")
    public ResponseEntity<IdentityDtos.UserResponse> getCurrentUser(
            @AuthenticationPrincipal LendosUserDetails currentUser) {
        return ResponseEntity.ok(userService.getUserById(currentUser.getUserId()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new user in the current tenant (Admin only)")
    public ResponseEntity<IdentityDtos.UserResponse> createUser(
            @AuthenticationPrincipal LendosUserDetails currentUser,
            @Valid @RequestBody IdentityDtos.CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createUser(currentUser.getTenantId(), request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users in the current tenant (Admin only)")
    public ResponseEntity<List<IdentityDtos.UserResponse>> getTenantUsers(
            @AuthenticationPrincipal LendosUserDetails currentUser) {
        return ResponseEntity.ok(userService.getUsersByTenant(currentUser.getTenantId()));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get a specific user by ID (Admin only)")
    public ResponseEntity<IdentityDtos.UserResponse> getUserById(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user status - ACTIVE, INACTIVE, LOCKED (Admin only)")
    public ResponseEntity<IdentityDtos.UserResponse> updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody IdentityDtos.UpdateUserStatusRequest request) {
        return ResponseEntity.ok(userService.updateUserStatus(userId, request));
    }
}
