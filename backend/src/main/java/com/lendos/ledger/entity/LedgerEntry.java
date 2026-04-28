package com.lendos.ledger.entity;

import com.lendos.common.entity.BaseEntity;
import com.lendos.identity.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * MODULE 5 — Double Entry Ledger
 *
 * CRITICAL DESIGN RULES (NEVER violate these):
 * 1. This table is APPEND-ONLY. No UPDATE or DELETE ever.
 * 2. Every financial event produces exactly TWO entries: one DEBIT, one CREDIT.
 * 3. Balances are COMPUTED from entries — never stored as a field.
 * 4. Both entries share the same transactionGroupId for reconciliation.
 *
 * Full implementation in Phase 3.
 */
@Entity
@Table(name = "ledger_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** Groups the DEBIT and CREDIT entries of a single financial event */
    @Column(nullable = false)
    private UUID transactionGroupId;

    /** ID of the source entity (payment ID, loan ID, etc.) */
    @Column(nullable = false)
    private UUID referenceId;

    /** Type of the source entity: PAYMENT, DISBURSEMENT, PENALTY */
    @Column(nullable = false)
    private String referenceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryType entryType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate valueDate;

    private String narration;

    public enum EntryType {
        DEBIT, CREDIT
    }

    public enum AccountType {
        LOAN_PRINCIPAL,
        LOAN_INTEREST,
        CASH,
        PENALTY_INCOME,
        INTEREST_INCOME,
        SUSPENSE
    }
}
