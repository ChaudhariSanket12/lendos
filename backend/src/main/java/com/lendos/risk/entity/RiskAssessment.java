package com.lendos.risk.entity;

import com.lendos.common.entity.BaseEntity;
import com.lendos.loan.entity.Loan;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * MODULE 6 — Risk Engine
 *
 * Every credit decision is persisted here for auditability.
 * A CA firm must be able to explain WHY a loan was approved or rejected.
 * The reasonCodes and rulesEvaluated fields capture the full decision trail.
 *
 * Full implementation in Phase 4.
 */
@Entity
@Table(name = "risk_assessments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskAssessment extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    /** Score 0-100. Higher = riskier. */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskDecision decision;

    /** JSON array of triggered rule reason codes */
    @Column(columnDefinition = "TEXT")
    private String reasonCodes;

    /** JSON array of all rules evaluated with their verdicts */
    @Column(columnDefinition = "TEXT")
    private String rulesEvaluated;

    public enum RiskDecision {
        APPROVED, REJECTED, NEEDS_REVIEW
    }
}
