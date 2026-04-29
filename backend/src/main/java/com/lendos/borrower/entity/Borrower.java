package com.lendos.borrower.entity;

import com.lendos.common.entity.BaseEntity;
import com.lendos.identity.entity.User;
import com.lendos.identity.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * MODULE 2 — Borrower & Onboarding
 * State machine: DRAFT → UNDER_REVIEW → VERIFIED → ACTIVE → BLACKLISTED
 * Full implementation in Phase 2.
 */
@Entity
@Table(name = "borrowers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Borrower extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String email;

    private String phone;

    private LocalDate dateOfBirth;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(precision = 15, scale = 2)
    private BigDecimal monthlyIncome;

    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    @Column(precision = 4, scale = 1)
    private BigDecimal yearsInCurrentJob;

    @Column(precision = 15, scale = 2)
    private BigDecimal existingMonthlyObligations;

    @Column(length = 10)
    private String panNumber;

    private Integer creditScore;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BorrowerStatus status;

    public enum BorrowerStatus {
        DRAFT, UNDER_REVIEW, VERIFIED, ACTIVE, BLACKLISTED
    }

    public enum EmploymentType {
        SALARIED, SELF_EMPLOYED, GOVERNMENT, BUSINESS
    }
}
