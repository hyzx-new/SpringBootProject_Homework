package com.example.answer.controller;

import com.example.answer.common.ApiResponse;
import com.example.answer.dto.DiscussionDetailDto;
import com.example.answer.dto.DiscussionPostDto;
import com.example.answer.dto.DiscussionPostRequest;
import com.example.answer.dto.DiscussionReplyRequest;
import com.example.answer.dto.PageResult;
import com.example.answer.service.DiscussionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discussions")
public class DiscussionController {

    private final DiscussionService discussionService;

    public DiscussionController(DiscussionService discussionService) {
        this.discussionService = discussionService;
    }

    @GetMapping
    public ApiResponse<PageResult<DiscussionPostDto>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long paperId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "3") int size
    ) {
        return ApiResponse.ok(discussionService.list(keyword, paperId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<DiscussionDetailDto> get(@PathVariable Long id) {
        return ApiResponse.ok(discussionService.get(id));
    }

    @PostMapping
    public ApiResponse<DiscussionDetailDto> create(@Valid @RequestBody DiscussionPostRequest request) {
        return ApiResponse.ok(discussionService.create(request));
    }

    @PostMapping("/{id}/replies")
    public ApiResponse<DiscussionDetailDto> reply(@PathVariable Long id, @Valid @RequestBody DiscussionReplyRequest request) {
        return ApiResponse.ok(discussionService.reply(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePost(@PathVariable Long id) {
        discussionService.deletePost(id);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{postId}/replies/{replyId}")
    public ApiResponse<DiscussionDetailDto> deleteReply(@PathVariable Long postId, @PathVariable Long replyId) {
        return ApiResponse.ok(discussionService.deleteReply(postId, replyId));
    }
}
