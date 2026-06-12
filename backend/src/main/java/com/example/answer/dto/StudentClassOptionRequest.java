package com.example.answer.dto;

import jakarta.validation.constraints.NotBlank;

public record StudentClassOptionRequest(
        @NotBlank String college,
        @NotBlank String major,
        @NotBlank String className
) {
}
