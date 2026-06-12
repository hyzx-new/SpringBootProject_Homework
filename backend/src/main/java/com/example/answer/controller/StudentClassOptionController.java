package com.example.answer.controller;

import com.example.answer.common.ApiResponse;
import com.example.answer.dto.StudentClassOptionDto;
import com.example.answer.dto.StudentClassOptionRequest;
import com.example.answer.service.StudentClassOptionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student-class-options")
@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
public class StudentClassOptionController {

    private final StudentClassOptionService studentClassOptionService;

    public StudentClassOptionController(StudentClassOptionService studentClassOptionService) {
        this.studentClassOptionService = studentClassOptionService;
    }

    @GetMapping
    public ApiResponse<List<StudentClassOptionDto>> list() {
        return ApiResponse.ok(studentClassOptionService.list());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StudentClassOptionDto> create(@Valid @RequestBody StudentClassOptionRequest request) {
        return ApiResponse.ok(studentClassOptionService.create(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        studentClassOptionService.delete(id);
        return ApiResponse.ok();
    }
}
