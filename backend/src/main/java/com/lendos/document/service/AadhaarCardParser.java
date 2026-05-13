package com.lendos.document.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AadhaarCardParser {

    private static final Pattern AADHAAR_PATTERN = Pattern.compile("\\d{4}\\s?\\d{4}\\s?\\d{4}");

    public Map<String, String> parse(String ocrText) {
        Map<String, String> result = new HashMap<>();
        if (ocrText == null || ocrText.isBlank()) {
            return result;
        }

        Matcher aadhaarMatcher = AADHAAR_PATTERN.matcher(ocrText);
        if (aadhaarMatcher.find()) {
            String aadhaar = aadhaarMatcher.group().replaceAll("\\s", "");
            result.put("aadhaarNumber", aadhaar);
            log.info("[AadhaarCardParser] Found Aadhaar number");
        }

        String[] lines = ocrText.split("\\R");
        for (String line : lines) {
            String clean = line.trim();
            String upper = clean.toUpperCase();
            if (clean.length() > 3
                    && !clean.matches(".*\\d.*")
                    && !upper.contains("GOVERNMENT")
                    && !upper.contains("AADHAAR")
                    && !upper.contains("AUTHORITY")) {
                result.put("nameOnCard", clean);
                break;
            }
        }

        log.info("[AadhaarCardParser] Extracted fields={}", result.keySet());
        return result;
    }
}
