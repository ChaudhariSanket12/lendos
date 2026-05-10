package com.lendos.risk.service;

import com.lendos.loan.entity.Loan;
import com.lendos.risk.entity.RiskAssessment;
import com.lendos.risk.repository.RiskAssessmentRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskEvaluationService {

    private static final BigDecimal MONTHLY_RATE = new BigDecimal("0.01");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final RiskAssessmentRepository riskAssessmentRepository;

    @Transactional
    public RiskEvaluationResult evaluateLoan(
            Loan loan,
            BigDecimal monthlyIncome,
            BigDecimal existingMonthlyObligations
    ) {
        BigDecimal estimatedEmi = calculateEstimatedEmi(loan.getLoanAmount(), loan.getTenureMonths());
        BigDecimal totalFixedObligation = existingMonthlyObligations.add(estimatedEmi);
        BigDecimal foir = totalFixedObligation
                .multiply(new BigDecimal("100"))
                .divide(monthlyIncome, 2, RoundingMode.HALF_UP);

        String recommendation = deriveRecommendation(foir);
        BigDecimal riskScore = deriveRiskScore(foir);
        RiskAssessment.RiskDecision decision = mapDecision(recommendation);

        RiskAssessment assessment = riskAssessmentRepository.findByLoan_Id(loan.getId())
                .orElseGet(() -> RiskAssessment.builder().loan(loan).build());
        assessment.setRiskScore(riskScore);
        assessment.setFoir(foir);
        assessment.setDecision(decision);
        assessment.setReasonCodes(buildReasonCodes(recommendation, foir));
        assessment.setRulesEvaluated(buildRulesEvaluated(estimatedEmi, monthlyIncome, existingMonthlyObligations, foir));
        RiskAssessment saved = riskAssessmentRepository.save(assessment);

        log.info("Risk evaluation completed: loanId={}, score={}, foir={}, recommendation={}",
                loan.getId(), riskScore, foir, recommendation);

        return RiskEvaluationResult.builder()
                .assessmentId(saved.getId())
                .riskScore(riskScore)
                .foir(foir)
                .recommendation(recommendation)
                .estimatedEmi(estimatedEmi)
                .build();
    }

    private BigDecimal calculateEstimatedEmi(BigDecimal principal, Integer tenureMonths) {
        double r = MONTHLY_RATE.doubleValue();
        double n = tenureMonths.doubleValue();
        double numerator = principal.doubleValue() * r * Math.pow(1 + r, n);
        double denominator = Math.pow(1 + r, n) - 1;
        if (denominator <= 0) {
            return ZERO;
        }
        return BigDecimal.valueOf(numerator / denominator).setScale(2, RoundingMode.HALF_UP);
    }

    private String deriveRecommendation(BigDecimal foir) {
        if (foir.compareTo(new BigDecimal("40")) < 0) {
            return "APPROVE";
        }
        if (foir.compareTo(new BigDecimal("50")) <= 0) {
            return "APPROVE_WITH_CAUTION";
        }
        return "REJECT";
    }

    private BigDecimal deriveRiskScore(BigDecimal foir) {
        BigDecimal scaled = foir.multiply(new BigDecimal("0.50"));
        BigDecimal raw = new BigDecimal("60").add(scaled);
        if (raw.compareTo(new BigDecimal("85")) > 0) {
            return new BigDecimal("85.00");
        }
        if (raw.compareTo(new BigDecimal("60")) < 0) {
            return new BigDecimal("60.00");
        }
        return raw.setScale(2, RoundingMode.HALF_UP);
    }

    private RiskAssessment.RiskDecision mapDecision(String recommendation) {
        return switch (recommendation) {
            case "APPROVE" -> RiskAssessment.RiskDecision.APPROVED;
            case "APPROVE_WITH_CAUTION" -> RiskAssessment.RiskDecision.NEEDS_REVIEW;
            default -> RiskAssessment.RiskDecision.REJECTED;
        };
    }

    private String buildReasonCodes(String recommendation, BigDecimal foir) {
        return "[\"RECOMMENDATION_" + recommendation + "\",\"FOIR_" + foir + "\"]";
    }

    private String buildRulesEvaluated(
            BigDecimal estimatedEmi,
            BigDecimal monthlyIncome,
            BigDecimal existingMonthlyObligations,
            BigDecimal foir
    ) {
        return "[{\"rule\":\"FOIR\",\"estimatedEmi\":" + estimatedEmi
                + ",\"monthlyIncome\":" + monthlyIncome
                + ",\"existingMonthlyObligations\":" + existingMonthlyObligations
                + ",\"foir\":" + foir
                + "}]";
    }

    @Getter
    @Builder
    public static class RiskEvaluationResult {
        private UUID assessmentId;
        private BigDecimal riskScore;
        private BigDecimal foir;
        private String recommendation;
        private BigDecimal estimatedEmi;
    }
}
