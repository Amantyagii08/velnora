package com.ruralmarketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
import com.ruralmarketplace.controller.ClearanceController;
import com.ruralmarketplace.entity.Product;
import com.ruralmarketplace.service.ProductService;
import com.ruralmarketplace.service.ShopService;

class ClearanceDateTimeBindingTest {

    @Test
    void parsesIsoLocalDateTimeRequestParameter() throws Exception {
        ProductService productService = mock(ProductService.class);
        ShopService shopService = mock(ShopService.class);
        Product product = new Product();
        org.mockito.Mockito.when(productService.setClearance(eq(7L), eq(12L), eq(new BigDecimal("5.00")),
                eq(LocalDateTime.of(2026, 11, 14, 22, 30)), eq("admin@example.com"))).thenReturn(product);

        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none")
                .subject("admin@example.com").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        try {
            MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                    new ClearanceController(productService, shopService))
                    .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                    .build();
            mockMvc.perform(post("/api/shops/7/products/12/clearance")
                    .param("discountAmount", "5.00")
                    .param("expiresAt", "2026-11-14T22:30:00"))
                    .andExpect(status().isOk());

            verify(productService).setClearance(eq(7L), eq(12L), eq(new BigDecimal("5.00")),
                    eq(LocalDateTime.of(2026, 11, 14, 22, 30)), eq("admin@example.com"));
            assertEquals(LocalDateTime.of(2026, 11, 14, 22, 30),
                    LocalDateTime.parse("2026-11-14T22:30:00"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
