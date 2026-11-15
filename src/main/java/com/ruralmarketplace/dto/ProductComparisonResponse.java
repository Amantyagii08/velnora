package com.ruralmarketplace.dto;

import java.math.BigDecimal;

import com.ruralmarketplace.entity.Product;

public record ProductComparisonResponse(Long productId, String productName, String brand,
        String variant, BigDecimal packSizeValue, String packSizeUnit, Long shopId, String shopName,
        BigDecimal originalPrice, BigDecimal currentPrice, boolean available) {
    public static ProductComparisonResponse from(Product product) {
        return new ProductComparisonResponse(product.getId(), product.getName(), product.getBrand(),
                product.getVariant(), product.getPackSizeValue(), product.getPackSizeUnit(),
                product.getShop().getId(), product.getShop().getName(), product.getPrice(),
                product.getCurrentPrice(), product.isManuallyAvailable() && product.getStockQuantity() != null
                        && product.getStockQuantity() > 0);
    }
}
