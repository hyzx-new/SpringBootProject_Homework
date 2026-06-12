package com.example.answer.dto;

import jakarta.validation.constraints.NotBlank;

public record DiscussionReplyRequest(@NotBlank String content) {
}
