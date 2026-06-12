package com.example.answer.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageUtils {

    private PageUtils() {
    }

    public static Pageable of(int page, int size, Sort sort) {
        int pageIndex = Math.max(page, 1) - 1;
        int pageSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(pageIndex, pageSize, sort);
    }
}
