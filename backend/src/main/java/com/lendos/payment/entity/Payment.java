package com.lendos.payment.entity;

import com.lendos.common.entity.BaseEntity;
import com.lendos.loan.entity.Loan;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * MODULE 4 — Payment Recording
 * Idempotency key prevents duplicate payment recording on retries.
 * Every payment triggers a ledger entry pair (see Module 5).
 * Full implementation in Phase 2.
 */
@Entity
@Table(name = "payments",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "idempotency_key")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amountPaid;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principalPortion;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal interestPortion;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal penaltyPortion;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMode paymentMode;

    private String referenceNumber;

    public enum PaymentMode {
        CASH, BANK_TRANSFER, CHEQUE, UPI, NEFT, RTGS
    }
}
