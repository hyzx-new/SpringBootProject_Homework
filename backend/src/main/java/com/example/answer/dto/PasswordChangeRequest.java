package com.example.answer.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordChangeRequest(
        @NotBlank String oldPassword,
        @NotBlank String newPassword
) {
}
