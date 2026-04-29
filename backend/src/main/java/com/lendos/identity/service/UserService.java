package com.lendos.identity.service;

import com.lendos.common.exception.BusinessException;
import com.lendos.common.exception.ResourceNotFoundException;
import com.lendos.identity.dto.IdentityDtos;
import com.lendos.identity.entity.Tenant;
import com.lendos.identity.entity.User;
import com.lendos.identity.repository.TenantRepository;
import com.lendos.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public IdentityDtos.UserResponse createUser(UUID tenantId, IdentityDtos.CreateUserRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId.toString()));
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (userRepository.existsByTenant_IdAndEmailIgnoreCase(tenantId, normalizedEmail)) {
            throw new BusinessException("USER_ALREADY_EXISTS",
                    "A user with this email already exists in your organization");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status(User.UserStatus.ACTIVE)
                .tenant(tenant)
                .build();

        user = userRepository.save(user);
        log.info("User created: email={}, role={}, tenant={}", user.getEmail(), user.getRole(), tenant.getSlug());
        return mapToUserResponse(user);
    }

    @Transactional(readOnly = true)
    public List<IdentityDtos.UserResponse> getUsersByTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId.toString()));
        return userRepository.findAllByTenant(tenant).stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IdentityDtos.UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
        return mapToUserResponse(user);
    }

    @Transactional
    public IdentityDtos.UserResponse updateUserStatus(UUID userId,
                                                       IdentityDtos.UpdateUserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        user.setStatus(request.getStatus());
        user = userRepository.save(user);
        log.info("User status updated: email={}, newStatus={}", user.getEmail(), user.getStatus());
        return mapToUserResponse(user);
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

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return email;
        }
        return email.trim().toLowerCase(Locale.ENGLISH);
    }
}
