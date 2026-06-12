package com.example.answer.controller;

import com.example.answer.common.ApiResponse;
import com.example.answer.dto.PageResult;
import com.example.answer.dto.PaperDto;
import com.example.answer.dto.PaperRequest;
import com.example.answer.dto.QuestionDto;
import com.example.answer.dto.QuestionRequest;
import com.example.answer.service.PaperService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/papers")
public class PaperController {

    private final PaperService paperService;

    public PaperController(PaperService paperService) {
        this.paperService = paperService;
    }
    // 分页
    @GetMapping
    public ApiResponse<PageResult<PaperDto>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(paperService.list(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<PaperDto> get(@PathVariable Long id) {
        return ApiResponse.ok(paperService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<PaperDto> create(@Valid @RequestBody PaperRequest request) {
        return ApiResponse.ok(paperService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<PaperDto> update(@PathVariable Long id, @Valid @RequestBody PaperRequest request) {
        return ApiResponse.ok(paperService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        paperService.delete(id);
        return ApiResponse.ok();
    }

    @GetMapping("/{paperId}/questions")
    public ApiResponse<List<QuestionDto>> questions(@PathVariable Long paperId) {
        return ApiResponse.ok(paperService.questions(paperId));
    }

    @PostMapping("/{paperId}/questions")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<QuestionDto> createQuestion(@PathVariable Long paperId, @Valid @RequestBody QuestionRequest request) {
        return ApiResponse.ok(paperService.createQuestion(paperId, request));
    }

    @PutMapping("/{paperId}/questions/{questionId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<QuestionDto> updateQuestion(
            @PathVariable Long paperId,
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionRequest request
    ) {
        return ApiResponse.ok(paperService.updateQuestion(paperId, questionId, request));
    }

    @DeleteMapping("/{paperId}/questions/{questionId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Void> deleteQuestion(@PathVariable Long paperId, @PathVariable Long questionId) {
        paperService.deleteQuestion(paperId, questionId);
        return ApiResponse.ok();
    }
}
