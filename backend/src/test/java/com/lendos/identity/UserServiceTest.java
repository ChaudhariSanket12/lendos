package com.lendos.identity;

import com.lendos.common.exception.BusinessException;
import com.lendos.common.exception.ResourceNotFoundException;
import com.lendos.identity.dto.IdentityDtos;
import com.lendos.identity.entity.Tenant;
import com.lendos.identity.entity.User;
import com.lendos.identity.repository.TenantRepository;
import com.lendos.identity.repository.UserRepository;
import com.lendos.identity.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UUID tenantId;
    private Tenant tenant;
    private User existingUser;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        tenant = Tenant.builder()
                .name("Test Firm")
                .slug("test-firm")
                .contactEmail("admin@testfirm.com")
                .status(Tenant.TenantStatus.ACTIVE)
                .build();

        existingUser = User.builder()
                .fullName("Existing Officer")
                .email("officer@testfirm.com")
                .password("encoded")
                .role(User.Role.CREDIT_OFFICER)
                .status(User.UserStatus.ACTIVE)
                .tenant(tenant)
                .build();
    }

    @Test
    @DisplayName("Create user succeeds with valid data")
    void createUser_withValidData_returnsUserResponse() {
        IdentityDtos.CreateUserRequest request = new IdentityDtos.CreateUserRequest();
        request.setFullName("New Officer");
        request.setEmail("new@testfirm.com");
        request.setPassword("pass12345");
        request.setRole(User.Role.CREDIT_OFFICER);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.existsByEmailAndTenant("new@testfirm.com", tenant)).thenReturn(false);
        when(passwordEncoder.encode("pass12345")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        IdentityDtos.UserResponse response = userService.createUser(tenantId, request);

        assertThat(response.getEmail()).isEqualTo("new@testfirm.com");
        assertThat(response.getRole()).isEqualTo(User.Role.CREDIT_OFFICER);
        assertThat(response.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("Create user fails when email already exists in tenant")
    void createUser_withDuplicateEmail_throwsBusinessException() {
        IdentityDtos.CreateUserRequest request = new IdentityDtos.CreateUserRequest();
        request.setEmail("officer@testfirm.com");
        request.setFullName("Duplicate");
        request.setPassword("pass12345");
        request.setRole(User.Role.CREDIT_OFFICER);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.existsByEmailAndTenant("officer@testfirm.com", tenant)).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(tenantId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get users by tenant returns all tenant users")
    void getUsersByTenant_returnsListOfUsers() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findAllByTenant(tenant)).thenReturn(List.of(existingUser));

        List<IdentityDtos.UserResponse> users = userService.getUsersByTenant(tenantId);

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getEmail()).isEqualTo("officer@testfirm.com");
    }

    @Test
    @DisplayName("Update user status to LOCKED succeeds")
    void updateUserStatus_toLocked_updatesCorrectly() {
        UUID userId = UUID.randomUUID();
        IdentityDtos.UpdateUserStatusRequest request = new IdentityDtos.UpdateUserStatusRequest();
        request.setStatus(User.UserStatus.LOCKED);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        IdentityDtos.UserResponse response = userService.updateUserStatus(userId, request);

        assertThat(response.getStatus()).isEqualTo(User.UserStatus.LOCKED);
    }

    @Test
    @DisplayName("Get user by invalid ID throws ResourceNotFoundException")
    void getUserById_withInvalidId_throwsResourceNotFoundException() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
