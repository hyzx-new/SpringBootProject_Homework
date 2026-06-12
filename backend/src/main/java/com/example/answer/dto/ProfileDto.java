package com.example.answer.dto;

public record ProfileDto(
        Long id,
        String username,
        String realName,
        String role,
        String avatarUrl,
        Long studentId,
        String studentNo,
        String className,
        String grade,
        String college,
        String major
) {
}
