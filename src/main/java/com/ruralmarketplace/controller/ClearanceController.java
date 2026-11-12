package com.ruralmarketplace.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import com.ruralmarketplace.service.ShopService;
import org.springframework.web.bind.annotation.*;
import com.ruralmarketplace.entity.Product;
import com.ruralmarketplace.service.ProductService;

@RestController
@RequestMapping("/api")
public class ClearanceController {
    private final ProductService productService;
    private final ShopService shopService;
    public ClearanceController(ProductService productService, ShopService shopService) { this.productService = productService; this.shopService = shopService; }
    @GetMapping("/clearance-deals") public List<Product> deals() { return productService.getClearanceDeals(); }
    @PostMapping("/shops/{shopId}/products/{productId}/clearance")
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public Product create(@PathVariable Long shopId, @PathVariable Long productId, @RequestParam BigDecimal discountAmount,
            @RequestParam(required = false) LocalDateTime expiresAt, @AuthenticationPrincipal Jwt jwt) {
        shopService.requireShopOwner(shopId, jwt.getSubject());
        return productService.setClearance(shopId, productId, discountAmount, expiresAt, jwt.getSubject());
    }
    @DeleteMapping("/shops/{shopId}/products/{productId}/clearance")
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public Product remove(@PathVariable Long shopId, @PathVariable Long productId, @AuthenticationPrincipal Jwt jwt) {
        shopService.requireShopOwner(shopId, jwt.getSubject());
        return productService.removeClearance(shopId, productId, jwt.getSubject());
    }
}
