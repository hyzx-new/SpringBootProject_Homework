package com.example.answer.controller;

import com.example.answer.common.ApiResponse;
import com.example.answer.dto.PageResult;
import com.example.answer.dto.StudentDto;
import com.example.answer.dto.StudentRequest;
import com.example.answer.service.StudentExportService;
import com.example.answer.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/students")
@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
public class StudentController {

    private final StudentService studentService;
    private final StudentExportService studentExportService;

    public StudentController(StudentService studentService, StudentExportService studentExportService) {
        this.studentService = studentService;
        this.studentExportService = studentExportService;
    }

    @GetMapping
    public ApiResponse<PageResult<StudentDto>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(studentService.list(keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<StudentDto> get(@PathVariable Long id) {
        return ApiResponse.ok(studentService.get(id));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) Long paperId
    ) {
        byte[] bytes = studentExportService.export(keyword, className, paperId);
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename("学生信息.xlsx", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StudentDto> create(@Valid @RequestBody StudentRequest request) {
        return ApiResponse.ok(studentService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StudentDto> update(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return ApiResponse.ok(studentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ApiResponse.ok();
    }
}
