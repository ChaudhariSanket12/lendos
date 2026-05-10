package com.lendos.loan.dto;

import com.lendos.borrower.entity.Borrower;
import com.lendos.loan.entity.Loan;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class LoanDtos {

    @Getter
    @Setter
    public static class ApplyLoanApplicationRequest {
        private BigDecimal monthlyIncome;
        private Borrower.EmploymentType employmentType;
        private BigDecimal yearsInCurrentJob;
        private BigDecimal totalWorkExperience;
        private BigDecimal existingMonthlyObligations;
        private BigDecimal loanAmount;
        private Integer tenureMonths;
        private Loan.LoanPurpose loanPurpose;
        private Borrower.ResidenceType residenceType;
        private BigDecimal yearsAtCurrentResidence;
        private Integer cibilScore;
    }

    @Getter
    @Setter
    public static class UpdateLoanStatusRequest {
        private String status;
        private String notes;
    }

    @Getter
    @Builder
    public static class LoanResponse {
        private UUID id;
        private UUID borrowerId;
        private UUID tenantId;
        private BigDecimal loanAmount;
        private Integer tenureMonths;
        private Loan.LoanPurpose loanPurpose;
        private Loan.LoanStatus status;
        private BigDecimal annualInterestRate;
        private LocalDateTime appliedAt;
        private LocalDate disbursementDate;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Builder
    public static class LoanListItemResponse {
        private UUID id;
        private UUID borrowerId;
        private String borrowerName;
        private BigDecimal loanAmount;
        private Integer tenureMonths;
        private Loan.LoanPurpose loanPurpose;
        private Loan.LoanStatus status;
        private LocalDateTime appliedAt;
    }

    @Getter
    @Builder
    public static class LoanDetailResponse {
        private UUID id;
        private UUID borrowerId;
        private UUID tenantId;
        private BigDecimal loanAmount;
        private Integer tenureMonths;
        private Loan.LoanPurpose loanPurpose;
        private Loan.LoanStatus status;
        private BigDecimal annualInterestRate;
        private LocalDateTime appliedAt;
        private LocalDate disbursementDate;
        private String statusNotes;
        private BorrowerSnapshot borrower;
        private RiskSnapshot risk;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Builder
    public static class BorrowerSnapshot {
        private UUID id;
        private String fullName;
        private String email;
        private String phone;
        private Borrower.BorrowerStatus status;
        private String panNumber;
        private String aadhaarNumber;
        private BigDecimal monthlyIncome;
        private Borrower.EmploymentType employmentType;
        private BigDecimal yearsInCurrentJob;
        private BigDecimal totalWorkExperience;
        private BigDecimal existingMonthlyObligations;
        private Borrower.ResidenceType residenceType;
        private BigDecimal yearsAtCurrentResidence;
        private Integer cibilScore;
    }

    @Getter
    @Builder
    public static class RiskSnapshot {
        private BigDecimal riskScore;
        private BigDecimal foir;
        private String recommendation;
        private LocalDateTime assessedAt;
    }
}
