package com.example.answer.dto;

import java.time.LocalDateTime;

public record DiscussionPostDto(
        Long id,
        String title,
        String content,
        Long paperId,
        String paperTitle,
        Long authorId,
        String authorName,
        String authorRole,
        String authorAvatarUrl,
        long replyCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean canDelete
) {
}
