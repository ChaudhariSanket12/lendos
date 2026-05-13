package com.lendos.loan.dto;

import com.lendos.borrower.entity.Borrower;
import com.lendos.loan.entity.Loan;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LoanDtos {

    @Getter
    @Setter
    public static class ApplyLoanApplicationRequest {
        private BigDecimal monthlyIncome;
        private Borrower.EmploymentType employmentType;
        private String employerName;
        private String industryType;
        private String salaryPaymentMode;
        private BigDecimal yearsInCurrentJob;
        private BigDecimal totalWorkExperience;
        private BigDecimal rentExpense;
        private BigDecimal existingLoanEmis;
        private BigDecimal creditCardPayments;
        private BigDecimal otherFixedExpenses;
        private BigDecimal loanAmount;
        private Integer tenureMonths;
        private Loan.LoanPurpose loanPurpose;
        private Map<String, String> documentUrls;
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
    @Setter
    public static class RejectLoanRequest {
        private String reason;
    }

    @Getter
    @Setter
    public static class DeleteLoanRequest {
        private String finalMessage;
    }

    @Getter
    @Builder
    public static class LoanActionResponse {
        private UUID loanId;
        private String status;
        private String rejectionMessage;
        private LocalDateTime rejectedAt;
        private String rejectedBy;
        private LocalDateTime deletedAt;
        private String message;
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
        private String rejectionMessage;
        private LocalDateTime rejectedAt;
        private String rejectedByName;
        private LocalDateTime deletedAt;
        private String deletedBy;
        private Boolean isDeleted;
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
        private String employerName;
        private String industryType;
        private String salaryPaymentMode;
        private BigDecimal yearsInCurrentJob;
        private BigDecimal totalWorkExperience;
        private BigDecimal rentExpense;
        private BigDecimal existingLoanEmis;
        private BigDecimal creditCardPayments;
        private BigDecimal otherFixedExpenses;
        private BigDecimal existingMonthlyObligations;
        private Borrower.ResidenceType residenceType;
        private BigDecimal yearsAtCurrentResidence;
        private Integer cibilScore;
        private List<DocumentSnapshot> documents;
    }

    @Getter
    @Builder
    public static class DocumentSnapshot {
        private UUID id;
        private String documentType;
        private String documentUrl;
        private String verificationStatus;
        private LocalDateTime verifiedAt;
        private LocalDateTime uploadedAt;
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
