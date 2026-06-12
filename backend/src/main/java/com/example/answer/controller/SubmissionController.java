package com.example.answer.controller;

import com.example.answer.common.ApiResponse;
import com.example.answer.dto.AnswerScoreRequest;
import com.example.answer.dto.PageResult;
import com.example.answer.dto.SubmissionDto;
import com.example.answer.dto.SubmitPaperRequest;
import com.example.answer.service.ScoreExportService;
import com.example.answer.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final ScoreExportService scoreExportService;

    public SubmissionController(SubmissionService submissionService, ScoreExportService scoreExportService) {
        this.submissionService = submissionService;
        this.scoreExportService = scoreExportService;
    }

    @PostMapping("/papers/{paperId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<SubmissionDto> submit(@PathVariable Long paperId, @Valid @RequestBody SubmitPaperRequest request) {
        return ApiResponse.ok(submissionService.submit(paperId, request));
    }

    @GetMapping
    public ApiResponse<PageResult<SubmissionDto>> list(
            @RequestParam(required = false) Long paperId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(submissionService.list(paperId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<SubmissionDto> get(@PathVariable Long id) {
        return ApiResponse.ok(submissionService.get(id));
    }

    @PutMapping("/{submissionId}/answers/{answerId}/score")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<SubmissionDto> updateAnswerScore(
            @PathVariable Long submissionId,
            @PathVariable Long answerId,
            @Valid @RequestBody AnswerScoreRequest request
    ) {
        return ApiResponse.ok(submissionService.updateAnswerScore(submissionId, answerId, request));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) Long paperId,
            @RequestParam(required = false) String className
    ) {
        byte[] bytes = scoreExportService.export(paperId, className);
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename("成绩表.xlsx", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
