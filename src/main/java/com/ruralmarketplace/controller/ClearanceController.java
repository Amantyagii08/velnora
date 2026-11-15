package com.ruralmarketplace.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.web.bind.annotation.*;
import com.ruralmarketplace.entity.Product;
import com.ruralmarketplace.service.ProductService;

@RestController
@RequestMapping("/api")
public class ClearanceController {
    private final ProductService productService;
    public ClearanceController(ProductService productService) { this.productService = productService; }
    @GetMapping("/clearance-deals") public List<Product> deals() { return productService.getClearanceDeals(); }
    @RequestMapping(value = "/shops/{shopId}/products/{productId}/clearance",
            method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public Product create(@PathVariable Long shopId, @PathVariable Long productId, @RequestParam BigDecimal discountAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiresAt,
            @AuthenticationPrincipal Jwt jwt) {
        return productService.setClearance(shopId, productId, discountAmount, expiresAt, jwt.getSubject());
    }
    @GetMapping("/shops/{shopId}/clearance-deals")
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public List<Product> myDeals(@PathVariable Long shopId, @AuthenticationPrincipal Jwt jwt) {
        return productService.getShopClearanceDeals(shopId, jwt.getSubject());
    }
    @DeleteMapping("/shops/{shopId}/products/{productId}/clearance")
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public Product remove(@PathVariable Long shopId, @PathVariable Long productId, @AuthenticationPrincipal Jwt jwt) {
        return productService.removeClearance(shopId, productId, jwt.getSubject());
    }
}
