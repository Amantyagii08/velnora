
package com.ruralmarketplace.controller;

import com.ruralmarketplace.dto.CreateProductRequest;
import com.ruralmarketplace.entity.Product;
import com.ruralmarketplace.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }


    @PostMapping("/api/shops/{shopId}/products")
    public Product createProduct(
            @PathVariable Long shopId,
            @Valid @RequestBody CreateProductRequest request) {
        return productService.createProduct(shopId, request);
    }

    
    @GetMapping("/api/shops/{shopId}/products")
    public List<Product> getProductsByShop(
            @PathVariable Long shopId) {
        return productService.getProductsByShop(shopId);
    }

    
    @GetMapping("/api/products/{productId}")
    public Product getProductById(
            @PathVariable Long productId) {
        return productService.getProductById(productId);
    }

    
    @GetMapping("/api/products/search")
    public List<Product> searchProducts(
            @RequestParam(required = false) String keyword) {
        return productService.searchProducts(keyword);
    }

    
    @GetMapping("/api/shops/{shopId}/products/search")
    public List<Product> searchProductsByShop(
            @PathVariable Long shopId,
            @RequestParam(required = false) String keyword) {
        return productService.searchProductsByShop(shopId, keyword);
    }
}
