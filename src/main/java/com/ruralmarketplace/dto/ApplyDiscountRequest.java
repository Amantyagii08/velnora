
package com.ruralmarketplace.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ApplyDiscountRequest {

    @NotNull(message = "Discount amount is required")
    @DecimalMin(
            value = "0.01",
            message = "Discount amount must be greater than zero"
    )
    private BigDecimal discountAmount;

    public ApplyDiscountRequest() {
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }
}

