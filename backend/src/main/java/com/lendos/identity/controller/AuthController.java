package com.lendos.identity.controller;

import com.lendos.borrower.service.BorrowerPortalService;
import com.lendos.identity.dto.BorrowerAuthDtos;
import com.lendos.identity.dto.IdentityDtos;
import com.lendos.identity.security.LendosUserDetails;
import com.lendos.identity.service.AuthService;
import com.lendos.identity.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Tenant registration, login, token refresh and logout")
public class AuthController {

    private final AuthService authService;
    private final TenantService tenantService;
    private final BorrowerPortalService borrowerPortalService;

    @PostMapping("/register")
    @Operation(summary = "Register a new tenant (organization) with an admin user")
    public ResponseEntity<IdentityDtos.TenantResponse> registerTenant(
            @Valid @RequestBody IdentityDtos.RegisterTenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tenantService.registerTenant(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT access + refresh tokens")
    public ResponseEntity<IdentityDtos.AuthResponse> login(
            @Valid @RequestBody IdentityDtos.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/borrower/register")
    @Operation(summary = "Register a borrower account and borrower profile")
    public ResponseEntity<BorrowerAuthDtos.BorrowerAuthResponse> registerBorrower(
            @Valid @RequestBody BorrowerAuthDtos.RegisterBorrowerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                borrowerPortalService.registerBorrower(request)
        );
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using a valid refresh token")
    public ResponseEntity<IdentityDtos.AuthResponse> refresh(
            @Valid @RequestBody IdentityDtos.RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke all refresh tokens for the current user")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal LendosUserDetails currentUser) {
        authService.logout(currentUser.getUsername());
        return ResponseEntity.noContent().build();
    }
}
