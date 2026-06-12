package com.example.answer.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitPaperRequest(
        @NotEmpty List<AnswerItemRequest> answers
) {
}
