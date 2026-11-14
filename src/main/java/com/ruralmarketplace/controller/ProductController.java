
package com.ruralmarketplace.controller;

import com.ruralmarketplace.dto.AddStockRequest;
import com.ruralmarketplace.dto.ApplyDiscountRequest;
import com.ruralmarketplace.dto.CreateProductRequest;
import com.ruralmarketplace.entity.Product;
import com.ruralmarketplace.service.ProductService;
import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.ruralmarketplace.dto.UpdateProductRequest;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }


    @PostMapping(value = "/api/shops/{shopId}/products",
                    consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public Product createProduct(
            @PathVariable Long shopId,
            @Valid @RequestPart("products") CreateProductRequest request,
            @RequestPart (value = "image", required = false)
            MultipartFile image,
            @AuthenticationPrincipal Jwt jwt)throws IOException {
        return productService.createProduct(shopId, request, image, jwt.getSubject());
    }

    
    @GetMapping("/api/shops/{shopId}/products")
    public List<Product> getProductsByShop(
            @PathVariable Long shopId) {
        return productService.getProductsByShop(shopId);
    }

    
    @GetMapping("/api/products/{productId}")
    public Product getProductById(
            @PathVariable Long productId) {
        Product product = productService.getProductById(productId);
        if (!product.isActive() || !product.getShop().isActive()
                || product.getShop().getApprovalStatus() != com.ruralmarketplace.entity.ShopApprovalStatus.APPROVED) {
            throw new com.ruralmarketplace.exception.ProductNotFoundException("Product not found with ID: " + productId);
        }
        return product;
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

    
    @PutMapping(value = "/api/shops/{shopId}/products/{productId}",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public Product updateProduct(
        @PathVariable Long shopId,
        @PathVariable Long productId,
        @Valid @RequestPart ("product")
        UpdateProductRequest request,
        @RequestPart (value = "image", required = false)
    MultipartFile image, @AuthenticationPrincipal Jwt jwt) throws IOException{

    return productService.updateProduct(
            shopId,
            productId,
            request,
            image, jwt.getSubject()
    );
}
    
    
@DeleteMapping("/api/shops/{shopId}/products/{productId}")
@PreAuthorize("hasRole('SHOPKEEPER')")
public ResponseEntity<Void> deleteProduct(
        @PathVariable Long shopId,
        @PathVariable Long productId, @AuthenticationPrincipal Jwt jwt) {

    productService.deleteProduct(shopId, productId, jwt.getSubject());
    return ResponseEntity.noContent().build();
}


@GetMapping("/api/shops/{shopId}/products/{productId}/stock")
@PreAuthorize("hasRole('SHOPKEEPER')")
public Integer getProductStock(
        @PathVariable Long shopId,
        @PathVariable Long productId, @AuthenticationPrincipal Jwt jwt) {

    return productService.getProductStock(shopId, productId, jwt.getSubject());
}

@PostMapping("/api/shops/{shopId}/products/{productId}/stock")
@PreAuthorize("hasRole('SHOPKEEPER')")
public Product addStock(
        @PathVariable Long shopId,
        @PathVariable Long productId,
        @Valid @RequestBody AddStockRequest request, @AuthenticationPrincipal Jwt jwt) {

    return productService.addStock(shopId, productId, request, jwt.getSubject());
}

@PatchMapping("/api/shops/{shopId}/products/{productId}/availability")
@PreAuthorize("hasRole('SHOPKEEPER')")
public Product setAvailability(@PathVariable Long shopId, @PathVariable Long productId,
        @RequestParam boolean available, @AuthenticationPrincipal Jwt jwt) {
    return productService.setAvailability(shopId, productId, available, jwt.getSubject());
}



@PostMapping("/api/shops/{shopId}/products/{productId}/discount")
@PreAuthorize("hasRole('SHOPKEEPER')")
public Product applyDiscount(
        @PathVariable Long shopId,
        @PathVariable Long productId,
        @Valid @RequestBody ApplyDiscountRequest request, @AuthenticationPrincipal Jwt jwt) {

    return productService.applyDiscount(shopId, productId, request, jwt.getSubject());
}

@DeleteMapping("/api/shops/{shopId}/products/{productId}/discount")
@PreAuthorize("hasRole('SHOPKEEPER')")
public Product removeDiscount(
        @PathVariable Long shopId,
        @PathVariable Long productId, @AuthenticationPrincipal Jwt jwt) {

    return productService.removeDiscount(shopId, productId, jwt.getSubject());
}

@GetMapping("/api/products/filter")
    public List<Product> filterAndSortProducts(
        @RequestParam(required = false) Long shopId,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) Boolean available,
        @RequestParam(defaultValue = "false") boolean discountedOnly,
        @RequestParam(required = false) String sortBy) {

    return productService.filterAndSortProducts(
            shopId,
            minPrice,
            maxPrice,
            available,
            discountedOnly,
            sortBy
    );
}

@GetMapping("/api/products/compare")
public List<Map<String, Object>> compareProducts(@RequestParam String brand, @RequestParam String variant) {
    return productService.compareProducts(brand, variant);
}

}
