package com.lendos.borrower;

import com.lendos.borrower.dto.BorrowerDtos;
import com.lendos.borrower.entity.Borrower;
import com.lendos.borrower.repository.BorrowerRepository;
import com.lendos.borrower.service.BorrowerPortalService;
import com.lendos.common.exception.BusinessException;
import com.lendos.identity.dto.BorrowerAuthDtos;
import com.lendos.identity.entity.RefreshToken;
import com.lendos.identity.entity.Tenant;
import com.lendos.identity.entity.User;
import com.lendos.identity.repository.TenantRepository;
import com.lendos.identity.repository.UserRepository;
import com.lendos.identity.security.JwtService;
import com.lendos.identity.service.RefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BorrowerPortalService Unit Tests")
class BorrowerPortalServiceTest {

    @Mock private BorrowerRepository borrowerRepository;
    @Mock private UserRepository userRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks
    private BorrowerPortalService borrowerPortalService;

    @Test
    @DisplayName("Register borrower creates BORROWER user and DRAFT borrower profile")
    void registerBorrower_createsUserAndBorrowerProfile() {
        Tenant tenant = Tenant.builder()
                .name("Default Tenant")
                .slug("default-tenant")
                .firmCode("DEMO-LEN-AB12")
                .contactEmail("admin@default.com")
                .status(Tenant.TenantStatus.ACTIVE)
                .build();
        tenant.setId(UUID.randomUUID());

        BorrowerAuthDtos.RegisterBorrowerRequest request = new BorrowerAuthDtos.RegisterBorrowerRequest();
        request.setFirmCode("DEMO-LEN-AB12");
        request.setFullName("Aarav Mehta");
        request.setEmail("aarav@example.com");
        request.setPassword("Borrower@123");

        when(tenantRepository.findByFirmCodeIgnoreCase("DEMO-LEN-AB12")).thenReturn(Optional.of(tenant));
        when(userRepository.existsByTenant_IdAndEmailIgnoreCase(tenant.getId(), "aarav@example.com")).thenReturn(false);
        when(borrowerRepository.existsByTenant_IdAndEmailIgnoreCase(tenant.getId(), "aarav@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Borrower@123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(borrowerRepository.save(any(Borrower.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(any(), any())).thenReturn("mock.jwt.token");
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(
                RefreshToken.builder()
                        .token("refresh-token")
                        .expiryDate(Instant.now().plusSeconds(3600))
                        .revoked(false)
                        .build()
        );

        BorrowerAuthDtos.BorrowerAuthResponse response = borrowerPortalService.registerBorrower(request);

        assertThat(response.getAccessToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getUser().getRole()).isEqualTo(User.Role.BORROWER);
        assertThat(response.getBorrower().getStatus()).isEqualTo(Borrower.BorrowerStatus.DRAFT);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(User.Role.BORROWER);
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("aarav@example.com");
    }

    @Test
    @DisplayName("Register borrower fails for invalid firm code")
    void registerBorrower_invalidFirmCode_throwsBusinessException() {
        BorrowerAuthDtos.RegisterBorrowerRequest request = new BorrowerAuthDtos.RegisterBorrowerRequest();
        request.setFirmCode("WRONG-CODE");
        request.setFullName("Borrower User");
        request.setEmail("borrower@test.com");
        request.setPassword("Borrower@123");

        when(tenantRepository.findByFirmCodeIgnoreCase("WRONG-CODE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> borrowerPortalService.registerBorrower(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Invalid firm code. Please contact your lending institution.");
    }

    @Test
    @DisplayName("Complete profile moves borrower from DRAFT to UNDER_REVIEW")
    void completeBorrowerProfile_movesStatusToUnderReview() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Borrower borrower = Borrower.builder()
                .firstName("Aarav")
                .lastName("Mehta")
                .email("aarav@example.com")
                .status(Borrower.BorrowerStatus.DRAFT)
                .build();
        when(borrowerRepository.findByTenant_IdAndUser_Id(tenantId, userId)).thenReturn(Optional.of(borrower));
        when(borrowerRepository.save(any(Borrower.class))).thenAnswer(inv -> inv.getArgument(0));

        BorrowerDtos.CompleteBorrowerProfileRequest request = new BorrowerDtos.CompleteBorrowerProfileRequest();
        request.setPhone("+91-9876543210");
        request.setDateOfBirth(LocalDate.now().minusYears(25));
        request.setAddress("42 Residency Road, Bengaluru");
        request.setMonthlyIncome(new BigDecimal("85000.00"));
        request.setEmploymentType(Borrower.EmploymentType.SALARIED);
        request.setYearsInCurrentJob(new BigDecimal("4.5"));
        request.setExistingMonthlyObligations(new BigDecimal("12500.00"));
        request.setPanNumber("ABCDE1234F");

        BorrowerDtos.BorrowerProfileResponse response = borrowerPortalService.completeMyBorrowerProfile(
                tenantId, userId, request
        );

        assertThat(response.getStatus()).isEqualTo(Borrower.BorrowerStatus.UNDER_REVIEW);
        assertThat(response.getPhone()).isEqualTo("919876543210");
        assertThat(response.getEmploymentType()).isEqualTo(Borrower.EmploymentType.SALARIED);
    }
}
