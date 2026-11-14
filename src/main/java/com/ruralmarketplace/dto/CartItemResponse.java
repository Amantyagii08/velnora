package com.ruralmarketplace.dto;

import java.math.BigDecimal;
import com.ruralmarketplace.entity.CartItem;

public class CartItemResponse {
    private final Long id;
    private final Long productId;
    private final String productName;
    private final String imagePath;
    private final String brand;
    private final String variant;
    private final Integer quantity;
    private final Integer stockQuantity;
    private final BigDecimal unitPrice;
    private final BigDecimal subtotal;

    public CartItemResponse(CartItem item) {
        var product = item.getProduct();
        id = item.getId(); productId = product.getId(); productName = product.getName();
        imagePath = product.getImagePath(); brand = product.getBrand(); variant = product.getVariant();
        quantity = item.getQuantity(); stockQuantity = product.getStockQuantity();
        unitPrice = product.getCurrentPrice();
        subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
    public Long getId() { return id; } public Long getProductId() { return productId; }
    public String getProductName() { return productName; } public String getImagePath() { return imagePath; }
    public String getBrand() { return brand; } public String getVariant() { return variant; }
    public Integer getQuantity() { return quantity; } public Integer getStockQuantity() { return stockQuantity; }
    public BigDecimal getUnitPrice() { return unitPrice; } public BigDecimal getSubtotal() { return subtotal; }
}
