package com.example.answer.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResult<T>(
        List<T> records,
        long total,
        int page,
        int size,
        int pages
) {

    public static <T> PageResult<T> from(Page<T> pageData) {
        return new PageResult<>(
                pageData.getContent(),
                pageData.getTotalElements(),
                pageData.getNumber() + 1,
                pageData.getSize(),
                pageData.getTotalPages()
        );
    }
}
