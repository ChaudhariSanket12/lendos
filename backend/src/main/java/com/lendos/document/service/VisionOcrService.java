package com.lendos.document.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class VisionOcrService {

    private static final String VISION_API_URL = "https://vision.googleapis.com/v1/images:annotate";

    private final ObjectMapper objectMapper;

    @Value("${google.cloud.vision.api-key:}")
    private String visionApiKey;

    public String extractText(String imageUrl) {
        log.info("[VisionOCR] Starting text extraction");

        if (visionApiKey == null || visionApiKey.isBlank()) {
            log.warn("[VisionOCR] API key not configured. Set GOOGLE_VISION_API_KEY before startup.");
            throw new IllegalStateException("OCR service not configured");
        }

        try {
            byte[] imageBytes = downloadImage(imageUrl);
            log.info("[VisionOCR] Downloaded image bytes={}", imageBytes.length);

            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String response = callVisionApi(base64Image);
            String extractedText = parseResponse(response);

            log.info("[VisionOCR] Extracted characters={}", extractedText.length());
            return extractedText;
        } catch (Exception ex) {
            log.error("[VisionOCR] OCR request failed: {}", ex.getMessage(), ex);
            throw new RuntimeException("OCR failed: " + ex.getMessage(), ex);
        }
    }

    private byte[] downloadImage(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        try (InputStream in = url.openStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            return out.toByteArray();
        }
    }

    private String callVisionApi(String base64Image) throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        String url = VISION_API_URL + "?key=" + visionApiKey;

        Map<String, Object> requestBody = Map.of(
                "requests", List.of(
                        Map.of(
                                "image", Map.of("content", base64Image),
                                "features", List.of(Map.of("type", "TEXT_DETECTION"))
                        )
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            return response.getBody() == null ? "" : response.getBody();
        } catch (HttpStatusCodeException ex) {
            log.error("[VisionOCR] Vision API error status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new IllegalStateException("Vision API request failed with status " + ex.getStatusCode().value());
        }
    }

    private String parseResponse(String jsonResponse) throws IOException {
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode responses = root.path("responses");
        if (!responses.isArray() || responses.isEmpty()) {
            return "";
        }

        JsonNode firstResponse = responses.get(0);
        if (firstResponse.has("error")) {
            String message = firstResponse.path("error").path("message").asText("Unknown Vision API error");
            throw new IllegalStateException(message);
        }

        JsonNode fullText = firstResponse.path("fullTextAnnotation").path("text");
        if (!fullText.isMissingNode()) {
            return fullText.asText("");
        }

        JsonNode textAnnotations = firstResponse.path("textAnnotations");
        if (textAnnotations.isArray() && !textAnnotations.isEmpty()) {
            return textAnnotations.get(0).path("description").asText("");
        }

        return "";
    }
}
