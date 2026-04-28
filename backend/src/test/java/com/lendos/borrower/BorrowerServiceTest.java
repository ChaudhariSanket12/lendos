package com.lendos.borrower;

import com.lendos.borrower.dto.BorrowerDtos;
import com.lendos.borrower.entity.Borrower;
import com.lendos.borrower.repository.BorrowerRepository;
import com.lendos.borrower.service.BorrowerService;
import com.lendos.common.exception.BusinessException;
import com.lendos.common.exception.ResourceNotFoundException;
import com.lendos.common.exception.ValidationException;
import com.lendos.identity.entity.Tenant;
import com.lendos.identity.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BorrowerService Unit Tests")
class BorrowerServiceTest {

    @Mock private BorrowerRepository borrowerRepository;
    @Mock private TenantRepository tenantRepository;

    @InjectMocks
    private BorrowerService borrowerService;

    private UUID tenantId;
    private UUID borrowerId;
    private Tenant tenant;
    private Borrower borrower;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        borrowerId = UUID.randomUUID();

        tenant = Tenant.builder()
                .name("Test Tenant")
                .slug("test-tenant")
                .contactEmail("admin@test.com")
                .status(Tenant.TenantStatus.ACTIVE)
                .build();

        borrower = Borrower.builder()
                .tenant(tenant)
                .firstName("Riya")
                .lastName("Shah")
                .email("riya@test.com")
                .phone("9999999999")
                .status(Borrower.BorrowerStatus.DRAFT)
                .build();
        borrower.setId(borrowerId);
    }

    @Test
    @DisplayName("Create borrower sets default status to DRAFT")
    void createBorrower_setsDefaultDraftStatus() {
        BorrowerDtos.CreateBorrowerRequest request = new BorrowerDtos.CreateBorrowerRequest();
        request.setFirstName("Aarav");
        request.setLastName("Jain");
        request.setEmail("aarav@test.com");
        request.setPhone("+91-9876543210");
        request.setDateOfBirth(LocalDate.of(1995, 1, 12));
        request.setAddress("123 Main Street");

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(borrowerRepository.existsByTenant_IdAndEmailIgnoreCase(tenantId, "aarav@test.com")).thenReturn(false);
        when(borrowerRepository.existsByTenant_IdAndPhone(tenantId, "919876543210")).thenReturn(false);
        when(borrowerRepository.save(any(Borrower.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BorrowerDtos.BorrowerResponse response = borrowerService.createBorrower(tenantId, request);

        assertThat(response.getStatus()).isEqualTo(Borrower.BorrowerStatus.DRAFT);
        assertThat(response.getEmail()).isEqualTo("aarav@test.com");
        assertThat(response.getPhone()).isEqualTo("919876543210");
    }

    @Test
    @DisplayName("List borrowers with status filter returns tenant-scoped results")
    void listBorrowers_withStatusFilter_returnsScopedResults() {
        borrower.setStatus(Borrower.BorrowerStatus.ACTIVE);
        when(borrowerRepository.findAllByTenant_IdAndStatusOrderByCreatedAtDesc(
                tenantId, Borrower.BorrowerStatus.ACTIVE)).thenReturn(List.of(borrower));

        List<BorrowerDtos.BorrowerResponse> result = borrowerService.listBorrowers(tenantId, "ACTIVE");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(Borrower.BorrowerStatus.ACTIVE);
    }

    @Test
    @DisplayName("Get borrower by ID returns 404 when borrower does not belong to tenant")
    void getBorrowerById_wrongTenant_throwsNotFound() {
        when(borrowerRepository.findByIdAndTenant_Id(borrowerId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> borrowerService.getBorrowerById(tenantId, borrowerId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Invalid status transition returns business error")
    void updateBorrowerStatus_invalidTransition_throwsBusinessException() {
        BorrowerDtos.UpdateBorrowerStatusRequest request = new BorrowerDtos.UpdateBorrowerStatusRequest();
        request.setStatus("ACTIVE");

        when(borrowerRepository.findByIdAndTenant_Id(borrowerId, tenantId)).thenReturn(Optional.of(borrower));

        assertThatThrownBy(() -> borrowerService.updateBorrowerStatus(tenantId, borrowerId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Status transition not allowed");

        verify(borrowerRepository, never()).save(any(Borrower.class));
    }

    @Test
    @DisplayName("Delete borrower is blocked when status is not DRAFT")
    void deleteBorrower_nonDraft_throwsBusinessException() {
        borrower.setStatus(Borrower.BorrowerStatus.VERIFIED);
        when(borrowerRepository.findByIdAndTenant_Id(borrowerId, tenantId)).thenReturn(Optional.of(borrower));

        assertThatThrownBy(() -> borrowerService.deleteBorrower(tenantId, borrowerId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("only when status is DRAFT");
    }

    @Test
    @DisplayName("Create borrower fails when email already exists in tenant (case-insensitive)")
    void createBorrower_duplicateEmail_throwsValidationException() {
        BorrowerDtos.CreateBorrowerRequest request = new BorrowerDtos.CreateBorrowerRequest();
        request.setFirstName("Aarav");
        request.setLastName("Jain");
        request.setEmail("AARAV@Test.com");
        request.setPhone("9876543210");
        request.setDateOfBirth(LocalDate.of(1995, 1, 12));
        request.setAddress("123 Main Street");

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(borrowerRepository.existsByTenant_IdAndEmailIgnoreCase(tenantId, "aarav@test.com")).thenReturn(true);
        when(borrowerRepository.existsByTenant_IdAndPhone(tenantId, "9876543210")).thenReturn(false);

        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> borrowerService.createBorrower(tenantId, request))
                .satisfies(ex -> assertThat(ex.getErrors().get("email"))
                        .isEqualTo("A borrower with this email already exists"));
    }

    @Test
    @DisplayName("Create borrower fails when phone format is invalid")
    void createBorrower_invalidPhone_throwsValidationException() {
        BorrowerDtos.CreateBorrowerRequest request = new BorrowerDtos.CreateBorrowerRequest();
        request.setFirstName("Aarav");
        request.setLastName("Jain");
        request.setEmail("aarav2@test.com");
        request.setPhone("12345");
        request.setDateOfBirth(LocalDate.of(1995, 1, 12));
        request.setAddress("123 Main Street");

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(borrowerRepository.existsByTenant_IdAndEmailIgnoreCase(tenantId, "aarav2@test.com")).thenReturn(false);

        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> borrowerService.createBorrower(tenantId, request))
                .satisfies(ex -> assertThat(ex.getErrors().get("phone"))
                        .isEqualTo("Invalid phone number format"));
    }

    @Test
    @DisplayName("Create borrower fails when date of birth is in the future")
    void createBorrower_futureDateOfBirth_throwsValidationException() {
        BorrowerDtos.CreateBorrowerRequest request = new BorrowerDtos.CreateBorrowerRequest();
        request.setFirstName("Aarav");
        request.setLastName("Jain");
        request.setEmail("aarav3@test.com");
        request.setPhone("9876543210");
        request.setDateOfBirth(LocalDate.now().plusDays(1));
        request.setAddress("123 Main Street");

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(borrowerRepository.existsByTenant_IdAndEmailIgnoreCase(tenantId, "aarav3@test.com")).thenReturn(false);
        when(borrowerRepository.existsByTenant_IdAndPhone(tenantId, "9876543210")).thenReturn(false);

        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> borrowerService.createBorrower(tenantId, request))
                .satisfies(ex -> assertThat(ex.getErrors().get("dateOfBirth"))
                        .isEqualTo("Date of birth cannot be in the future"));
    }

    @Test
    @DisplayName("Create borrower fails when borrower is below 18 years")
    void createBorrower_under18_throwsValidationException() {
        BorrowerDtos.CreateBorrowerRequest request = new BorrowerDtos.CreateBorrowerRequest();
        request.setFirstName("Aarav");
        request.setLastName("Jain");
        request.setEmail("aarav4@test.com");
        request.setPhone("9876543210");
        request.setDateOfBirth(LocalDate.now().minusYears(17));
        request.setAddress("123 Main Street");

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(borrowerRepository.existsByTenant_IdAndEmailIgnoreCase(tenantId, "aarav4@test.com")).thenReturn(false);
        when(borrowerRepository.existsByTenant_IdAndPhone(tenantId, "9876543210")).thenReturn(false);

        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> borrowerService.createBorrower(tenantId, request))
                .satisfies(ex -> assertThat(ex.getErrors().get("dateOfBirth"))
                        .isEqualTo("Borrower must be at least 18 years old"));
    }
}
