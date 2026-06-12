package com.example.answer.dto;

import jakarta.validation.constraints.NotBlank;

public record DiscussionPostRequest(
        @NotBlank String title,
        @NotBlank String content,
        Long paperId
) {
}
