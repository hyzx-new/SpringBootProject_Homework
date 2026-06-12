package com.example.answer.dto;

import java.util.List;

public record DiscussionDetailDto(
        DiscussionPostDto post,
        List<DiscussionReplyDto> replies
) {
}
