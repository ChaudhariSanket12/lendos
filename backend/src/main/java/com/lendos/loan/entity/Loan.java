package com.lendos.loan.entity;

import com.lendos.borrower.entity.Borrower;
import com.lendos.common.entity.BaseEntity;
import com.lendos.identity.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MODULE 3 — Loan Lifecycle
 * State machine: APPLIED → UNDER_ASSESSMENT → APPROVED/REJECTED
 *               → DISBURSED → ACTIVE → CLOSED/DEFAULTED
 * Full implementation in Phase 2.
 */
@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = false)
    private Borrower borrower;

    @Column(name = "loan_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal loanAmount;

    @Column(name = "principal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal annualInterestRate;

    @Column(nullable = false)
    private Integer tenureMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanPurpose loanPurpose;

    @Column(nullable = false)
    private LocalDateTime appliedAt;

    private LocalDate disbursementDate;

    @Column(columnDefinition = "TEXT")
    private String statusNotes;

    public enum LoanStatus {
        APPLIED, UNDER_ASSESSMENT, APPROVED, REJECTED,
        DISBURSED, ACTIVE, CLOSED, DEFAULTED
    }

    public enum LoanPurpose {
        DEBT_CONSOLIDATION,
        HOME_RENOVATION,
        MEDICAL,
        EDUCATION,
        BUSINESS,
        WEDDING,
        TRAVEL,
        VEHICLE,
        OTHER
    }
}
