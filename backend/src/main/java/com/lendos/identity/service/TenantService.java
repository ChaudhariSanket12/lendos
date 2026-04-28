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

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
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

    @Transactional
    public IdentityDtos.TenantResponse registerTenant(IdentityDtos.RegisterTenantRequest request) {
        String slug = generateSlug(request.getName());

        if (tenantRepository.existsBySlug(slug)) {
            throw new BusinessException("TENANT_ALREADY_EXISTS",
                    "An organization with this name already exists");
        }

        Tenant tenant = Tenant.builder()
                .name(request.getName())
                .slug(slug)
                .contactEmail(request.getContactEmail())
                .status(Tenant.TenantStatus.ACTIVE)
                .build();

        tenant = tenantRepository.save(tenant);
        log.info("Tenant registered: slug={}", slug);

        // Create the admin user for this tenant
        User admin = User.builder()
                .fullName(request.getAdminFullName())
                .email(request.getContactEmail())
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

    private String generateSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String slug = WHITESPACE.matcher(normalized).replaceAll("-");
        slug = NON_LATIN.matcher(slug).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }

    private IdentityDtos.TenantResponse mapToTenantResponse(Tenant tenant) {
        return IdentityDtos.TenantResponse.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .slug(tenant.getSlug())
                .contactEmail(tenant.getContactEmail())
                .status(tenant.getStatus().name())
                .createdAt(tenant.getCreatedAt())
                .build();
    }
}
