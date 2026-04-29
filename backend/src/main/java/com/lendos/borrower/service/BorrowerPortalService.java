package com.lendos.borrower.service;

import com.lendos.borrower.dto.BorrowerDtos;
import com.lendos.borrower.entity.Borrower;
import com.lendos.borrower.repository.BorrowerRepository;
import com.lendos.common.exception.BusinessException;
import com.lendos.common.exception.ResourceNotFoundException;
import com.lendos.common.exception.ValidationException;
import com.lendos.identity.dto.BorrowerAuthDtos;
import com.lendos.identity.dto.IdentityDtos;
import com.lendos.identity.entity.Tenant;
import com.lendos.identity.entity.User;
import com.lendos.identity.entity.RefreshToken;
import com.lendos.identity.repository.TenantRepository;
import com.lendos.identity.repository.UserRepository;
import com.lendos.identity.security.JwtService;
import com.lendos.identity.security.LendosUserDetails;
import com.lendos.identity.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.Period;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowerPortalService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\d{10}|\\d{11,15})$");
    private static final int MINIMUM_AGE_YEARS = 18;

    private final BorrowerRepository borrowerRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public BorrowerAuthDtos.BorrowerAuthResponse registerBorrower(BorrowerAuthDtos.RegisterBorrowerRequest request) {
        Tenant tenant = resolveTenantByFirmCode(request.getFirmCode());
        String normalizedEmail = normalizeEmail(request.getEmail());
        String normalizedFullName = normalize(request.getFullName());

        validateBorrowerRegistration(tenant, normalizedFullName, normalizedEmail, request.getPassword());
        NameParts nameParts = splitName(normalizedFullName);

        User user = User.builder()
                .fullName(normalizedFullName)
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.BORROWER)
                .status(User.UserStatus.ACTIVE)
                .tenant(tenant)
                .build();
        user = userRepository.save(user);

        Borrower borrower = Borrower.builder()
                .tenant(tenant)
                .user(user)
                .firstName(nameParts.firstName())
                .lastName(nameParts.lastName())
                .email(normalizedEmail)
                .status(Borrower.BorrowerStatus.DRAFT)
                .build();
        borrower = borrowerRepository.save(borrower);

        LendosUserDetails userDetails = new LendosUserDetails(user);
        Map<String, Object> extraClaims = Map.of(
                "tenantId", tenant.getId().toString(),
                "tenantSlug", tenant.getSlug(),
                "role", user.getRole().name()
        );
        String accessToken = jwtService.generateAccessToken(userDetails, extraClaims);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("Borrower self-registration successful: userId={}, borrowerId={}, tenantId={}",
                user.getId(), borrower.getId(), tenant.getId());

        return BorrowerAuthDtos.BorrowerAuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(86400)
                .user(mapToUserResponse(user))
                .borrower(mapToBorrowerProfileResponse(borrower))
                .build();
    }

    @Transactional(readOnly = true)
    public BorrowerDtos.BorrowerMeResponse getMyBorrower(UUID tenantId, UUID userId) {
        Borrower borrower = getBorrowerByUser(tenantId, userId);
        return mapToBorrowerMeResponse(borrower);
    }

    @Transactional(readOnly = true)
    public BorrowerDtos.BorrowerProfileResponse getMyBorrowerProfile(UUID tenantId, UUID userId) {
        Borrower borrower = getBorrowerByUser(tenantId, userId);
        return mapToBorrowerProfileResponse(borrower);
    }

    @Transactional
    public BorrowerDtos.BorrowerProfileResponse updateMyBorrowerProfile(
            UUID tenantId,
            UUID userId,
            BorrowerDtos.UpdateBorrowerProfileRequest request
    ) {
        Borrower borrower = getBorrowerByUser(tenantId, userId);
        borrower.setMonthlyIncome(request.getMonthlyIncome());
        borrower.setEmploymentType(request.getEmploymentType());
        borrower.setYearsInCurrentJob(request.getYearsInCurrentJob());
        borrower.setExistingMonthlyObligations(request.getExistingMonthlyObligations());
        borrower.setPanNumber(normalizePan(request.getPanNumber()));

        Borrower updated = borrowerRepository.save(borrower);
        log.info("Borrower profile updated: borrowerId={}, tenantId={}", updated.getId(), tenantId);
        return mapToBorrowerProfileResponse(updated);
    }

    @Transactional
    public BorrowerDtos.BorrowerProfileResponse completeMyBorrowerProfile(
            UUID tenantId,
            UUID userId,
            BorrowerDtos.CompleteBorrowerProfileRequest request
    ) {
        Borrower borrower = getBorrowerByUser(tenantId, userId);
        String normalizedPhone = normalizePhone(request.getPhone());
        String normalizedAddress = normalize(request.getAddress());
        String normalizedPan = normalizePan(request.getPanNumber());

        Map<String, String> errors = new LinkedHashMap<>();
        if (!StringUtils.hasText(normalizedPhone)) {
            errors.put("phone", "Phone is required");
        } else if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            errors.put("phone", "Invalid phone number format");
        }

        if (request.getDateOfBirth() == null) {
            errors.put("dateOfBirth", "Date of birth is required");
        } else {
            validateDateOfBirth(request.getDateOfBirth(), errors);
        }

        if (!StringUtils.hasText(normalizedAddress)) {
            errors.put("address", "Address is required");
        }

        if (StringUtils.hasText(request.getPanNumber()) && !StringUtils.hasText(normalizedPan)) {
            errors.put("panNumber", "PAN number must be exactly 10 characters");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        borrower.setPhone(normalizedPhone);
        borrower.setDateOfBirth(request.getDateOfBirth());
        borrower.setAddress(normalizedAddress);
        borrower.setMonthlyIncome(request.getMonthlyIncome());
        borrower.setEmploymentType(request.getEmploymentType());
        borrower.setYearsInCurrentJob(request.getYearsInCurrentJob());
        borrower.setExistingMonthlyObligations(request.getExistingMonthlyObligations());
        borrower.setPanNumber(normalizedPan);
        if (borrower.getStatus() == Borrower.BorrowerStatus.DRAFT) {
            borrower.setStatus(Borrower.BorrowerStatus.UNDER_REVIEW);
        }

        Borrower updated = borrowerRepository.save(borrower);
        log.info("Borrower profile completed: borrowerId={}, tenantId={}, status={}",
                updated.getId(), tenantId, updated.getStatus());
        return mapToBorrowerProfileResponse(updated);
    }

    private Borrower getBorrowerByUser(UUID tenantId, UUID userId) {
        return borrowerRepository.findByTenant_IdAndUser_Id(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower", userId.toString()));
    }

    private Tenant resolveTenantByFirmCode(String firmCode) {
        String normalizedFirmCode = normalize(firmCode);
        if (!StringUtils.hasText(normalizedFirmCode)) {
            throw new BusinessException(
                    "INVALID_FIRM_CODE",
                    "Invalid firm code. Please contact your lending institution.",
                    HttpStatus.BAD_REQUEST
            );
        }

        return tenantRepository.findByFirmCodeIgnoreCase(normalizedFirmCode)
                .orElseThrow(() -> new BusinessException(
                        "INVALID_FIRM_CODE",
                        "Invalid firm code. Please contact your lending institution.",
                        HttpStatus.BAD_REQUEST
                ));
    }

    private void validateBorrowerRegistration(Tenant tenant, String fullName, String email, String password) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!StringUtils.hasText(fullName)) {
            errors.put("fullName", "Full name is required");
        }
        if (!StringUtils.hasText(email)) {
            errors.put("email", "Email is required");
        }
        if (!StringUtils.hasText(password) || password.length() < 8) {
            errors.put("password", "Password must be at least 8 characters");
        }
        if (StringUtils.hasText(email) && userRepository.existsByTenant_IdAndEmailIgnoreCase(tenant.getId(), email)) {
            errors.put("email", "Email already registered");
        }
        if (StringUtils.hasText(email) && borrowerRepository.existsByTenant_IdAndEmailIgnoreCase(tenant.getId(), email)) {
            errors.put("email", "Email already registered");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private void validateDateOfBirth(LocalDate dateOfBirth, Map<String, String> errors) {
        LocalDate today = LocalDate.now();
        if (dateOfBirth.isAfter(today)) {
            errors.put("dateOfBirth", "Date of birth cannot be in the future");
            return;
        }

        int years = Period.between(dateOfBirth, today).getYears();
        if (years < MINIMUM_AGE_YEARS) {
            errors.put("dateOfBirth", "Borrower must be at least 18 years old");
        }
    }

    private NameParts splitName(String fullName) {
        String[] parts = fullName.trim().split("\\s+", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : "Borrower";
        return new NameParts(firstName, lastName);
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeEmail(String email) {
        String normalized = normalize(email);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return normalized.toLowerCase(Locale.ENGLISH);
    }

    private String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        return phone.replaceAll("[^0-9]", "");
    }

    private String normalizePan(String panNumber) {
        String normalized = normalize(panNumber);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        String pan = normalized.toUpperCase(Locale.ENGLISH);
        if (pan.length() != 10) {
            return null;
        }
        return pan;
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

    private BorrowerDtos.BorrowerMeResponse mapToBorrowerMeResponse(Borrower borrower) {
        return BorrowerDtos.BorrowerMeResponse.builder()
                .id(borrower.getId())
                .firstName(borrower.getFirstName())
                .lastName(borrower.getLastName())
                .fullName(borrower.getFirstName() + " " + borrower.getLastName())
                .email(borrower.getEmail())
                .status(borrower.getStatus())
                .build();
    }

    private BorrowerDtos.BorrowerProfileResponse mapToBorrowerProfileResponse(Borrower borrower) {
        return BorrowerDtos.BorrowerProfileResponse.builder()
                .id(borrower.getId())
                .firstName(borrower.getFirstName())
                .lastName(borrower.getLastName())
                .fullName(borrower.getFirstName() + " " + borrower.getLastName())
                .email(borrower.getEmail())
                .phone(borrower.getPhone())
                .status(borrower.getStatus())
                .dateOfBirth(borrower.getDateOfBirth())
                .address(borrower.getAddress())
                .monthlyIncome(borrower.getMonthlyIncome())
                .employmentType(borrower.getEmploymentType())
                .yearsInCurrentJob(borrower.getYearsInCurrentJob())
                .existingMonthlyObligations(borrower.getExistingMonthlyObligations())
                .panNumber(borrower.getPanNumber())
                .creditScore(borrower.getCreditScore())
                .createdAt(borrower.getCreatedAt())
                .updatedAt(borrower.getUpdatedAt())
                .build();
    }

    private record NameParts(String firstName, String lastName) {
    }
}
