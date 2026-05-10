package com.lendos.loan;

import com.lendos.borrower.entity.Borrower;
import com.lendos.borrower.repository.BorrowerRepository;
import com.lendos.common.exception.BusinessException;
import com.lendos.loan.dto.LoanDtos;
import com.lendos.loan.entity.Loan;
import com.lendos.loan.repository.LoanRepository;
import com.lendos.loan.service.LoanService;
import com.lendos.risk.repository.RiskAssessmentRepository;
import com.lendos.risk.service.RiskEvaluationService;
import com.lendos.identity.entity.Tenant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanService Unit Tests")
class LoanServiceTest {

    @Mock private BorrowerRepository borrowerRepository;
    @Mock private LoanRepository loanRepository;
    @Mock private RiskAssessmentRepository riskAssessmentRepository;
    @Mock private RiskEvaluationService riskEvaluationService;

    @InjectMocks
    private LoanService loanService;

    @Test
    @DisplayName("Apply loan stores borrower financials and creates APPLIED loan")
    void applyForLoan_createsAppliedLoan() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Tenant tenant = Tenant.builder().name("Demo").slug("demo").contactEmail("admin@demo.com").status(Tenant.TenantStatus.ACTIVE).build();
        tenant.setId(tenantId);

        Borrower borrower = Borrower.builder()
                .tenant(tenant)
                .firstName("Riya")
                .lastName("Shah")
                .email("riya@test.com")
                .status(Borrower.BorrowerStatus.UNDER_REVIEW)
                .build();
        borrower.setId(UUID.randomUUID());

        when(borrowerRepository.findByTenant_IdAndUser_Id(tenantId, userId)).thenReturn(Optional.of(borrower));
        when(borrowerRepository.save(any(Borrower.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        LoanDtos.ApplyLoanApplicationRequest request = new LoanDtos.ApplyLoanApplicationRequest();
        request.setMonthlyIncome(new BigDecimal("85000"));
        request.setEmploymentType(Borrower.EmploymentType.SALARIED);
        request.setYearsInCurrentJob(new BigDecimal("4.5"));
        request.setTotalWorkExperience(new BigDecimal("9.0"));
        request.setExistingMonthlyObligations(new BigDecimal("12000"));
        request.setLoanAmount(new BigDecimal("500000"));
        request.setTenureMonths(36);
        request.setLoanPurpose(Loan.LoanPurpose.HOME_RENOVATION);
        request.setResidenceType(Borrower.ResidenceType.OWNED);
        request.setYearsAtCurrentResidence(new BigDecimal("6.0"));
        request.setCibilScore(780);

        LoanDtos.LoanResponse response = loanService.applyForLoan(tenantId, userId, request);

        assertThat(response.getStatus()).isEqualTo(Loan.LoanStatus.APPLIED);
        assertThat(response.getLoanAmount()).isEqualByComparingTo("500000.00");
        assertThat(response.getLoanPurpose()).isEqualTo(Loan.LoanPurpose.HOME_RENOVATION);
        assertThat(borrower.getMonthlyIncome()).isEqualByComparingTo("85000.00");
    }

    @Test
    @DisplayName("Apply loan is blocked for DRAFT borrower")
    void applyForLoan_draftBorrower_throwsBusinessException() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Borrower borrower = Borrower.builder()
                .status(Borrower.BorrowerStatus.DRAFT)
                .build();
        when(borrowerRepository.findByTenant_IdAndUser_Id(tenantId, userId)).thenReturn(Optional.of(borrower));

        LoanDtos.ApplyLoanApplicationRequest request = new LoanDtos.ApplyLoanApplicationRequest();
        request.setMonthlyIncome(new BigDecimal("85000"));

        assertThatThrownBy(() -> loanService.applyForLoan(tenantId, userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("UNDER_REVIEW or VERIFIED");
    }

    @Test
    @DisplayName("Update status to UNDER_ASSESSMENT runs risk evaluation")
    void updateLoanStatus_underAssessment_runsRiskEvaluation() {
        UUID tenantId = UUID.randomUUID();
        UUID loanId = UUID.randomUUID();

        Tenant tenant = Tenant.builder().name("Demo").slug("demo").contactEmail("admin@demo.com").status(Tenant.TenantStatus.ACTIVE).build();
        tenant.setId(tenantId);

        Borrower borrower = Borrower.builder()
                .tenant(tenant)
                .firstName("Riya")
                .lastName("Shah")
                .monthlyIncome(new BigDecimal("85000"))
                .existingMonthlyObligations(new BigDecimal("12000"))
                .status(Borrower.BorrowerStatus.VERIFIED)
                .build();
        borrower.setId(UUID.randomUUID());

        Loan loan = Loan.builder()
                .tenant(tenant)
                .borrower(borrower)
                .loanAmount(new BigDecimal("500000"))
                .principalAmount(new BigDecimal("500000"))
                .annualInterestRate(new BigDecimal("12.00"))
                .tenureMonths(36)
                .loanPurpose(Loan.LoanPurpose.HOME_RENOVATION)
                .status(Loan.LoanStatus.APPLIED)
                .build();
        loan.setId(loanId);

        when(loanRepository.findByIdAndTenant_Id(loanId, tenantId)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(riskAssessmentRepository.findByLoan_Id(loanId)).thenReturn(Optional.empty());
        when(riskEvaluationService.evaluateLoan(any(Loan.class), any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(RiskEvaluationService.RiskEvaluationResult.builder()
                        .riskScore(new BigDecimal("72.00"))
                        .foir(new BigDecimal("37.60"))
                        .recommendation("APPROVE")
                        .estimatedEmi(new BigDecimal("16500"))
                        .build());

        LoanDtos.UpdateLoanStatusRequest request = new LoanDtos.UpdateLoanStatusRequest();
        request.setStatus("UNDER_ASSESSMENT");
        request.setNotes("Starting credit review");

        LoanDtos.LoanDetailResponse response = loanService.updateLoanStatus(tenantId, loanId, request);

        assertThat(response.getStatus()).isEqualTo(Loan.LoanStatus.UNDER_ASSESSMENT);
        verify(riskEvaluationService).evaluateLoan(any(Loan.class), any(BigDecimal.class), any(BigDecimal.class));
    }
}
