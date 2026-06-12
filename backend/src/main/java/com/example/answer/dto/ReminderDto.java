package com.example.answer.dto;

import java.time.LocalDateTime;

public record ReminderDto(
        Long id,
        Long paperId,
        String paperTitle,
        String message,
        LocalDateTime remindAt,
        Boolean readFlag
) {
}
