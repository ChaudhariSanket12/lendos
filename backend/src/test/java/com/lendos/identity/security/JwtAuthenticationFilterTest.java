package com.lendos.identity.security;

import com.lendos.identity.entity.Tenant;
import com.lendos.identity.entity.User;
import com.lendos.identity.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Unit Tests")
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserDetailsServiceImpl userDetailsService;
    @Mock private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private LendosUserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, userDetailsService);

        Tenant tenant = Tenant.builder()
                .name("Test Tenant")
                .slug("test-tenant")
                .contactEmail("admin@test.com")
                .status(Tenant.TenantStatus.ACTIVE)
                .build();

        User user = User.builder()
                .fullName("Admin User")
                .email("admin@test.com")
                .password("encoded")
                .role(User.Role.ADMIN)
                .status(User.UserStatus.ACTIVE)
                .tenant(tenant)
                .build();
        userDetails = new LendosUserDetails(user);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Authenticates request with standard Bearer header")
    void doFilter_withBearerHeader_setsAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-value");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractUsername("token-value")).thenReturn("admin@test.com");
        when(userDetailsService.loadUserByUsername("admin@test.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("token-value", userDetails)).thenReturn(true);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Authenticates request with lowercase bearer header")
    void doFilter_withLowercaseBearer_setsAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "bearer    token-value");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractUsername("token-value")).thenReturn("admin@test.com");
        when(userDetailsService.loadUserByUsername("admin@test.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("token-value", userDetails)).thenReturn(true);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Skips authentication when header does not contain bearer token")
    void doFilter_withoutBearerHeader_skipsAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Token token-value");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Keeps request unauthenticated when token validation fails")
    void doFilter_withInvalidToken_keepsUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-value");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractUsername("token-value")).thenReturn("admin@test.com");
        when(userDetailsService.loadUserByUsername("admin@test.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid(any(), any())).thenReturn(false);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
