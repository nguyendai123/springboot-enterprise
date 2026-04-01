package com.enterprise.app.model.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

// ── Page DTO ─────────────────────────────────────────────────────────────
@Data
@Builder
public class PageDto<T> {
    private List<T> content;
    private int     totalPages;
    private long    totalElements;
    private int     currentPage;
    private int     pageSize;
    private boolean first;
    private boolean last;

    public static <T> PageDto<T> from(Page<T> page) {
        return PageDto.<T>builder()
                .content(page.getContent())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
