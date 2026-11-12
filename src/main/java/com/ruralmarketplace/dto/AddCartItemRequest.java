package com.ruralmarketplace.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class AddCartItemRequest {
    @NotNull @Positive private Long productId;
    @NotNull @Positive private Integer quantity;
    private boolean clearExistingCart;
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public boolean isClearExistingCart() { return clearExistingCart; }
    public void setClearExistingCart(boolean clearExistingCart) { this.clearExistingCart = clearExistingCart; }
}
