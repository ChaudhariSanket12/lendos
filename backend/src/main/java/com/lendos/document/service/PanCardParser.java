package com.lendos.document.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PanCardParser {

    private static final Pattern PAN_PATTERN = Pattern.compile("(?i)[A-Z]{5}[0-9]{4}[A-Z]");

    public Map<String, String> parse(String ocrText) {
        Map<String, String> result = new HashMap<>();
        if (ocrText == null || ocrText.isBlank()) {
            return result;
        }

        Matcher panMatcher = PAN_PATTERN.matcher(ocrText);
        if (panMatcher.find()) {
            String pan = panMatcher.group().toUpperCase();
            result.put("panNumber", pan);
            log.info("[PanCardParser] Found PAN={}", pan);
        }

        log.info("[PanCardParser] Extracted fields={}", result.keySet());
        return result;
    }
}
