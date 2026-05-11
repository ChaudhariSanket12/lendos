package com.lendos.document.controller;

import com.lendos.borrower.entity.Borrower;
import com.lendos.borrower.repository.BorrowerRepository;
import com.lendos.common.exception.BusinessException;
import com.lendos.common.exception.ResourceNotFoundException;
import com.lendos.document.dto.DocumentDtos;
import com.lendos.document.entity.DocumentType;
import com.lendos.document.service.DocumentService;
import com.lendos.document.service.DocumentStorageService;
import com.lendos.identity.security.LendosUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/borrower/me/documents")
@RequiredArgsConstructor
@Tag(name = "Borrower Documents", description = "Borrower document upload metadata endpoints")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('BORROWER')")
public class DocumentController {

    private final BorrowerRepository borrowerRepository;
    private final DocumentService documentService;
    private final DocumentStorageService documentStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload borrower document via backend storage proxy")
    public ResponseEntity<DocumentDtos.DocumentResponse> uploadDocument(
            @AuthenticationPrincipal LendosUserDetails currentUser,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType
    ) {
        Borrower borrower = resolveBorrower(currentUser);

        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPTY_FILE", "File is required");
        }

        try {
            DocumentType validatedType = documentService.validateUploadAllowed(borrower, documentType);
            DocumentStorageService.UploadResult uploadResult = documentStorageService.uploadDocument(
                    file.getBytes(),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    borrower.getId().toString(),
                    validatedType.name()
            );

            DocumentDtos.DocumentResponse response = documentService.uploadDocument(
                    borrower,
                    validatedType.name(),
                    uploadResult.getPublicUrl(),
                    uploadResult.getStoragePath(),
                    uploadResult.getFileSize()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException ex) {
            throw new BusinessException("FILE_READ_FAILED", "Unable to read uploaded file");
        }
    }

    @GetMapping
    @Operation(summary = "Get all borrower documents")
    public ResponseEntity<List<DocumentDtos.DocumentResponse>> getDocuments(
            @AuthenticationPrincipal LendosUserDetails currentUser
    ) {
        Borrower borrower = resolveBorrower(currentUser);
        return ResponseEntity.ok(documentService.getDocuments(borrower));
    }

    @GetMapping("/{documentType}")
    @Operation(summary = "Get borrower document by type")
    public ResponseEntity<DocumentDtos.DocumentResponse> getDocumentByType(
            @AuthenticationPrincipal LendosUserDetails currentUser,
            @PathVariable String documentType
    ) {
        Borrower borrower = resolveBorrower(currentUser);
        return ResponseEntity.ok(documentService.getDocument(borrower, documentType));
    }

    @DeleteMapping("/{documentId}")
    @Operation(summary = "Delete borrower document by id (only pending/rejected)")
    public ResponseEntity<Void> deleteDocument(
            @AuthenticationPrincipal LendosUserDetails currentUser,
            @PathVariable UUID documentId
    ) {
        Borrower borrower = resolveBorrower(currentUser);
        documentService.deleteDocument(borrower, documentId);
        return ResponseEntity.noContent().build();
    }

    private Borrower resolveBorrower(LendosUserDetails currentUser) {
        return borrowerRepository.findByTenant_IdAndUser_Id(currentUser.getTenantId(), currentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Borrower", currentUser.getUserId().toString()));
    }
}
