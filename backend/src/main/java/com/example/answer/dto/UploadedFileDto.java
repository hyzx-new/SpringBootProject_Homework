package com.example.answer.dto;

import java.time.LocalDateTime;

public record UploadedFileDto(
        Long id,
        String originalName,
        String contentType,
        Long fileSize,
        Long uploaderId,
        String uploaderName,
        LocalDateTime createdAt,
        String downloadUrl,
        boolean canDelete
) {
}
