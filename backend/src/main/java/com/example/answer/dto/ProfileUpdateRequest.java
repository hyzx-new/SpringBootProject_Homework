package com.example.answer.dto;

import jakarta.validation.constraints.NotBlank;

public record ProfileUpdateRequest(
        @NotBlank String realName,
        String avatarUrl
) {
}
