package com.example.answer.dto;

public record UserDto(
        Long id,
        String username,
        String realName,
        String role,
        Boolean enabled,
        String avatarUrl,
        String major
) {
}
