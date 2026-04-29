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

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final String FIRM_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    @Transactional
    public IdentityDtos.TenantResponse registerTenant(IdentityDtos.RegisterTenantRequest request) {
        String slug = generateSlug(request.getName());
        String normalizedContactEmail = normalizeEmail(request.getContactEmail());

        if (tenantRepository.existsBySlug(slug)) {
            throw new BusinessException("TENANT_ALREADY_EXISTS",
                    "An organization with this name already exists");
        }

        Tenant tenant = Tenant.builder()
                .name(request.getName())
                .slug(slug)
                .firmCode(generateFirmCode(request.getName()))
                .contactEmail(normalizedContactEmail)
                .status(Tenant.TenantStatus.ACTIVE)
                .build();

        tenant = tenantRepository.save(tenant);
        log.info("Tenant registered: slug={}", slug);

        // Create the admin user for this tenant
        User admin = User.builder()
                .fullName(request.getAdminFullName())
                .email(normalizedContactEmail)
                .password(passwordEncoder.encode(request.getAdminPassword()))
                .role(User.Role.ADMIN)
                .status(User.UserStatus.ACTIVE)
                .tenant(tenant)
                .build();

        userRepository.save(admin);
        log.info("Admin user created for tenant: slug={}, email={}", slug, admin.getEmail());

        return mapToTenantResponse(tenant);
    }

    @Transactional(readOnly = true)
    public IdentityDtos.TenantResponse getTenantById(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId.toString()));
        return mapToTenantResponse(tenant);
    }

    @Transactional(readOnly = true)
    public List<IdentityDtos.TenantResponse> getAllTenants() {
        return tenantRepository.findAll().stream()
                .map(this::mapToTenantResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IdentityDtos.FirmCodeResponse getFirmCodeByTenantId(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId.toString()));

        return IdentityDtos.FirmCodeResponse.builder()
                .tenantId(tenant.getId())
                .tenantName(tenant.getName())
                .firmCode(tenant.getFirmCode())
                .build();
    }

    private String generateSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String slug = WHITESPACE.matcher(normalized).replaceAll("-");
        slug = NON_LATIN.matcher(slug).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ENGLISH);
    }

    private String generateFirmCode(String tenantName) {
        String normalized = tenantName == null
                ? "TENANT"
                : tenantName.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ENGLISH);
        if (!StringUtils.hasText(normalized)) {
            normalized = "TENANT";
        }
        String prefix = normalized.length() > 8 ? normalized.substring(0, 8) : normalized;

        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = prefix + "-" + randomSuffix(4);
            if (!tenantRepository.existsByFirmCodeIgnoreCase(candidate)) {
                return candidate;
            }
        }

        throw new BusinessException(
                "FIRM_CODE_GENERATION_FAILED",
                "Unable to generate a unique firm code. Please retry tenant registration."
        );
    }

    private String randomSuffix(int length) {
        StringBuilder value = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = ThreadLocalRandom.current().nextInt(FIRM_CODE_CHARS.length());
            value.append(FIRM_CODE_CHARS.charAt(index));
        }
        return value.toString();
    }

    private IdentityDtos.TenantResponse mapToTenantResponse(Tenant tenant) {
        return IdentityDtos.TenantResponse.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .slug(tenant.getSlug())
                .firmCode(tenant.getFirmCode())
                .contactEmail(tenant.getContactEmail())
                .status(tenant.getStatus().name())
                .createdAt(tenant.getCreatedAt())
                .build();
    }
}
