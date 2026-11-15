package com.ruralmarketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.ruralmarketplace.entity.Product;
import com.ruralmarketplace.service.ProductService;
import com.ruralmarketplace.controller.ClearanceController;
import java.util.concurrent.atomic.AtomicReference;

class ClearanceDateTimeBindingTest {

    @Test
    void parsesIsoLocalDateTimeRequestParameter() throws Exception {
        AtomicReference<LocalDateTime> receivedExpiry = new AtomicReference<>();
        Product product = new Product();
        ProductService productService = new ProductService(null, null, null, null) {
            @Override
            public Product setClearance(Long shopId, Long productId, BigDecimal amount,
                    LocalDateTime expiresAt, String ownerEmail) {
                receivedExpiry.set(expiresAt);
                return product;
            }
        };

        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none")
                .subject("admin@example.com").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        try {
            MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                    new ClearanceController(productService))
                    .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                    .build();
            mockMvc.perform(post("/api/shops/7/products/12/clearance")
                    .param("discountAmount", "5.00")
                    .param("expiresAt", "2026-11-14T22:30:00"))
                    .andExpect(status().isOk());

            assertEquals(LocalDateTime.of(2026, 11, 14, 22, 30), receivedExpiry.get());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
