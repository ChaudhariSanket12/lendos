package com.lendos.document.service;

import com.lendos.common.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class DocumentStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;

    private static final String BUCKET = "borrower-documents";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    public UploadResult uploadDocument(
            byte[] fileBytes,
            String originalFilename,
            String contentType,
            String borrowerId,
            String documentType
    ) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new BusinessException("EMPTY_FILE", "File is empty");
        }
        if (fileBytes.length > MAX_FILE_SIZE) {
            throw new BusinessException("FILE_TOO_LARGE", "File size exceeds 5MB limit");
        }

        String normalizedType = contentType == null ? null : contentType.toLowerCase();
        if (normalizedType == null || !ALLOWED_TYPES.contains(normalizedType)) {
            throw new BusinessException("INVALID_FILE_TYPE", "Invalid file type. Allowed: JPEG, PNG, WebP");
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString().substring(0, 6);
        String extension = normalizedType.contains("webp")
                ? "webp"
                : normalizedType.contains("png")
                ? "png"
                : "jpg";
        String filename = documentType + "_" + timestamp + "_" + random + "." + extension;
        String path = borrowerId + "/" + filename;

        log.info("[DocumentStorage] Uploading to {}/{}", BUCKET, path);
        log.info("[DocumentStorage] File size: {} bytes, Type: {}, Original name: {}",
                fileBytes.length, normalizedType, originalFilename);

        String publicUrl = uploadToSupabase(path, fileBytes, normalizedType);
        log.info("[DocumentStorage] Upload successful. URL: {}", publicUrl);

        return new UploadResult(publicUrl, path, fileBytes.length);
    }

    private String uploadToSupabase(String path, byte[] fileBytes, String contentType) {
        RestTemplate restTemplate = new RestTemplate();
        String url = supabaseUrl + "/storage/v1/object/" + BUCKET + "/" + path;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(serviceRoleKey);
        headers.set("apikey", serviceRoleKey);
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.set("x-upsert", "false");

        HttpEntity<byte[]> requestEntity = new HttpEntity<>(fileBytes, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return supabaseUrl + "/storage/v1/object/public/" + BUCKET + "/" + path;
            }
            throw new BusinessException("DOCUMENT_UPLOAD_FAILED", "Upload failed: " + response.getBody());
        } catch (HttpClientErrorException ex) {
            log.error("[DocumentStorage] Upload failed: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BusinessException(
                    "DOCUMENT_UPLOAD_FAILED",
                    "Document upload failed. Please try again.",
                    HttpStatus.BAD_GATEWAY
            );
        } catch (Exception ex) {
            log.error("[DocumentStorage] Upload failed with unexpected error", ex);
            throw new BusinessException(
                    "DOCUMENT_UPLOAD_FAILED",
                    "Document upload failed. Please try again.",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    public void deleteDocument(String path) {
        if (path == null || path.isBlank()) {
            return;
        }

        RestTemplate restTemplate = new RestTemplate();
        String url = supabaseUrl + "/storage/v1/object/" + BUCKET + "/" + path;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(serviceRoleKey);
        headers.set("apikey", serviceRoleKey);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        try {
            restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, Void.class);
            log.info("[DocumentStorage] Deleted: {}", path);
        } catch (Exception ex) {
            log.warn("[DocumentStorage] Delete failed (may not exist): {}", path);
        }
    }

    @Getter
    @AllArgsConstructor
    public static class UploadResult {
        private String publicUrl;
        private String storagePath;
        private long fileSize;
    }
}
