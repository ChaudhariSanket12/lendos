package com.lendos.loan.service;

import com.lendos.borrower.entity.Borrower;
import com.lendos.borrower.repository.BorrowerRepository;
import com.lendos.common.exception.BusinessException;
import com.lendos.common.exception.ResourceNotFoundException;
import com.lendos.common.exception.ValidationException;
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
    private static final Set<Borrower.EmploymentType> ALLOWED_EMPLOYMENT_TYPES = EnumSet.of(
            Borrower.EmploymentType.SALARIED,
            Borrower.EmploymentType.GOVERNMENT,
            Borrower.EmploymentType.SELF_EMPLOYED,
            Borrower.EmploymentType.PROFESSIONAL,
            Borrower.EmploymentType.RETIRED,
            Borrower.EmploymentType.OTHER
    );

    private static final Map<Loan.LoanStatus, Set<Loan.LoanStatus>> ADMIN_TRANSITIONS = Map.of(
            Loan.LoanStatus.APPLIED, Set.of(Loan.LoanStatus.UNDER_ASSESSMENT, Loan.LoanStatus.REJECTED),
            Loan.LoanStatus.UNDER_ASSESSMENT, Set.of(Loan.LoanStatus.APPROVED, Loan.LoanStatus.REJECTED),
            Loan.LoanStatus.APPROVED, Set.of(Loan.LoanStatus.DISBURSED)
    );

    private final BorrowerRepository borrowerRepository;
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

        borrower.setMonthlyIncome(scaleMoney(request.getMonthlyIncome()));
        borrower.setEmploymentType(request.getEmploymentType());
        borrower.setYearsInCurrentJob(scaleOneDecimal(request.getYearsInCurrentJob()));
        borrower.setTotalWorkExperience(scaleOneDecimal(request.getTotalWorkExperience()));
        borrower.setExistingMonthlyObligations(scaleMoney(request.getExistingMonthlyObligations()));
        borrower.setResidenceType(request.getResidenceType());
        borrower.setYearsAtCurrentResidence(scaleOneDecimal(request.getYearsAtCurrentResidence()));
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

        return loanRepository.findAllByTenant_IdAndBorrower_IdOrderByAppliedAtDescCreatedAtDesc(tenantId, borrower.getId())
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
            loans = loanRepository.findAllByTenant_IdOrderByAppliedAtDescCreatedAtDesc(tenantId);
        } else {
            Loan.LoanStatus parsed = parseLoanStatus(status);
            loans = loanRepository.findAllByTenant_IdAndStatusOrderByAppliedAtDescCreatedAtDesc(tenantId, parsed);
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

        if (request.getExistingMonthlyObligations() == null) {
            errors.put("existingMonthlyObligations", "Existing monthly obligations is required");
        } else {
            if (request.getExistingMonthlyObligations().compareTo(BigDecimal.ZERO) < 0) {
                errors.put("existingMonthlyObligations", "Existing monthly obligations cannot be negative");
            } else if (request.getMonthlyIncome() != null
                    && request.getExistingMonthlyObligations().compareTo(request.getMonthlyIncome()) > 0) {
                errors.put("existingMonthlyObligations", "Existing monthly obligations cannot exceed monthly income");
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

        if (request.getResidenceType() == null) {
            errors.put("residenceType", "Residence type is required");
        }

        if (request.getYearsAtCurrentResidence() == null) {
            errors.put("yearsAtCurrentResidence", "Years at current residence is required");
        } else {
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

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleOneDecimal(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP);
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
                        .yearsInCurrentJob(borrower.getYearsInCurrentJob())
                        .totalWorkExperience(borrower.getTotalWorkExperience())
                        .existingMonthlyObligations(borrower.getExistingMonthlyObligations())
                        .residenceType(borrower.getResidenceType())
                        .yearsAtCurrentResidence(borrower.getYearsAtCurrentResidence())
                        .cibilScore(borrower.getCibilScore())
                        .build())
                .risk(mapRiskSnapshot(loan.getId()))
                .createdAt(loan.getCreatedAt())
                .updatedAt(loan.getUpdatedAt())
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
