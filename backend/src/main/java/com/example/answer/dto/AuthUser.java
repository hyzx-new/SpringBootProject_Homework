package com.example.answer.dto;

public record AuthUser(
        Long id,
        String username,
        String realName,
        String role,
        Long studentId,
        String avatarUrl,
        String major
) {
}
