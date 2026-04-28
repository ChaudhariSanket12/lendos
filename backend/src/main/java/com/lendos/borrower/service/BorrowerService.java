package com.lendos.borrower.service;

import com.lendos.borrower.dto.BorrowerDtos;
import com.lendos.borrower.entity.Borrower;
import com.lendos.borrower.repository.BorrowerRepository;
import com.lendos.common.exception.BusinessException;
import com.lendos.common.exception.ResourceNotFoundException;
import com.lendos.common.exception.ValidationException;
import com.lendos.identity.entity.Tenant;
import com.lendos.identity.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.Period;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowerService {

    private final BorrowerRepository borrowerRepository;
    private final TenantRepository tenantRepository;
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z ]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\d{10}|\\d{11,15})$");
    private static final int MINIMUM_AGE_YEARS = 18;

    private static final Map<Borrower.BorrowerStatus, Set<Borrower.BorrowerStatus>> ALLOWED_TRANSITIONS = Map.of(
            Borrower.BorrowerStatus.DRAFT, Set.of(Borrower.BorrowerStatus.UNDER_REVIEW),
            Borrower.BorrowerStatus.UNDER_REVIEW, Set.of(Borrower.BorrowerStatus.VERIFIED, Borrower.BorrowerStatus.DRAFT),
            Borrower.BorrowerStatus.VERIFIED, Set.of(Borrower.BorrowerStatus.ACTIVE, Borrower.BorrowerStatus.BLACKLISTED),
            Borrower.BorrowerStatus.ACTIVE, Set.of(Borrower.BorrowerStatus.BLACKLISTED),
            Borrower.BorrowerStatus.BLACKLISTED, Set.of(Borrower.BorrowerStatus.VERIFIED)
    );

    @Transactional
    public BorrowerDtos.BorrowerResponse createBorrower(UUID tenantId, BorrowerDtos.CreateBorrowerRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId.toString()));

        String firstName = normalize(request.getFirstName());
        String lastName = normalize(request.getLastName());
        String email = normalizeEmail(request.getEmail());
        String phone = normalizePhone(request.getPhone());
        String address = normalize(request.getAddress());
        LocalDate dateOfBirth = request.getDateOfBirth();

        validateCreateRequest(tenantId, firstName, lastName, email, phone, dateOfBirth, address);

        Borrower borrower = Borrower.builder()
                .tenant(tenant)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone(phone)
                .dateOfBirth(dateOfBirth)
                .address(address)
                .status(Borrower.BorrowerStatus.DRAFT)
                .build();

        Borrower saved = borrowerRepository.save(borrower);
        log.info("Borrower created: borrowerId={}, tenantId={}", saved.getId(), tenantId);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BorrowerDtos.BorrowerResponse> listBorrowers(UUID tenantId, String status) {
        List<Borrower> borrowers;
        if (StringUtils.hasText(status)) {
            Borrower.BorrowerStatus parsedStatus = parseStatus(status);
            borrowers = borrowerRepository.findAllByTenant_IdAndStatusOrderByCreatedAtDesc(tenantId, parsedStatus);
        } else {
            borrowers = borrowerRepository.findAllByTenant_IdOrderByCreatedAtDesc(tenantId);
        }

        return borrowers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BorrowerDtos.BorrowerResponse getBorrowerById(UUID tenantId, UUID borrowerId) {
        Borrower borrower = getBorrowerEntityById(tenantId, borrowerId);
        return mapToResponse(borrower);
    }

    @Transactional
    public BorrowerDtos.BorrowerResponse updateBorrowerStatus(
            UUID tenantId,
            UUID borrowerId,
            BorrowerDtos.UpdateBorrowerStatusRequest request
    ) {
        Borrower borrower = getBorrowerEntityById(tenantId, borrowerId);
        Borrower.BorrowerStatus currentStatus = borrower.getStatus();
        Borrower.BorrowerStatus targetStatus = parseStatus(request.getStatus());
        validateStatusTransition(currentStatus, targetStatus);

        borrower.setStatus(targetStatus);
        Borrower updated = borrowerRepository.save(borrower);

        log.info("Borrower status updated: borrowerId={}, tenantId={}, from={}, to={}",
                borrowerId, tenantId, currentStatus, targetStatus);

        return mapToResponse(updated);
    }

    @Transactional
    public void deleteBorrower(UUID tenantId, UUID borrowerId) {
        Borrower borrower = getBorrowerEntityById(tenantId, borrowerId);

        if (borrower.getStatus() != Borrower.BorrowerStatus.DRAFT) {
            throw new BusinessException(
                    "BORROWER_DELETE_NOT_ALLOWED",
                    "Borrower can be deleted only when status is DRAFT"
            );
        }

        borrowerRepository.delete(borrower);
        log.info("Borrower deleted: borrowerId={}, tenantId={}", borrowerId, tenantId);
    }

    private Borrower getBorrowerEntityById(UUID tenantId, UUID borrowerId) {
        return borrowerRepository.findByIdAndTenant_Id(borrowerId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower", borrowerId.toString()));
    }

    private void validateStatusTransition(Borrower.BorrowerStatus current, Borrower.BorrowerStatus target) {
        Set<Borrower.BorrowerStatus> allowedTargets = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowedTargets.contains(target)) {
            throw new BusinessException(
                    "INVALID_BORROWER_STATUS_TRANSITION",
                    String.format("Status transition not allowed: %s -> %s", current, target)
            );
        }
    }

    private Borrower.BorrowerStatus parseStatus(String rawStatus) {
        try {
            return Borrower.BorrowerStatus.valueOf(rawStatus.trim().toUpperCase(Locale.ENGLISH));
        } catch (Exception ex) {
            throw new BusinessException(
                    "INVALID_BORROWER_STATUS",
                    "Invalid borrower status: " + rawStatus
            );
        }
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
        // Phone is stored in canonical numeric format to make uniqueness reliable.
        return phone.replaceAll("[^0-9]", "");
    }

    private void validateCreateRequest(
            UUID tenantId,
            String firstName,
            String lastName,
            String email,
            String phone,
            LocalDate dateOfBirth,
            String address
    ) {
        // Server-side validation is the source of truth; frontend validation is convenience only.
        // Rules covered: name format, email/phone uniqueness per tenant, phone format, DOB age limits, and address length.
        Map<String, String> errors = new LinkedHashMap<>();

        if (!StringUtils.hasText(firstName) || firstName.length() < 2 || !NAME_PATTERN.matcher(firstName).matches()) {
            errors.put("firstName", "First name must be at least 2 characters and contain only letters and spaces");
        }

        if (!StringUtils.hasText(lastName) || lastName.length() < 2 || !NAME_PATTERN.matcher(lastName).matches()) {
            errors.put("lastName", "Last name must be at least 2 characters and contain only letters and spaces");
        }

        if (!StringUtils.hasText(email)) {
            errors.put("email", "Email is required");
        }

        if (!StringUtils.hasText(phone)) {
            errors.put("phone", "Phone is required");
        } else if (!PHONE_PATTERN.matcher(phone).matches()) {
            // Accepted formats after normalization: 10-digit India local or 11-15 digit international.
            errors.put("phone", "Invalid phone number format");
        }

        if (dateOfBirth == null) {
            errors.put("dateOfBirth", "Date of birth is required");
        } else {
            validateDateOfBirth(dateOfBirth, errors);
        }

        if (!StringUtils.hasText(address) || address.length() < 5) {
            errors.put("address", "Address must be at least 5 characters");
        }

        if (StringUtils.hasText(email) && borrowerRepository.existsByTenant_IdAndEmailIgnoreCase(tenantId, email)) {
            errors.put("email", "A borrower with this email already exists");
        }

        if (StringUtils.hasText(phone) && borrowerRepository.existsByTenant_IdAndPhone(tenantId, phone)) {
            errors.put("phone", "A borrower with this phone number already exists");
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

    private BorrowerDtos.BorrowerResponse mapToResponse(Borrower borrower) {
        return BorrowerDtos.BorrowerResponse.builder()
                .id(borrower.getId())
                .firstName(borrower.getFirstName())
                .lastName(borrower.getLastName())
                .fullName(borrower.getFirstName() + " " + borrower.getLastName())
                .email(borrower.getEmail())
                .phone(borrower.getPhone())
                .status(borrower.getStatus())
                .dateOfBirth(borrower.getDateOfBirth())
                .address(borrower.getAddress())
                .createdAt(borrower.getCreatedAt())
                .updatedAt(borrower.getUpdatedAt())
                .build();
    }
}
