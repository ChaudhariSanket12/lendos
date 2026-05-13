package com.lendos.document.service;

import com.lendos.borrower.entity.Borrower;
import com.lendos.common.exception.BusinessException;
import com.lendos.common.exception.ResourceNotFoundException;
import com.lendos.document.dto.DocumentDtos;
import com.lendos.document.entity.BorrowerDocument;
import com.lendos.document.entity.DocumentType;
import com.lendos.document.entity.VerificationStatus;
import com.lendos.document.repository.BorrowerDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService {

    private final BorrowerDocumentRepository borrowerDocumentRepository;
    private final DocumentStorageService documentStorageService;
    private final VisionOcrService visionOcrService;
    private final PanCardParser panCardParser;
    private final AadhaarCardParser aadhaarCardParser;
    private final DocumentVerificationService documentVerificationService;

    public DocumentDtos.DocumentResponse uploadDocument(
            Borrower borrower,
            String rawDocumentType,
            String documentUrl,
            String storagePath,
            long fileSize
    ) {
        DocumentType documentType = parseDocumentType(rawDocumentType);
        String normalizedDocumentUrl = normalize(documentUrl);
        String normalizedStoragePath = normalize(storagePath);

        if (!StringUtils.hasText(normalizedDocumentUrl)) {
            throw new BusinessException("DOCUMENT_URL_REQUIRED", "Document URL is required");
        }
        if (fileSize < 0) {
            throw new BusinessException("INVALID_FILE_SIZE", "File size cannot be negative");
        }

        borrowerDocumentRepository.findByBorrowerAndDocumentType(borrower, documentType)
                .ifPresent(existing -> {
                    if (existing.getVerificationStatus() == VerificationStatus.VERIFIED) {
                        throw new BusinessException(
                                "DOCUMENT_ALREADY_VERIFIED",
                                "Verified document cannot be replaced for type: " + documentType
                        );
                    }
                    borrowerDocumentRepository.delete(existing);
                });

        BorrowerDocument document = BorrowerDocument.builder()
                .borrower(borrower)
                .documentType(documentType)
                .documentUrl(normalizedDocumentUrl)
                .storagePath(normalizedStoragePath)
                .originalSize(fileSize)
                .compressedSize(fileSize)
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        BorrowerDocument saved = borrowerDocumentRepository.save(document);
        log.info("Borrower document uploaded: borrowerId={}, documentId={}, type={}",
                borrower.getId(), saved.getId(), saved.getDocumentType());
        return mapToResponse(saved);
    }

    public DocumentType validateUploadAllowed(Borrower borrower, String rawDocumentType) {
        DocumentType documentType = parseDocumentType(rawDocumentType);
        borrowerDocumentRepository.findByBorrowerAndDocumentType(borrower, documentType)
                .ifPresent(existing -> {
                    if (existing.getVerificationStatus() == VerificationStatus.VERIFIED) {
                        throw new BusinessException(
                                "DOCUMENT_ALREADY_VERIFIED",
                                "Verified document cannot be replaced for type: " + documentType
                        );
                    }
                });
        return documentType;
    }

    @Transactional(readOnly = true)
    public List<DocumentDtos.DocumentResponse> getDocuments(Borrower borrower) {
        return borrowerDocumentRepository.findByBorrowerOrderByCreatedAtDesc(borrower)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentDtos.DocumentResponse getDocument(Borrower borrower, String rawDocumentType) {
        return getDocument(borrower, parseDocumentType(rawDocumentType));
    }

    @Transactional(readOnly = true)
    public DocumentDtos.DocumentResponse getDocument(Borrower borrower, DocumentType documentType) {
        BorrowerDocument document = borrowerDocumentRepository.findByBorrowerAndDocumentType(borrower, documentType)
                .orElseThrow(() -> new ResourceNotFoundException("BorrowerDocument", documentType.name()));
        return mapToResponse(document);
    }

    public void deleteDocument(Borrower borrower, UUID documentId) {
        BorrowerDocument document = borrowerDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("BorrowerDocument", documentId.toString()));

        if (!document.getBorrower().getId().equals(borrower.getId())) {
            throw new BusinessException("DOCUMENT_ACCESS_DENIED", "Document not found for the current borrower");
        }
        if (document.getVerificationStatus() == VerificationStatus.VERIFIED) {
            throw new BusinessException("DOCUMENT_ALREADY_VERIFIED", "Verified document cannot be deleted");
        }

        if (StringUtils.hasText(document.getStoragePath())) {
            documentStorageService.deleteDocument(document.getStoragePath());
        }
        borrowerDocumentRepository.delete(document);
        log.info("Borrower document deleted: borrowerId={}, documentId={}", borrower.getId(), documentId);
    }

    public DocumentDtos.AdminVerificationResponse verifyDocumentForTenant(UUID tenantId, UUID documentId) {
        BorrowerDocument document = borrowerDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("BorrowerDocument", documentId.toString()));

        Borrower borrower = document.getBorrower();
        if (!borrower.getTenant().getId().equals(tenantId)) {
            throw new BusinessException("DOCUMENT_ACCESS_DENIED", "Document not found for the current tenant");
        }

        log.info("[VisionOCR] Admin verification started: tenantId={}, documentId={}", tenantId, documentId);

        String ocrText = visionOcrService.extractText(document.getDocumentUrl());
        Map<String, String> parsedData = switch (document.getDocumentType()) {
            case PAN -> panCardParser.parse(ocrText);
            case AADHAAR -> aadhaarCardParser.parse(ocrText);
        };

        DocumentVerificationService.VerificationResult verificationResult = documentVerificationService.evaluate(
                parsedData,
                borrower,
                document.getDocumentType()
        );

        VerificationStatus status = verificationResult.getVerificationStatus();
        LocalDateTime verifiedAt = LocalDateTime.now();

        document.setOcrText(ocrText);
        document.setVerificationStatus(status);
        document.setVerifiedAt(verifiedAt);
        borrowerDocumentRepository.save(document);

        Map<String, String> extractedData = new LinkedHashMap<>();
        extractedData.put("panNumber", parsedData.get("panNumber"));
        extractedData.put("aadhaarNumber", parsedData.get("aadhaarNumber"));
        extractedData.put("nameOnCard", parsedData.get("nameOnCard"));

        Map<String, String> profileData = new LinkedHashMap<>();
        profileData.put("panNumber", borrower.getPanNumber());
        profileData.put("aadhaarNumber", borrower.getAadhaarNumber());
        profileData.put("fullName", formatFullName(borrower));

        Map<String, Boolean> matches = new LinkedHashMap<>();
        matches.put("panMatch", verificationResult.isPanMatch());
        matches.put("aadhaarMatch", verificationResult.isAadhaarMatch());
        matches.put("nameMatch", verificationResult.isNameMatch());

        log.info("[Verification] Admin verification completed: documentId={}, status={}", documentId, status);

        return DocumentDtos.AdminVerificationResponse.builder()
                .documentId(document.getId())
                .documentType(document.getDocumentType().name())
                .verificationStatus(status.name())
                .verifiedAt(verifiedAt)
                .ocrText(ocrText)
                .extractedData(extractedData)
                .profileData(profileData)
                .matches(matches)
                .build();
    }

    public DocumentType parseDocumentType(String rawDocumentType) {
        String normalized = normalize(rawDocumentType);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException("INVALID_DOCUMENT_TYPE", "Document type is required");
        }
        try {
            return DocumentType.valueOf(normalized.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(
                    "INVALID_DOCUMENT_TYPE",
                    "Document type must be PAN or AADHAAR"
            );
        }
    }

    private DocumentDtos.DocumentResponse mapToResponse(BorrowerDocument document) {
        return DocumentDtos.DocumentResponse.builder()
                .id(document.getId())
                .documentType(document.getDocumentType().name())
                .documentUrl(document.getDocumentUrl())
                .storagePath(document.getStoragePath())
                .originalSize(document.getOriginalSize())
                .compressedSize(document.getCompressedSize())
                .verificationStatus(document.getVerificationStatus().name())
                .verifiedAt(document.getVerifiedAt())
                .uploadedAt(document.getCreatedAt())
                .build();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String formatFullName(Borrower borrower) {
        return String.format("%s %s",
                borrower.getFirstName() == null ? "" : borrower.getFirstName().trim(),
                borrower.getLastName() == null ? "" : borrower.getLastName().trim()).trim();
    }
}
