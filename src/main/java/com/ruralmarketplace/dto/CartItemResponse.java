package com.ruralmarketplace.dto;

import java.math.BigDecimal;
import com.ruralmarketplace.entity.CartItem;

public class CartItemResponse {
    private final Long id;
    private final Long productId;
    private final Long shopId;
    private final String shopName;
    private final String productName;
    private final String imagePath;
    private final String brand;
    private final String variant;
    private final Integer quantity;
    private final Integer stockQuantity;
    private final BigDecimal unitPrice;
    private final BigDecimal subtotal;
    private final boolean available;

    public CartItemResponse(CartItem item) {
        var product = item.getProduct();
        id = item.getId(); productId = product.getId(); shopId = product.getShop().getId(); shopName = product.getShop().getName(); productName = product.getName();
        imagePath = product.getImagePath(); brand = product.getBrand(); variant = product.getVariant();
        quantity = item.getQuantity(); stockQuantity = product.getStockQuantity();
        unitPrice = product.getCurrentPrice();
        subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        available = product.isActive() && product.isManuallyAvailable()
                && stockQuantity != null && stockQuantity > 0
                && product.getShop().isActive()
                && product.getShop().getApprovalStatus() == com.ruralmarketplace.entity.ShopApprovalStatus.APPROVED;
    }
    public Long getId() { return id; } public Long getProductId() { return productId; }
    public Long getShopId() { return shopId; } public String getShopName() { return shopName; }
    public String getProductName() { return productName; } public String getImagePath() { return imagePath; }
    public String getBrand() { return brand; } public String getVariant() { return variant; }
    public Integer getQuantity() { return quantity; } public Integer getStockQuantity() { return stockQuantity; }
    public BigDecimal getUnitPrice() { return unitPrice; } public BigDecimal getSubtotal() { return subtotal; }
    public boolean isAvailable() { return available; }
}
