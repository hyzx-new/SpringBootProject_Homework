package com.example.answer.dto;

import com.example.answer.entity.QuestionType;

import java.math.BigDecimal;

public record AnswerResultDto(
        Long answerId,
        Long questionId,
        QuestionType questionType,
        String title,
        String answer,
        String correctAnswer,
        boolean correct,
        BigDecimal score,
        BigDecimal fullScore
) {
}
