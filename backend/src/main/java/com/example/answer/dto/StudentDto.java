package com.example.answer.dto;

public record StudentDto(
        Long id,
        Long userId,
        String username,
        String realName,
        String studentNo,
        String className,
        String grade,
        String college,
        String major,
        Boolean enabled,
        String avatarUrl
) {
}
