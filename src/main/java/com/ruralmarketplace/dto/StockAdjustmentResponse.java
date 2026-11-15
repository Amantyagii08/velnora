package com.ruralmarketplace.dto;

import java.time.LocalDateTime;

import com.ruralmarketplace.entity.StockAdjustment;

public record StockAdjustmentResponse(Long id, int previousQuantity, int newQuantity,
        String reason, LocalDateTime createdAt, String changedBy) {
    public static StockAdjustmentResponse from(StockAdjustment adjustment) {
        return new StockAdjustmentResponse(adjustment.getId(), adjustment.getPreviousQuantity(),
                adjustment.getNewQuantity(), adjustment.getReason(), adjustment.getCreatedAt(),
                adjustment.getActor().getEmail());
    }
}
