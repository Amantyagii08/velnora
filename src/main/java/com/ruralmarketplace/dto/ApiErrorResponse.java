package com.ruralmarketplace.dto;

import java.util.Map;

public record ApiErrorResponse(int status, String message, Map<String, String> errors) {
    public static ApiErrorResponse of(int status, String message) {
        return new ApiErrorResponse(status, message, Map.of());
    }
}
