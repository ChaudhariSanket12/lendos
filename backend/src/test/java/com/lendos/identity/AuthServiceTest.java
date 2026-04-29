package com.lendos.identity;

import com.lendos.common.exception.BusinessException;
import com.lendos.identity.dto.IdentityDtos;
import com.lendos.identity.entity.RefreshToken;
import com.lendos.identity.entity.Tenant;
import com.lendos.identity.entity.User;
import com.lendos.identity.repository.UserRepository;
import com.lendos.identity.security.JwtService;
import com.lendos.identity.security.LendosUserDetails;
import com.lendos.identity.service.AuthService;
import com.lendos.identity.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private Tenant tenant;
    private User activeUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(UUID.randomUUID());  // Using setter since @Builder not used here directly
        tenant.setName("Test CA Firm");
        tenant.setSlug("test-ca-firm");
        tenant.setContactEmail("admin@testca.com");
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);

        activeUser = User.builder()
                .fullName("Test Admin")
                .email("admin@testca.com")
                .password("$2a$12$encodedPassword")
                .role(User.Role.ADMIN)
                .status(User.UserStatus.ACTIVE)
                .tenant(tenant)
                .build();

        inactiveUser = User.builder()
                .fullName("Inactive User")
                .email("inactive@testca.com")
                .password("$2a$12$encodedPassword")
                .role(User.Role.CREDIT_OFFICER)
                .status(User.UserStatus.INACTIVE)
                .tenant(tenant)
                .build();
    }

    @Test
    @DisplayName("Login succeeds with valid credentials and active user")
    void login_withValidCredentialsAndActiveUser_returnsAuthResponse() {
        // Arrange
        IdentityDtos.LoginRequest request = new IdentityDtos.LoginRequest();
        request.setEmail("admin@testca.com");
        request.setPassword("password123");

        LendosUserDetails userDetails = new LendosUserDetails(activeUser);
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(activeUser)
                .expiryDate(Instant.now().plusSeconds(604800))
                .revoked(false)
                .build();

        when(authenticationManager.authenticate(any())).thenReturn(authToken);
        when(userRepository.findByEmailIgnoreCase("admin@testca.com")).thenReturn(Optional.of(activeUser));
        when(jwtService.generateAccessToken(any(), any())).thenReturn("mock.jwt.token");
        when(refreshTokenService.createRefreshToken(activeUser)).thenReturn(refreshToken);

        // Act
        IdentityDtos.AuthResponse response = authService.login(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getRefreshToken()).isEqualTo(refreshToken.getToken());
        assertThat(response.getUser().getEmail()).isEqualTo("admin@testca.com");
        assertThat(response.getUser().getRole()).isEqualTo(User.Role.ADMIN);
        verify(refreshTokenService).createRefreshToken(activeUser);
    }

    @Test
    @DisplayName("Login fails when user account is inactive")
    void login_withInactiveUser_throwsBusinessException() {
        // Arrange
        IdentityDtos.LoginRequest request = new IdentityDtos.LoginRequest();
        request.setEmail("inactive@testca.com");
        request.setPassword("password123");

        LendosUserDetails userDetails = new LendosUserDetails(inactiveUser);
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authToken);
        when(userRepository.findByEmailIgnoreCase("inactive@testca.com")).thenReturn(Optional.of(inactiveUser));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not active");

        verify(jwtService, never()).generateAccessToken(any(), any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    @DisplayName("Login fails with bad credentials")
    void login_withBadCredentials_throwsException() {
        // Arrange
        IdentityDtos.LoginRequest request = new IdentityDtos.LoginRequest();
        request.setEmail("admin@testca.com");
        request.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
