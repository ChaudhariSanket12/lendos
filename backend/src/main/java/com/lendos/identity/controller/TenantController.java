package com.lendos.identity.controller;

import com.lendos.identity.dto.IdentityDtos;
import com.lendos.identity.security.LendosUserDetails;
import com.lendos.identity.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenants", description = "Tenant metadata endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TenantController {

    private final TenantService tenantService;

    @GetMapping("/me/firm-code")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get current admin tenant firm code")
    public ResponseEntity<IdentityDtos.FirmCodeResponse> getMyFirmCode(
            @AuthenticationPrincipal LendosUserDetails currentUser
    ) {
        return ResponseEntity.ok(tenantService.getFirmCodeByTenantId(currentUser.getTenantId()));
    }
}
