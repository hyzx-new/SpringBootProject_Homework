package com.example.answer.dto;

import java.time.LocalDateTime;

public record DiscussionReplyDto(
        Long id,
        Long postId,
        String content,
        Long authorId,
        String authorName,
        String authorRole,
        String authorAvatarUrl,
        LocalDateTime createdAt,
        boolean canDelete
) {
}
