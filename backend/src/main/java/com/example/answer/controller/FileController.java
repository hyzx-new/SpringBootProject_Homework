package com.example.answer.controller;

import com.example.answer.common.ApiResponse;
import com.example.answer.dto.PageResult;
import com.example.answer.dto.UploadedFileDto;
import com.example.answer.entity.UploadedFile;
import com.example.answer.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public ApiResponse<PageResult<UploadedFileDto>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(fileStorageService.list(page, size));
    }

    @PostMapping
    public ApiResponse<UploadedFileDto> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(fileStorageService.upload(file));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        UploadedFile uploadedFile = fileStorageService.getFile(id);
        Resource resource = fileStorageService.loadAsResource(uploadedFile);
        MediaType mediaType = StringUtils.hasText(uploadedFile.getContentType())
                ? MediaType.parseMediaType(uploadedFile.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        ContentDisposition contentDisposition = ContentDisposition.inline()
                .filename(uploadedFile.getOriginalName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        fileStorageService.delete(id);
        return ApiResponse.ok();
    }
}
