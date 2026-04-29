package com.lendos.identity.service;

import com.lendos.common.exception.BusinessException;
import com.lendos.common.exception.ResourceNotFoundException;
import com.lendos.identity.dto.IdentityDtos;
import com.lendos.identity.entity.RefreshToken;
import com.lendos.identity.entity.User;
import com.lendos.identity.repository.UserRepository;
import com.lendos.identity.security.JwtService;
import com.lendos.identity.security.LendosUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public IdentityDtos.AuthResponse login(IdentityDtos.LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
        );

        LendosUserDetails userDetails = (LendosUserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmailIgnoreCase(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", userDetails.getUsername()));

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new BusinessException("USER_NOT_ACTIVE",
                    "Your account is not active. Please contact your administrator.",
                    HttpStatus.FORBIDDEN);
        }

        Map<String, Object> extraClaims = Map.of(
                "tenantId", user.getTenant().getId().toString(),
                "tenantSlug", user.getTenant().getSlug(),
                "role", user.getRole().name()
        );

        String accessToken = jwtService.generateAccessToken(userDetails, extraClaims);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("Login successful: email={}, tenant={}", user.getEmail(), user.getTenant().getSlug());

        return IdentityDtos.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(86400)
                .user(mapToUserResponse(user))
                .build();
    }

    @Transactional
    public IdentityDtos.AuthResponse refreshToken(IdentityDtos.RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(request.getRefreshToken());
        User user = refreshToken.getUser();

        LendosUserDetails userDetails = new LendosUserDetails(user);
        Map<String, Object> extraClaims = Map.of(
                "tenantId", user.getTenant().getId().toString(),
                "tenantSlug", user.getTenant().getSlug(),
                "role", user.getRole().name()
        );

        String newAccessToken = jwtService.generateAccessToken(userDetails, extraClaims);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        log.info("Token refreshed for user: {}", user.getEmail());

        return IdentityDtos.AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(86400)
                .user(mapToUserResponse(user))
                .build();
    }

    @Transactional
    public void logout(String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(userEmail))
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));
        refreshTokenService.revokeAllUserTokens(user);
        log.info("User logged out: {}", userEmail);
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return email;
        }
        return email.trim().toLowerCase(Locale.ENGLISH);
    }

    private IdentityDtos.UserResponse mapToUserResponse(User user) {
        return IdentityDtos.UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .tenantId(user.getTenant().getId())
                .tenantName(user.getTenant().getName())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
