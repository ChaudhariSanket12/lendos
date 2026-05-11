package com.lendos.loan.service;

import com.lendos.borrower.entity.Borrower;
import com.lendos.borrower.repository.BorrowerRepository;
import com.lendos.common.exception.BusinessException;
import com.lendos.common.exception.ResourceNotFoundException;
import com.lendos.common.exception.ValidationException;
import com.lendos.document.entity.BorrowerDocument;
import com.lendos.document.repository.BorrowerDocumentRepository;
import com.lendos.document.service.DocumentStorageService;
import com.lendos.identity.entity.User;
import com.lendos.loan.dto.LoanDtos;
import com.lendos.loan.entity.Loan;
import com.lendos.loan.repository.LoanRepository;
import com.lendos.risk.entity.RiskAssessment;
import com.lendos.risk.repository.RiskAssessmentRepository;
import com.lendos.risk.service.RiskEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {

    private static final BigDecimal MIN_MONTHLY_INCOME = new BigDecimal("10000");
    private static final BigDecimal MAX_MONTHLY_INCOME = new BigDecimal("10000000");
    private static final BigDecimal MIN_LOAN_AMOUNT = new BigDecimal("5000");
    private static final BigDecimal MAX_LOAN_AMOUNT = new BigDecimal("5000000");
    private static final BigDecimal ANNUAL_INTEREST_RATE = new BigDecimal("12.00");
    private static final Set<Integer> ALLOWED_TENURES = Set.of(3, 6, 12, 18, 24, 36, 48, 60);
    private static final Set<String> ALLOWED_INDUSTRY_TYPES = Set.of(
            "INFORMATION_TECHNOLOGY",
            "MANUFACTURING",
            "BANKING",
            "HEALTHCARE",
            "EDUCATION",
            "RETAIL",
            "CONSTRUCTION",
            "OTHER"
    );
    private static final Set<String> ALLOWED_SALARY_PAYMENT_MODES = Set.of(
            "BANK_TRANSFER",
            "CHEQUE",
            "CASH"
    );
    private static final Set<Borrower.EmploymentType> ALLOWED_EMPLOYMENT_TYPES = EnumSet.of(
            Borrower.EmploymentType.SALARIED,
            Borrower.EmploymentType.GOVERNMENT,
            Borrower.EmploymentType.SELF_EMPLOYED,
            Borrower.EmploymentType.PROFESSIONAL,
            Borrower.EmploymentType.RETIRED,
            Borrower.EmploymentType.OTHER
    );

    private static final Map<Loan.LoanStatus, Set<Loan.LoanStatus>> ADMIN_TRANSITIONS = Map.of(
            Loan.LoanStatus.APPLIED, Set.of(Loan.LoanStatus.UNDER_ASSESSMENT),
            Loan.LoanStatus.UNDER_ASSESSMENT, Set.of(Loan.LoanStatus.APPROVED),
            Loan.LoanStatus.APPROVED, Set.of(Loan.LoanStatus.DISBURSED)
    );

    private final BorrowerRepository borrowerRepository;
    private final BorrowerDocumentRepository borrowerDocumentRepository;
    private final DocumentStorageService documentStorageService;
    private final LoanRepository loanRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final RiskEvaluationService riskEvaluationService;

    @Transactional
    public LoanDtos.LoanResponse applyForLoan(
            UUID tenantId,
            UUID userId,
            LoanDtos.ApplyLoanApplicationRequest request
    ) {
        Borrower borrower = borrowerRepository.findByTenant_IdAndUser_Id(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower", userId.toString()));

        validateLoanApplicationEligibility(borrower);
        validateLoanApplicationRequest(request);

        BigDecimal rentExpense = scaleMoney(nonNegativeOrZero(request.getRentExpense()));
        BigDecimal existingLoanEmis = scaleMoney(nonNegativeOrZero(request.getExistingLoanEmis()));
        BigDecimal creditCardPayments = scaleMoney(nonNegativeOrZero(request.getCreditCardPayments()));
        BigDecimal otherFixedExpenses = scaleMoney(nonNegativeOrZero(request.getOtherFixedExpenses()));
        BigDecimal totalMonthlyObligations = rentExpense
                .add(existingLoanEmis)
                .add(creditCardPayments)
                .add(otherFixedExpenses);

        borrower.setMonthlyIncome(scaleMoney(request.getMonthlyIncome()));
        borrower.setEmploymentType(request.getEmploymentType());
        borrower.setEmployerName(normalize(request.getEmployerName()));
        borrower.setIndustryType(normalizeEnumLike(request.getIndustryType()));
        borrower.setSalaryPaymentMode(normalizeEnumLike(request.getSalaryPaymentMode()));
        borrower.setYearsInCurrentJob(scaleOneDecimal(request.getYearsInCurrentJob()));
        borrower.setTotalWorkExperience(scaleOneDecimal(request.getTotalWorkExperience()));
        borrower.setRentExpense(rentExpense);
        borrower.setExistingLoanEmis(existingLoanEmis);
        borrower.setCreditCardPayments(creditCardPayments);
        borrower.setOtherFixedExpenses(otherFixedExpenses);
        borrower.setExistingMonthlyObligations(totalMonthlyObligations);
        borrower.setResidenceType(request.getResidenceType());
        borrower.setYearsAtCurrentResidence(
                request.getYearsAtCurrentResidence() == null ? null : scaleOneDecimal(request.getYearsAtCurrentResidence())
        );
        borrower.setCibilScore(request.getCibilScore());

        borrowerRepository.save(borrower);

        Loan loan = Loan.builder()
                .tenant(borrower.getTenant())
                .borrower(borrower)
                .loanAmount(scaleMoney(request.getLoanAmount()))
                .principalAmount(scaleMoney(request.getLoanAmount()))
                .annualInterestRate(ANNUAL_INTEREST_RATE)
                .tenureMonths(request.getTenureMonths())
                .loanPurpose(request.getLoanPurpose())
                .status(Loan.LoanStatus.APPLIED)
                .appliedAt(LocalDateTime.now())
                .build();

        Loan saved = loanRepository.save(loan);
        log.info("Loan application submitted: loanId={}, borrowerId={}, tenantId={}",
                saved.getId(), borrower.getId(), tenantId);
        return mapToLoanResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LoanDtos.LoanListItemResponse> getMyLoans(UUID tenantId, UUID userId) {
        Borrower borrower = borrowerRepository.findByTenant_IdAndUser_Id(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower", userId.toString()));

        return loanRepository.findAllByTenant_IdAndBorrower_IdAndDeletedAtIsNullOrderByAppliedAtDescCreatedAtDesc(
                        tenantId,
                        borrower.getId()
                )
                .stream()
                .map(this::mapToLoanListItemResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LoanDtos.LoanDetailResponse getMyLoanById(UUID tenantId, UUID userId, UUID loanId) {
        Borrower borrower = borrowerRepository.findByTenant_IdAndUser_Id(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower", userId.toString()));

        Loan loan = loanRepository.findByIdAndTenant_IdAndBorrower_Id(loanId, tenantId, borrower.getId())
                .orElseThrow(() -> new BusinessException(
                        "LOAN_ACCESS_DENIED",
                        "Loan not found for the current borrower"
                ));
        return mapToLoanDetailResponse(loan);
    }

    @Transactional(readOnly = true)
    public List<LoanDtos.LoanListItemResponse> listTenantLoans(UUID tenantId, String status) {
        List<Loan> loans;
        if (status == null || status.isBlank()) {
            loans = loanRepository.findAllByTenant_IdAndDeletedAtIsNullOrderByAppliedAtDescCreatedAtDesc(tenantId);
        } else {
            Loan.LoanStatus parsed = parseLoanStatus(status);
            loans = loanRepository.findAllByTenant_IdAndStatusAndDeletedAtIsNullOrderByAppliedAtDescCreatedAtDesc(
                    tenantId,
                    parsed
            );
        }

        return loans.stream()
                .map(this::mapToLoanListItemResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LoanDtos.LoanDetailResponse getTenantLoanById(UUID tenantId, UUID loanId) {
        Loan loan = loanRepository.findByIdAndTenant_Id(loanId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId.toString()));
        return mapToLoanDetailResponse(loan);
    }

    @Transactional
    public LoanDtos.LoanDetailResponse updateLoanStatus(
            UUID tenantId,
            UUID loanId,
            LoanDtos.UpdateLoanStatusRequest request
    ) {
        Loan loan = loanRepository.findByIdAndTenant_Id(loanId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId.toString()));

        Loan.LoanStatus targetStatus = parseLoanStatus(request.getStatus());
        Loan.LoanStatus currentStatus = loan.getStatus();
        validateStatusTransition(currentStatus, targetStatus);

        loan.setStatus(targetStatus);
        loan.setStatusNotes(normalize(request.getNotes()));
        if (targetStatus == Loan.LoanStatus.DISBURSED) {
            loan.setDisbursementDate(LocalDate.now());
        }

        if (targetStatus == Loan.LoanStatus.UNDER_ASSESSMENT) {
            if (loan.getBorrower().getMonthlyIncome() == null
                    || loan.getBorrower().getMonthlyIncome().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(
                        "RISK_EVALUATION_DATA_MISSING",
                        "Borrower monthly income is required before starting assessment"
                );
            }
            riskEvaluationService.evaluateLoan(
                    loan,
                    loan.getBorrower().getMonthlyIncome(),
                    loan.getBorrower().getExistingMonthlyObligations() == null
                            ? BigDecimal.ZERO
                            : loan.getBorrower().getExistingMonthlyObligations()
            );
        }

        Loan updated = loanRepository.save(loan);
        log.info("Loan status updated: loanId={}, tenantId={}, from={}, to={}",
                loanId, tenantId, currentStatus, targetStatus);
        return mapToLoanDetailResponse(updated);
    }

    @Transactional
    public LoanDtos.LoanDetailResponse rejectLoan(
            UUID tenantId,
            UUID loanId,
            String reason,
            User adminUser
    ) {
        String normalizedReason = normalize(reason);
        if (normalizedReason == null) {
            throw new BusinessException("REJECTION_REASON_REQUIRED", "Rejection reason is required");
        }

        Loan loan = loanRepository.findByIdAndTenant_Id(loanId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId.toString()));

        if (loan.getDeletedAt() != null) {
            throw new BusinessException("LOAN_ALREADY_DELETED", "Loan application is already deleted");
        }

        if (loan.getStatus() != Loan.LoanStatus.APPLIED && loan.getStatus() != Loan.LoanStatus.UNDER_ASSESSMENT) {
            throw new BusinessException(
                    "INVALID_LOAN_REJECTION_STATE",
                    "Loan can only be rejected when in APPLIED or UNDER_ASSESSMENT status"
            );
        }

        List<BorrowerDocument> documents = borrowerDocumentRepository.findByBorrower(loan.getBorrower());
        for (BorrowerDocument document : documents) {
            String storagePath = resolveStoragePath(document);
            if (storagePath != null) {
                documentStorageService.deleteDocument(storagePath);
            }
        }
        borrowerDocumentRepository.deleteAll(documents);

        loan.setStatus(Loan.LoanStatus.REJECTED);
        loan.setRejectionMessage(normalizedReason);
        loan.setRejectedAt(LocalDateTime.now());
        loan.setRejectedBy(adminUser);
        loan.setDeletedAt(null);
        loan.setDeletedBy(null);

        Loan saved = loanRepository.save(loan);
        log.info("Loan rejected with cleanup: loanId={}, tenantId={}, deletedDocuments={}",
                loanId, tenantId, documents.size());
        return mapToLoanDetailResponse(saved);
    }

    @Transactional
    public LoanDtos.LoanDetailResponse deleteLoanApplication(
            UUID tenantId,
            UUID loanId,
            String finalMessage,
            User adminUser
    ) {
        Loan loan = loanRepository.findByIdAndTenant_Id(loanId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId.toString()));

        if (loan.getDeletedAt() != null) {
            throw new BusinessException("LOAN_ALREADY_DELETED", "Loan application has already been deleted");
        }

        if (loan.getStatus() != Loan.LoanStatus.REJECTED) {
            throw new BusinessException(
                    "LOAN_DELETE_NOT_ALLOWED",
                    "Only rejected applications can be deleted. Please reject first."
            );
        }

        loan.setDeletedAt(LocalDateTime.now());
        loan.setDeletedBy(adminUser.getFullName());
        String normalizedFinalMessage = normalize(finalMessage);
        if (normalizedFinalMessage != null) {
            loan.setRejectionMessage(normalizedFinalMessage);
        }

        Loan saved = loanRepository.save(loan);
        log.info("Loan soft-deleted: loanId={}, tenantId={}, deletedBy={}",
                loanId, tenantId, adminUser.getId());
        return mapToLoanDetailResponse(saved);
    }

    private void validateLoanApplicationEligibility(Borrower borrower) {
        if (borrower.getStatus() == Borrower.BorrowerStatus.UNDER_REVIEW
                || borrower.getStatus() == Borrower.BorrowerStatus.VERIFIED) {
            return;
        }
        throw new BusinessException(
                "LOAN_APPLICATION_NOT_ALLOWED",
                "Loan application is allowed only when borrower status is UNDER_REVIEW or VERIFIED"
        );
    }

    private void validateLoanApplicationRequest(LoanDtos.ApplyLoanApplicationRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (request.getMonthlyIncome() == null) {
            errors.put("monthlyIncome", "Monthly income is required");
        } else {
            validateRange(request.getMonthlyIncome(), MIN_MONTHLY_INCOME, MAX_MONTHLY_INCOME,
                    "monthlyIncome", "Monthly income must be between 10000 and 10000000", errors);
        }

        if (request.getEmploymentType() == null) {
            errors.put("employmentType", "Employment type is required");
        } else if (!ALLOWED_EMPLOYMENT_TYPES.contains(request.getEmploymentType())) {
            errors.put("employmentType", "Invalid employment type");
        }

        String employerName = normalize(request.getEmployerName());
        if (employerName == null || employerName.length() < 2) {
            errors.put("employerName", "Employer or company name is required");
        } else if (employerName.length() > 255) {
            errors.put("employerName", "Employer or company name cannot exceed 255 characters");
        }

        String industryType = normalizeEnumLike(request.getIndustryType());
        if (industryType == null) {
            errors.put("industryType", "Industry type is required");
        } else if (!ALLOWED_INDUSTRY_TYPES.contains(industryType)) {
            errors.put("industryType", "Invalid industry type");
        }

        String salaryPaymentMode = normalizeEnumLike(request.getSalaryPaymentMode());
        if (salaryPaymentMode == null) {
            errors.put("salaryPaymentMode", "Salary payment mode is required");
        } else if (!ALLOWED_SALARY_PAYMENT_MODES.contains(salaryPaymentMode)) {
            errors.put("salaryPaymentMode", "Invalid salary payment mode");
        }

        if (request.getYearsInCurrentJob() == null) {
            errors.put("yearsInCurrentJob", "Years in current job is required");
        } else {
            validateRange(request.getYearsInCurrentJob(), BigDecimal.ZERO, new BigDecimal("50"),
                    "yearsInCurrentJob", "Years in current job must be between 0 and 50", errors);
            if (!hasMaxOneDecimal(request.getYearsInCurrentJob())) {
                errors.put("yearsInCurrentJob", "Years in current job can have at most 1 decimal place");
            }
        }

        if (request.getTotalWorkExperience() == null) {
            errors.put("totalWorkExperience", "Total work experience is required");
        } else {
            validateRange(request.getTotalWorkExperience(), BigDecimal.ZERO, new BigDecimal("60"),
                    "totalWorkExperience", "Total work experience must be between 0 and 60", errors);
            if (!hasMaxOneDecimal(request.getTotalWorkExperience())) {
                errors.put("totalWorkExperience", "Total work experience can have at most 1 decimal place");
            }
            if (request.getYearsInCurrentJob() != null
                    && request.getTotalWorkExperience().compareTo(request.getYearsInCurrentJob()) < 0) {
                errors.put("totalWorkExperience", "Total work experience must be greater than or equal to years in current job");
            }
        }

        if (request.getRentExpense() == null) {
            errors.put("rentExpense", "Rent or mortgage amount is required");
        } else if (request.getRentExpense().compareTo(BigDecimal.ZERO) < 0) {
            errors.put("rentExpense", "Rent or mortgage amount cannot be negative");
        }

        if (request.getExistingLoanEmis() == null) {
            errors.put("existingLoanEmis", "Existing loan EMIs amount is required");
        } else if (request.getExistingLoanEmis().compareTo(BigDecimal.ZERO) < 0) {
            errors.put("existingLoanEmis", "Existing loan EMIs amount cannot be negative");
        }

        if (request.getCreditCardPayments() == null) {
            errors.put("creditCardPayments", "Credit card payments amount is required");
        } else if (request.getCreditCardPayments().compareTo(BigDecimal.ZERO) < 0) {
            errors.put("creditCardPayments", "Credit card payments amount cannot be negative");
        }

        if (request.getOtherFixedExpenses() == null) {
            errors.put("otherFixedExpenses", "Other fixed expenses amount is required");
        } else if (request.getOtherFixedExpenses().compareTo(BigDecimal.ZERO) < 0) {
            errors.put("otherFixedExpenses", "Other fixed expenses amount cannot be negative");
        }

        if (!errors.containsKey("rentExpense")
                && !errors.containsKey("existingLoanEmis")
                && !errors.containsKey("creditCardPayments")
                && !errors.containsKey("otherFixedExpenses")
                && request.getMonthlyIncome() != null) {
            BigDecimal totalMonthlyObligations = nonNegativeOrZero(request.getRentExpense())
                    .add(nonNegativeOrZero(request.getExistingLoanEmis()))
                    .add(nonNegativeOrZero(request.getCreditCardPayments()))
                    .add(nonNegativeOrZero(request.getOtherFixedExpenses()));
            if (totalMonthlyObligations.compareTo(request.getMonthlyIncome()) > 0) {
                errors.put("existingLoanEmis", "Total monthly obligations cannot exceed monthly income");
            }
        }

        if (request.getLoanAmount() == null) {
            errors.put("loanAmount", "Loan amount is required");
        } else {
            validateRange(request.getLoanAmount(), MIN_LOAN_AMOUNT, MAX_LOAN_AMOUNT,
                    "loanAmount", "Loan amount must be between 5000 and 5000000", errors);
        }

        if (request.getTenureMonths() == null) {
            errors.put("tenureMonths", "Tenure is required");
        } else if (!ALLOWED_TENURES.contains(request.getTenureMonths())) {
            errors.put("tenureMonths", "Tenure must be one of 3, 6, 12, 18, 24, 36, 48, 60");
        }

        if (request.getLoanPurpose() == null) {
            errors.put("loanPurpose", "Loan purpose is required");
        }

        if (request.getYearsAtCurrentResidence() != null) {
            validateRange(request.getYearsAtCurrentResidence(), BigDecimal.ZERO, new BigDecimal("50"),
                    "yearsAtCurrentResidence", "Years at current residence must be between 0 and 50", errors);
            if (!hasMaxOneDecimal(request.getYearsAtCurrentResidence())) {
                errors.put("yearsAtCurrentResidence", "Years at current residence can have at most 1 decimal place");
            }
        }

        if (request.getCibilScore() != null
                && (request.getCibilScore() < 300 || request.getCibilScore() > 900)) {
            errors.put("cibilScore", "CIBIL score must be between 300 and 900");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Loan application validation failed", errors);
        }
    }

    private void validateRange(
            BigDecimal value,
            BigDecimal min,
            BigDecimal max,
            String field,
            String message,
            Map<String, String> errors
    ) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            errors.put(field, message);
        }
    }

    private boolean hasMaxOneDecimal(BigDecimal value) {
        return value.stripTrailingZeros().scale() <= 1;
    }

    private Loan.LoanStatus parseLoanStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            throw new BusinessException("INVALID_LOAN_STATUS", "Loan status is required");
        }
        try {
            return Loan.LoanStatus.valueOf(rawStatus.trim().toUpperCase(Locale.ENGLISH));
        } catch (Exception ex) {
            throw new BusinessException("INVALID_LOAN_STATUS", "Invalid loan status: " + rawStatus);
        }
    }

    private void validateStatusTransition(Loan.LoanStatus currentStatus, Loan.LoanStatus targetStatus) {
        if (targetStatus == Loan.LoanStatus.REJECTED) {
            throw new BusinessException(
                    "REJECT_REQUIRES_WORKFLOW",
                    "Use the reject endpoint to reject applications and clean up uploaded documents"
            );
        }
        Set<Loan.LoanStatus> allowed = ADMIN_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(targetStatus)) {
            throw new BusinessException(
                    "INVALID_LOAN_STATUS_TRANSITION",
                    String.format("Status transition not allowed: %s -> %s", currentStatus, targetStatus)
            );
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeEnumLike(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ENGLISH).replace(' ', '_');
    }

    private BigDecimal nonNegativeOrZero(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return value;
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleOneDecimal(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP);
    }

    private String resolveStoragePath(BorrowerDocument document) {
        String explicitPath = normalize(document.getStoragePath());
        if (explicitPath != null) {
            return explicitPath;
        }

        String documentUrl = normalize(document.getDocumentUrl());
        if (documentUrl == null) {
            return null;
        }
        String marker = "/storage/v1/object/public/borrower-documents/";
        int markerIndex = documentUrl.indexOf(marker);
        if (markerIndex < 0) {
            return null;
        }
        return documentUrl.substring(markerIndex + marker.length());
    }

    private LoanDtos.LoanResponse mapToLoanResponse(Loan loan) {
        return LoanDtos.LoanResponse.builder()
                .id(loan.getId())
                .borrowerId(loan.getBorrower().getId())
                .tenantId(loan.getTenant().getId())
                .loanAmount(loan.getLoanAmount())
                .tenureMonths(loan.getTenureMonths())
                .loanPurpose(loan.getLoanPurpose())
                .status(loan.getStatus())
                .annualInterestRate(loan.getAnnualInterestRate())
                .appliedAt(loan.getAppliedAt())
                .disbursementDate(loan.getDisbursementDate())
                .createdAt(loan.getCreatedAt())
                .updatedAt(loan.getUpdatedAt())
                .build();
    }

    private LoanDtos.LoanListItemResponse mapToLoanListItemResponse(Loan loan) {
        return LoanDtos.LoanListItemResponse.builder()
                .id(loan.getId())
                .borrowerId(loan.getBorrower().getId())
                .borrowerName(loan.getBorrower().getFirstName() + " " + loan.getBorrower().getLastName())
                .loanAmount(loan.getLoanAmount())
                .tenureMonths(loan.getTenureMonths())
                .loanPurpose(loan.getLoanPurpose())
                .status(loan.getStatus())
                .appliedAt(loan.getAppliedAt())
                .build();
    }

    private LoanDtos.LoanDetailResponse mapToLoanDetailResponse(Loan loan) {
        Borrower borrower = loan.getBorrower();
        return LoanDtos.LoanDetailResponse.builder()
                .id(loan.getId())
                .borrowerId(borrower.getId())
                .tenantId(loan.getTenant().getId())
                .loanAmount(loan.getLoanAmount())
                .tenureMonths(loan.getTenureMonths())
                .loanPurpose(loan.getLoanPurpose())
                .status(loan.getStatus())
                .annualInterestRate(loan.getAnnualInterestRate())
                .appliedAt(loan.getAppliedAt())
                .disbursementDate(loan.getDisbursementDate())
                .statusNotes(loan.getStatusNotes())
                .rejectionMessage(loan.getRejectionMessage())
                .rejectedAt(loan.getRejectedAt())
                .rejectedByName(loan.getRejectedBy() == null ? null : loan.getRejectedBy().getFullName())
                .deletedAt(loan.getDeletedAt())
                .deletedBy(loan.getDeletedBy())
                .isDeleted(loan.getDeletedAt() != null)
                .borrower(LoanDtos.BorrowerSnapshot.builder()
                        .id(borrower.getId())
                        .fullName(borrower.getFirstName() + " " + borrower.getLastName())
                        .email(borrower.getEmail())
                        .phone(borrower.getPhone())
                        .status(borrower.getStatus())
                        .panNumber(borrower.getPanNumber())
                        .aadhaarNumber(borrower.getAadhaarNumber())
                        .monthlyIncome(borrower.getMonthlyIncome())
                        .employmentType(borrower.getEmploymentType())
                        .employerName(borrower.getEmployerName())
                        .industryType(borrower.getIndustryType())
                        .salaryPaymentMode(borrower.getSalaryPaymentMode())
                        .yearsInCurrentJob(borrower.getYearsInCurrentJob())
                        .totalWorkExperience(borrower.getTotalWorkExperience())
                        .rentExpense(borrower.getRentExpense())
                        .existingLoanEmis(borrower.getExistingLoanEmis())
                        .creditCardPayments(borrower.getCreditCardPayments())
                        .otherFixedExpenses(borrower.getOtherFixedExpenses())
                        .existingMonthlyObligations(borrower.getExistingMonthlyObligations())
                        .residenceType(borrower.getResidenceType())
                        .yearsAtCurrentResidence(borrower.getYearsAtCurrentResidence())
                        .cibilScore(borrower.getCibilScore())
                        .documents(mapBorrowerDocuments(borrower))
                        .build())
                .risk(mapRiskSnapshot(loan.getId()))
                .createdAt(loan.getCreatedAt())
                .updatedAt(loan.getUpdatedAt())
                .build();
    }

    private List<LoanDtos.DocumentSnapshot> mapBorrowerDocuments(Borrower borrower) {
        return borrowerDocumentRepository.findByBorrowerOrderByCreatedAtDesc(borrower)
                .stream()
                .map(this::mapDocumentSnapshot)
                .toList();
    }

    private LoanDtos.DocumentSnapshot mapDocumentSnapshot(BorrowerDocument document) {
        return LoanDtos.DocumentSnapshot.builder()
                .id(document.getId())
                .documentType(document.getDocumentType().name())
                .documentUrl(document.getDocumentUrl())
                .verificationStatus(document.getVerificationStatus().name())
                .uploadedAt(document.getCreatedAt())
                .build();
    }

    private LoanDtos.RiskSnapshot mapRiskSnapshot(UUID loanId) {
        return riskAssessmentRepository.findByLoan_Id(loanId)
                .map(this::toRiskSnapshot)
                .orElse(null);
    }

    private LoanDtos.RiskSnapshot toRiskSnapshot(RiskAssessment riskAssessment) {
        return LoanDtos.RiskSnapshot.builder()
                .riskScore(riskAssessment.getRiskScore())
                .foir(riskAssessment.getFoir())
                .recommendation(toRecommendation(riskAssessment.getDecision()))
                .assessedAt(riskAssessment.getUpdatedAt())
                .build();
    }

    private String toRecommendation(RiskAssessment.RiskDecision decision) {
        return switch (decision) {
            case APPROVED -> "APPROVE";
            case NEEDS_REVIEW -> "APPROVE_WITH_CAUTION";
            case REJECTED -> "REJECT";
        };
    }
}
