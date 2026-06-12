package com.example.answer.dto;

import jakarta.validation.constraints.NotBlank;

public record StudentRequest(
        @NotBlank String username,
        String password,
        @NotBlank String realName,
        String studentNo,
        @NotBlank String className,
        @NotBlank String grade,
        @NotBlank String college,
        @NotBlank String major,
        Boolean enabled
) {
}
