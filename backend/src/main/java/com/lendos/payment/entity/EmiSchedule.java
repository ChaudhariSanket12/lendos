package com.lendos.payment.entity;

import com.lendos.common.entity.BaseEntity;
import com.lendos.loan.entity.Loan;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * MODULE 4 — Schedule & Payment Engine
 * EMI schedule rows are IMMUTABLE once generated at disbursement.
 * Payments are matched against schedule entries.
 * Full implementation in Phase 2.
 */
@Entity
@Table(name = "emi_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmiSchedule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(nullable = false)
    private Integer installmentNumber;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principalComponent;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal interestComponent;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalEmiAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal outstandingPrincipalAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmiStatus status;

    public enum EmiStatus {
        PENDING, PAID, PARTIALLY_PAID, OVERDUE, WAIVED
    }
}
