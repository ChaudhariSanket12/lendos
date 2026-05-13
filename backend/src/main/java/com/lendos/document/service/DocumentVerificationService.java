package com.lendos.document.service;

import com.lendos.borrower.entity.Borrower;
import com.lendos.document.entity.DocumentType;
import com.lendos.document.entity.VerificationStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class DocumentVerificationService {

    public VerificationStatus verify(Map<String, String> ocrData, Borrower borrower, DocumentType documentType) {
        return evaluate(ocrData, borrower, documentType).getVerificationStatus();
    }

    public VerificationResult evaluate(Map<String, String> ocrData, Borrower borrower, DocumentType documentType) {
        String ocrPan = normalize(ocrData == null ? null : ocrData.get("panNumber"));
        String ocrAadhaar = cleanDigits(ocrData == null ? null : ocrData.get("aadhaarNumber"));
        String ocrName = normalizeName(ocrData == null ? null : ocrData.get("nameOnCard"));

        String profilePan = normalize(borrower.getPanNumber());
        String profileAadhaar = cleanDigits(borrower.getAadhaarNumber());
        String profileName = normalizeName(String.format("%s %s",
                borrower.getFirstName() == null ? "" : borrower.getFirstName(),
                borrower.getLastName() == null ? "" : borrower.getLastName()));

        boolean panMatch = ocrPan != null && profilePan != null && ocrPan.equals(profilePan);
        boolean aadhaarMatch = ocrAadhaar != null && profileAadhaar != null && ocrAadhaar.equals(profileAadhaar);
        boolean nameMatch = ocrName != null && profileName != null && isSimilarName(ocrName, profileName);

        VerificationStatus status = switch (documentType) {
            case PAN -> panMatch ? VerificationStatus.VERIFIED : VerificationStatus.REJECTED;
            case AADHAAR -> aadhaarMatch ? VerificationStatus.VERIFIED : VerificationStatus.REJECTED;
        };

        log.info("[Verification] {} - panMatch={}, aadhaarMatch={}, nameMatch={}, status={}",
                documentType, panMatch, aadhaarMatch, nameMatch, status);

        return VerificationResult.builder()
                .panMatch(panMatch)
                .aadhaarMatch(aadhaarMatch)
                .nameMatch(nameMatch)
                .verificationStatus(status)
                .build();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.replaceAll("\\s+", "").toUpperCase();
    }

    private String cleanDigits(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.replaceAll("\\D", "");
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private boolean isSimilarName(String left, String right) {
        if (left == null || right == null || left.isBlank() || right.isBlank()) {
            return false;
        }

        String first = left.toLowerCase().trim();
        String second = right.toLowerCase().trim();
        if (first.equals(second) || first.contains(second) || second.contains(first)) {
            return true;
        }

        String[] words1 = first.split("\\s+");
        String[] words2 = second.split("\\s+");
        for (String w1 : words1) {
            for (String w2 : words2) {
                if (w1.equals(w2)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Getter
    @Builder
    public static class VerificationResult {
        private boolean panMatch;
        private boolean aadhaarMatch;
        private boolean nameMatch;
        private VerificationStatus verificationStatus;
    }
}
