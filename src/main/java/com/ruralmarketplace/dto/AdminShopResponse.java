package com.ruralmarketplace.dto;

import com.ruralmarketplace.entity.Shop;

public record AdminShopResponse(Long id, String name, String description, String address,
        Long categoryId, String contactPhone, String contactEmail, String imagePath,
        Long ownerId, String ownerName, String ownerEmail, String approvalStatus, boolean active) {
    public static AdminShopResponse from(Shop shop) {
        return new AdminShopResponse(shop.getId(), shop.getName(), shop.getDescription(), shop.getAddress(),
                shop.getCategory().getId(), shop.getContactPhone(), shop.getContactEmail(), shop.getImagePath(),
                shop.getOwner() == null ? null : shop.getOwner().getId(),
                shop.getOwner() == null ? null : shop.getOwner().getFullName(),
                shop.getOwner() == null ? null : shop.getOwner().getEmail(),
                shop.getApprovalStatus().name(), shop.isActive());
    }
}
