package com.lendos.loan.controller;

import com.lendos.identity.security.LendosUserDetails;
import com.lendos.loan.dto.LoanDtos;
import com.lendos.loan.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Tag(name = "Loans", description = "Loan application and review endpoints")
@SecurityRequirement(name = "bearerAuth")
public class LoanController {

    private final LoanService loanService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CREDIT_OFFICER','AUDITOR')")
    @Operation(summary = "List loans for current tenant with optional status filter")
    public ResponseEntity<List<LoanDtos.LoanListItemResponse>> listLoans(
            @AuthenticationPrincipal LendosUserDetails currentUser,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(
                loanService.listTenantLoans(currentUser.getTenantId(), status)
        );
    }

    @GetMapping("/{loanId}")
    @PreAuthorize("hasAnyRole('ADMIN','CREDIT_OFFICER','AUDITOR')")
    @Operation(summary = "Get tenant loan detail by ID")
    public ResponseEntity<LoanDtos.LoanDetailResponse> getLoanById(
            @AuthenticationPrincipal LendosUserDetails currentUser,
            @PathVariable UUID loanId
    ) {
        return ResponseEntity.ok(
                loanService.getTenantLoanById(currentUser.getTenantId(), loanId)
        );
    }

    @PatchMapping("/{loanId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','CREDIT_OFFICER')")
    @Operation(summary = "Update tenant loan status with transition validation")
    public ResponseEntity<LoanDtos.LoanDetailResponse> updateLoanStatus(
            @AuthenticationPrincipal LendosUserDetails currentUser,
            @PathVariable UUID loanId,
            @Valid @RequestBody LoanDtos.UpdateLoanStatusRequest request
    ) {
        return ResponseEntity.ok(
                loanService.updateLoanStatus(currentUser.getTenantId(), loanId, request)
        );
    }
}
