package com.example.answer.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AnswerScoreRequest(
        @NotNull BigDecimal score,
        Boolean correct
) {
}
