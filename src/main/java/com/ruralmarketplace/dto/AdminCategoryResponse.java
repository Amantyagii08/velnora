package com.ruralmarketplace.dto;

import com.ruralmarketplace.entity.Category;

public record AdminCategoryResponse(Long id, String name, String imagePath, boolean active) {
    public static AdminCategoryResponse from(Category category) {
        return new AdminCategoryResponse(category.getId(), category.getName(), category.getImagePath(),
                category.isActive());
    }
}
