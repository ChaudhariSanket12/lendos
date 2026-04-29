package com.lendos.borrower.controller;

import com.lendos.borrower.dto.BorrowerDtos;
import com.lendos.borrower.service.BorrowerPortalService;
import com.lendos.identity.security.LendosUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/borrower")
@RequiredArgsConstructor
@Tag(name = "Borrower Self Service", description = "Borrower self-service profile operations")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('BORROWER')")
public class BorrowerPortalController {

    private final BorrowerPortalService borrowerPortalService;

    @GetMapping("/me")
    @Operation(summary = "Get current borrower summary profile")
    public ResponseEntity<BorrowerDtos.BorrowerMeResponse> getMyBorrower(
            @AuthenticationPrincipal LendosUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                borrowerPortalService.getMyBorrower(currentUser.getTenantId(), currentUser.getUserId())
        );
    }

    @GetMapping("/me/profile")
    @Operation(summary = "Get current borrower full profile")
    public ResponseEntity<BorrowerDtos.BorrowerProfileResponse> getMyBorrowerProfile(
            @AuthenticationPrincipal LendosUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                borrowerPortalService.getMyBorrowerProfile(currentUser.getTenantId(), currentUser.getUserId())
        );
    }

    @PutMapping("/me/profile")
    @Operation(summary = "Update current borrower financial profile")
    public ResponseEntity<BorrowerDtos.BorrowerProfileResponse> updateMyBorrowerProfile(
            @AuthenticationPrincipal LendosUserDetails currentUser,
            @Valid @RequestBody BorrowerDtos.UpdateBorrowerProfileRequest request
    ) {
        return ResponseEntity.ok(
                borrowerPortalService.updateMyBorrowerProfile(currentUser.getTenantId(), currentUser.getUserId(), request)
        );
    }

    @PutMapping("/me/complete-profile")
    @Operation(summary = "Complete borrower profile and move to under review")
    public ResponseEntity<BorrowerDtos.BorrowerProfileResponse> completeMyBorrowerProfile(
            @AuthenticationPrincipal LendosUserDetails currentUser,
            @Valid @RequestBody BorrowerDtos.CompleteBorrowerProfileRequest request
    ) {
        return ResponseEntity.ok(
                borrowerPortalService.completeMyBorrowerProfile(currentUser.getTenantId(), currentUser.getUserId(), request)
        );
    }
}
