package com.example.answer.dto;

import com.example.answer.entity.QuestionType;

import java.math.BigDecimal;

public record QuestionDto(
        Long id,
        Long paperId,
        String title,
        QuestionType type,
        String optionsText,
        String correctAnswer,
        BigDecimal score,
        Integer orderNo
) {
}
