package com.example.answer.dto;

import com.example.answer.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserRequest(
        @NotBlank String username,
        String password,
        @NotBlank String realName,
        @NotNull Role role,
        Boolean enabled,
        String major
) {
}
