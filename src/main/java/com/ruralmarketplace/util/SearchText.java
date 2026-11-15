package com.ruralmarketplace.util;

import java.util.Locale;

public final class SearchText {
    private SearchText() {}

    public static String normalizeAndEscape(String keyword, int maximumLength) {
        if (keyword == null || keyword.isBlank()) return null;
        String normalized = keyword.trim();
        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException("Search keyword cannot exceed " + maximumLength + " characters");
        }
        return normalized.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
