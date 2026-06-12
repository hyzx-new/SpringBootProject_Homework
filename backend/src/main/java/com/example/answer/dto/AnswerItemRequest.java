package com.example.answer.dto;

import jakarta.validation.constraints.NotNull;

public record AnswerItemRequest(
        @NotNull Long questionId,
        String answer
) {
}
