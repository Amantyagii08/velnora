package com.ruralmarketplace.dto;

import com.ruralmarketplace.entity.Shop;

public record ShopApplicationResponse(ShopResponse shop, String approvalStatus, String rejectionReason) {
    public static ShopApplicationResponse from(Shop shop) {
        return new ShopApplicationResponse(ShopResponse.from(shop), shop.getApprovalStatus().name(),
                shop.getRejectionReason());
    }
}
