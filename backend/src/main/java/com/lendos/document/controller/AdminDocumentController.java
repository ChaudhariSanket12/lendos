package com.lendos.document.controller;

import com.lendos.document.dto.DocumentDtos;
import com.lendos.document.service.DocumentService;
import com.lendos.identity.security.LendosUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/documents")
@RequiredArgsConstructor
@Tag(name = "Admin Documents", description = "Admin OCR verification endpoints")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'CREDIT_OFFICER')")
public class AdminDocumentController {

    private final DocumentService documentService;

    @PostMapping("/{documentId}/verify")
    @Operation(summary = "Verify borrower document with OCR (synchronous)")
    public ResponseEntity<DocumentDtos.AdminVerificationResponse> verifyDocument(
            @AuthenticationPrincipal LendosUserDetails currentUser,
            @PathVariable UUID documentId
    ) {
        DocumentDtos.AdminVerificationResponse response = documentService.verifyDocumentForTenant(
                currentUser.getTenantId(),
                documentId
        );
        return ResponseEntity.ok(response);
    }
}
