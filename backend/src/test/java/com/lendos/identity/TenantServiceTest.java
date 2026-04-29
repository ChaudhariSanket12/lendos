package com.lendos.identity;

import com.lendos.common.exception.BusinessException;
import com.lendos.identity.dto.IdentityDtos;
import com.lendos.identity.entity.Tenant;
import com.lendos.identity.entity.User;
import com.lendos.identity.repository.TenantRepository;
import com.lendos.identity.repository.UserRepository;
import com.lendos.identity.service.TenantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantService Unit Tests")
class TenantServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TenantService tenantService;

    @Test
    @DisplayName("Register tenant succeeds with valid data")
    void registerTenant_withValidData_createsTenantAndAdmin() {
        // Arrange
        IdentityDtos.RegisterTenantRequest request = new IdentityDtos.RegisterTenantRequest();
        request.setName("Alpha CA Firm");
        request.setContactEmail("admin@alphaca.com");
        request.setAdminFullName("Alpha Admin");
        request.setAdminPassword("securepass123");

        Tenant savedTenant = Tenant.builder()
                .name("Alpha CA Firm")
                .slug("alpha-ca-firm")
                .firmCode("ALPHACAF-AB12")
                .contactEmail("admin@alphaca.com")
                .status(Tenant.TenantStatus.ACTIVE)
                .build();

        // Use reflection to set id (BaseEntity)
        try {
            java.lang.reflect.Field idField = savedTenant.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(savedTenant, UUID.randomUUID());
        } catch (Exception ignored) {}

        when(tenantRepository.existsBySlug(anyString())).thenReturn(false);
        when(tenantRepository.existsByFirmCodeIgnoreCase(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        IdentityDtos.TenantResponse response = tenantService.registerTenant(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Alpha CA Firm");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User createdAdmin = userCaptor.getValue();
        assertThat(createdAdmin.getRole()).isEqualTo(User.Role.ADMIN);
        assertThat(createdAdmin.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
        assertThat(createdAdmin.getEmail()).isEqualTo("admin@alphaca.com");
    }

    @Test
    @DisplayName("Register tenant fails when name already exists")
    void registerTenant_withDuplicateName_throwsBusinessException() {
        // Arrange
        IdentityDtos.RegisterTenantRequest request = new IdentityDtos.RegisterTenantRequest();
        request.setName("Existing Firm");
        request.setContactEmail("admin@existing.com");
        request.setAdminFullName("Admin");
        request.setAdminPassword("pass12345");

        when(tenantRepository.existsBySlug(anyString())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> tenantService.registerTenant(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");

        verify(tenantRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Slug is generated correctly from organization name")
    void registerTenant_slugGeneratedCorrectly() {
        IdentityDtos.RegisterTenantRequest request = new IdentityDtos.RegisterTenantRequest();
        request.setName("My Finance Corp");
        request.setContactEmail("admin@mfc.com");
        request.setAdminFullName("Admin");
        request.setAdminPassword("pass12345");

        Tenant savedTenant = Tenant.builder()
                .name("My Finance Corp")
                .slug("my-finance-corp")
                .firmCode("MYFINANC-A1B2")
                .contactEmail("admin@mfc.com")
                .status(Tenant.TenantStatus.ACTIVE)
                .build();

        when(tenantRepository.existsBySlug(anyString())).thenReturn(false);
        when(tenantRepository.existsByFirmCodeIgnoreCase(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IdentityDtos.TenantResponse response = tenantService.registerTenant(request);

        assertThat(response.getSlug()).isEqualTo("my-finance-corp");
    }
}
