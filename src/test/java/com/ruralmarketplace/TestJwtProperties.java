package com.ruralmarketplace;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.test.context.DynamicPropertyRegistry;

public final class TestJwtProperties {

    private static final String SECRET = createSecret();

    private TestJwtProperties() {
    }

    public static void register(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> SECRET);
    }

    private static String createSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
