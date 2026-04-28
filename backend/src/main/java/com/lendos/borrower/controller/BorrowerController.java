package com.lendos.borrower.controller;

import com.lendos.borrower.dto.BorrowerDtos;
import com.lendos.borrower.service.BorrowerService;
import com.lendos.identity.security.LendosUserDetails;
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
@RequestMapping("/api/v1/borrowers")
@RequiredArgsConstructor
@Tag(name = "Borrowers", description = "Borrower management within a tenant")
@SecurityRequirement(name = "bearerAuth")
public class BorrowerController {

    private final BorrowerService borrowerService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CREDIT_OFFICER')")
    @Operation(summary = "Create a borrower in the current tenant")
    public ResponseEntity<BorrowerDtos.BorrowerResponse> createBorrower(
            @AuthenticationPrincipal LendosUserDetails currentUser,
            @Valid @RequestBody BorrowerDtos.CreateBorrowerRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(borrowerService.createBorrower(currentUser.getTenantId(), request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CREDIT_OFFICER','AUDITOR')")
    @Operation(summary = "List borrowers in the current tenant with optional status filter")
    public ResponseEntity<List<BorrowerDtos.BorrowerResponse>> listBorrowers(
            @AuthenticationPrincipal LendosUserDetails currentUser,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(borrowerService.listBorrowers(currentUser.getTenantId(), status));
    }

    @GetMapping("/{borrowerId}")
    @PreAuthorize("hasAnyRole('ADMIN','CREDIT_OFFICER','AUDITOR')")
    @Operation(summary = "Get a borrower by ID in the current tenant")
    public ResponseEntity<BorrowerDtos.BorrowerResponse> getBorrowerById(
            @AuthenticationPrincipal LendosUserDetails currentUser,
            @PathVariable UUID borrowerId
    ) {
        return ResponseEntity.ok(borrowerService.getBorrowerById(currentUser.getTenantId(), borrowerId));
    }

    @PatchMapping("/{borrowerId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','CREDIT_OFFICER')")
    @Operation(summary = "Update borrower status with transition validation")
    public ResponseEntity<BorrowerDtos.BorrowerResponse> updateBorrowerStatus(
            @AuthenticationPrincipal LendosUserDetails currentUser,
            @PathVariable UUID borrowerId,
            @Valid @RequestBody BorrowerDtos.UpdateBorrowerStatusRequest request
    ) {
        return ResponseEntity.ok(
                borrowerService.updateBorrowerStatus(currentUser.getTenantId(), borrowerId, request)
        );
    }

    @DeleteMapping("/{borrowerId}")
    @PreAuthorize("hasAnyRole('ADMIN','CREDIT_OFFICER')")
    @Operation(summary = "Delete borrower (allowed only when status is DRAFT)")
    public ResponseEntity<Void> deleteBorrower(
            @AuthenticationPrincipal LendosUserDetails currentUser,
            @PathVariable UUID borrowerId
    ) {
        borrowerService.deleteBorrower(currentUser.getTenantId(), borrowerId);
        return ResponseEntity.noContent().build();
    }
}
