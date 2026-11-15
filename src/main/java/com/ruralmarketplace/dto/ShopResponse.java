package com.ruralmarketplace.dto;

import com.ruralmarketplace.entity.Shop;

public record ShopResponse(Long id, String name, String description, String address,
        String contactPhone, String contactEmail, String imagePath, Long categoryId,
        String categoryName) {
    public static ShopResponse from(Shop shop) {
        return new ShopResponse(shop.getId(), shop.getName(), shop.getDescription(), shop.getAddress(),
                shop.getContactPhone(), shop.getContactEmail(), shop.getImagePath(),
                shop.getCategory().getId(), shop.getCategory().getName());
    }
}
