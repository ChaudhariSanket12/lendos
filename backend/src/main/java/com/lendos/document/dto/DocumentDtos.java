package com.lendos.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class DocumentDtos {

    @Getter
    @Setter
    public static class DocumentUploadRequest {
        @NotNull(message = "Document type is required")
        private String documentType;

        @NotBlank(message = "Document URL is required")
        private String documentUrl;

        private String storagePath;
        private Long originalSize;
        private Long compressedSize;
    }

    @Getter
    @Builder
    public static class DocumentResponse {
        private UUID id;
        private String documentType;
        private String documentUrl;
        private String storagePath;
        private Long originalSize;
        private Long compressedSize;
        private String verificationStatus;
        private LocalDateTime verifiedAt;
        private LocalDateTime uploadedAt;
    }

    @Getter
    @Builder
    public static class AdminVerificationResponse {
        private UUID documentId;
        private String documentType;
        private String verificationStatus;
        private LocalDateTime verifiedAt;
        private String ocrText;
        private Map<String, String> extractedData;
        private Map<String, String> profileData;
        private Map<String, Boolean> matches;
    }
}
