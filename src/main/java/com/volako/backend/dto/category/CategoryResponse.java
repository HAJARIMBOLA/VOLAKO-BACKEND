package com.volako.backend.dto.category;

import com.volako.backend.domain.Category;
import com.volako.backend.domain.enums.TransactionType;

public record CategoryResponse(
        Long id,
        String name,
        TransactionType type,
        boolean isSystem,
        boolean active
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.isSystem(),
                category.isActive()
        );
    }
}
