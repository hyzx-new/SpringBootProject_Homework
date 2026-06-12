package com.example.answer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaperDto(
        Long id,
        String title,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BigDecimal totalScore,
        Boolean published,
        String major,
        Boolean commonCourse,
        String createdBy
) {
}
