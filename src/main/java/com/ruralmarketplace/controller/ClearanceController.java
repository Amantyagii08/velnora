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
import com.ruralmarketplace.dto.ProductResponse;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api")
public class ClearanceController {
    private final ProductService productService;
    public ClearanceController(ProductService productService) { this.productService = productService; }
    @GetMapping("/clearance-deals") public Page<ProductResponse> deals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productService.getClearanceDealsPage(page, size).map(ProductResponse::from);
    }
    @RequestMapping(value = "/shops/{shopId}/products/{productId}/clearance",
            method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public ProductResponse create(@PathVariable Long shopId, @PathVariable Long productId, @RequestParam BigDecimal discountAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiresAt,
            @AuthenticationPrincipal Jwt jwt) {
        return ProductResponse.from(productService.setClearance(shopId, productId, discountAmount, expiresAt, jwt.getSubject()));
    }
    @GetMapping("/shops/{shopId}/clearance-deals")
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public Page<ProductResponse> myDeals(@PathVariable Long shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        return productService.getShopClearanceDeals(shopId, jwt.getSubject(), page, size)
                .map(ProductResponse::from);
    }
    @DeleteMapping("/shops/{shopId}/products/{productId}/clearance")
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public ProductResponse remove(@PathVariable Long shopId, @PathVariable Long productId, @AuthenticationPrincipal Jwt jwt) {
        return ProductResponse.from(productService.removeClearance(shopId, productId, jwt.getSubject()));
    }
}
