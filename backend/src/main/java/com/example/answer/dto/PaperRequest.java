package com.example.answer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record PaperRequest(
        @NotBlank String title,
        String description,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        Boolean published,
        String major,
        Boolean commonCourse
) {
}
