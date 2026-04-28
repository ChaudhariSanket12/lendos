package com.lendos.identity.service;

import com.lendos.common.exception.BusinessException;
import com.lendos.config.AppProperties;
import com.lendos.identity.entity.RefreshToken;
import com.lendos.identity.entity.User;
import com.lendos.identity.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AppProperties appProperties;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Revoke all existing tokens for user before issuing new one
        refreshTokenRepository.revokeAllUserTokens(user);

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(
                        appProperties.getJwt().getRefreshExpirationMs()))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String tokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new BusinessException(
                        "INVALID_REFRESH_TOKEN",
                        "Refresh token not found",
                        HttpStatus.UNAUTHORIZED));

        if (token.isRevoked()) {
            throw new BusinessException(
                    "REFRESH_TOKEN_REVOKED",
                    "Refresh token has been revoked",
                    HttpStatus.UNAUTHORIZED);
        }

        if (token.isExpired()) {
            throw new BusinessException(
                    "REFRESH_TOKEN_EXPIRED",
                    "Refresh token has expired. Please login again.",
                    HttpStatus.UNAUTHORIZED);
        }

        return token;
    }

    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllUserTokens(user);
        log.info("All refresh tokens revoked for user: {}", user.getEmail());
    }

    // Runs daily at 2 AM to clean up expired/revoked tokens
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Running scheduled cleanup of expired refresh tokens");
        refreshTokenRepository.deleteExpiredAndRevokedTokens();
    }
}
