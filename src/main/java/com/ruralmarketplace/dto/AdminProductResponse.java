package com.ruralmarketplace.dto;

import java.math.BigDecimal;
import com.ruralmarketplace.entity.Product;

public record AdminProductResponse(Long id, String name, String description, String brand,
        String variant, BigDecimal price, BigDecimal currentPrice, Integer stockQuantity,
        boolean available, boolean active, String imagePath, Long shopId, String shopName) {
    public static AdminProductResponse from(Product product) {
        return new AdminProductResponse(product.getId(), product.getName(), product.getDescription(),
                product.getBrand(), product.getVariant(), product.getPrice(), product.getCurrentPrice(),
                product.getStockQuantity(), Boolean.TRUE.equals(product.getAvailable()), product.isActive(),
                product.getImagePath(), product.getShop().getId(), product.getShop().getName());
    }
}
