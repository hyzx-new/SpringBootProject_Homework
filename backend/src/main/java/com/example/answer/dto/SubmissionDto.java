package com.example.answer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SubmissionDto(
        Long id,
        Long paperId,
        String paperTitle,
        Long studentId,
        String studentNo,
        String studentName,
        String className,
        String grade,
        String college,
        BigDecimal score,
        BigDecimal totalScore,
        LocalDateTime submittedAt,
        List<AnswerResultDto> answers
) {
}
