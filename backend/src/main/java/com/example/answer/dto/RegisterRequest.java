package com.example.answer.dto;

import com.example.answer.entity.Role;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String realName,
        Role role,
        String studentNo,
        String className,
        String grade,
        String college,
        String major
) {
}
