package com.ruralmarketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ruralmarketplace.entity.Product;

public record ProductResponse(Long id, String name, String description, String brand, String variant,
        BigDecimal packSizeValue, String packSizeUnit, BigDecimal price, BigDecimal currentPrice,
        Integer stockQuantity, boolean available, String imagePath, Long shopId, String shopName,
        String dealType, BigDecimal discountAmount, LocalDateTime dealExpiresAt) {
    public static ProductResponse from(Product product) {
        boolean clearance = product.hasActiveClearance();
        BigDecimal discount = clearance ? product.getClearanceDiscountAmount()
                : Boolean.TRUE.equals(product.getDiscountActive()) ? product.getDiscountAmount() : BigDecimal.ZERO;
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(),
                product.getBrand(), product.getVariant(), product.getPackSizeValue(), product.getPackSizeUnit(),
                product.getPrice(), product.getCurrentPrice(), product.getStockQuantity(),
                product.isActive() && product.isManuallyAvailable() && product.getStockQuantity() != null
                        && product.getStockQuantity() > 0,
                product.getImagePath(), product.getShop().getId(), product.getShop().getName(),
                clearance ? "CLEARANCE" : Boolean.TRUE.equals(product.getDiscountActive()) ? "DISCOUNT" : null,
                discount, clearance ? product.getClearanceExpiresAt() : null);
    }
}
