package com.ruralmarketplace.dto;

import java.time.LocalDateTime;
import com.ruralmarketplace.entity.User;

public record AdminUserResponse(Long id, String fullName, String email, String phone,
        String role, boolean suspended, LocalDateTime createdAt) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getRole().name(), user.isSuspended(), user.getCreatedAt());
    }
}
