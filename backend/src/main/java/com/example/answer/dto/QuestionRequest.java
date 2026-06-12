package com.example.answer.dto;

import com.example.answer.entity.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record QuestionRequest(
        @NotBlank String title,
        @NotNull QuestionType type,
        String optionsText,
        @NotBlank String correctAnswer,
        @NotNull BigDecimal score,
        Integer orderNo
) {
}
