package com.lendos.borrower.dto;

import com.lendos.borrower.entity.Borrower;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class BorrowerDtos {

    @Getter
    @Setter
    public static class CreateBorrowerRequest {

        @NotBlank(message = "First name is required")
        @Size(min = 2, message = "First name must be at least 2 characters")
        @Pattern(regexp = "^[A-Za-z ]+$", message = "First name can contain only letters and spaces")
        private String firstName;

        @NotBlank(message = "Last name is required")
        @Size(min = 2, message = "Last name must be at least 2 characters")
        @Pattern(regexp = "^[A-Za-z ]+$", message = "Last name can contain only letters and spaces")
        private String lastName;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "Invalid email format")
        private String email;

        @NotBlank(message = "Phone is required")
        private String phone;

        @NotNull(message = "Date of birth is required")
        private LocalDate dateOfBirth;

        @NotBlank(message = "Address is required")
        @Size(min = 5, message = "Address must be at least 5 characters")
        private String address;
    }

    @Getter
    @Setter
    public static class UpdateBorrowerStatusRequest {
        @NotBlank(message = "Status is required")
        private String status;
    }

    @Getter
    @Builder
    public static class BorrowerResponse {
        private UUID id;
        private String firstName;
        private String lastName;
        private String fullName;
        private String email;
        private String phone;
        private Borrower.BorrowerStatus status;
        private LocalDate dateOfBirth;
        private String address;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
