package com.boticario.common.pagination;

import org.springframework.data.domain.Page;

import java.util.List;

/** Envoltorio explicito de paginacion para no acoplar la API al tipo Page de Spring. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
