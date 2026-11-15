package com.ruralmarketplace.dto;

import com.ruralmarketplace.entity.Category;

public record CategoryResponse(Long id, String name, String imagePath) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getImagePath());
    }
}
