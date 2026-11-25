
package com.ruralmarketplace.controller;

import java.io.IOException;
import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ruralmarketplace.dto.AddStockRequest;
import com.ruralmarketplace.dto.ApplyDiscountRequest;
import com.ruralmarketplace.dto.CreateProductRequest;
import com.ruralmarketplace.dto.ProductComparisonResponse;
import com.ruralmarketplace.dto.ProductResponse;
import com.ruralmarketplace.dto.SetStockRequest;
import com.ruralmarketplace.dto.StockAdjustmentResponse;
import com.ruralmarketplace.dto.UpdateProductRequest;
import com.ruralmarketplace.entity.Product;
import com.ruralmarketplace.service.ProductService;

import jakarta.validation.Valid;

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
    public ProductResponse createProduct(
            @PathVariable Long shopId,
            @Valid @RequestPart("products") CreateProductRequest request,
            @RequestPart (value = "image", required = false)
            MultipartFile image,
            @AuthenticationPrincipal Jwt jwt)throws IOException {
        return ProductResponse.from(productService.createProduct(shopId, request, image, jwt.getSubject()));
    }

    
    @GetMapping("/api/shops/{shopId}/products")
    public Page<ProductResponse> getProductsByShop(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productService.getProductsByShop(shopId, page, size).map(ProductResponse::from);
    }

    
    @GetMapping("/api/products/{productId}")
    public ProductResponse getProductById(
            @PathVariable Long productId) {
        Product product = productService.getProductById(productId);
        if (!product.isActive() || !product.getShop().isActive()
                || product.getShop().getApprovalStatus() != com.ruralmarketplace.entity.ShopApprovalStatus.APPROVED) {
            throw new com.ruralmarketplace.exception.ProductNotFoundException("Product not found with ID: " + productId);
        }
        return ProductResponse.from(product);
    }

    
    @GetMapping("/api/products/search")
    public Page<ProductResponse> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productService.searchProducts(keyword, page, size).map(ProductResponse::from);
    }

    @GetMapping("/api/products/search/page")
    public Page<ProductResponse> searchProductsPage(@RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productService.searchPublicProducts(keyword, page, size).map(ProductResponse::from);
    }

    
    @GetMapping("/api/shops/{shopId}/products/search")
    public Page<ProductResponse> searchProductsByShop(
            @PathVariable Long shopId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productService.searchProductsByShop(shopId, keyword, page, size).map(ProductResponse::from);
    }

    
    @PutMapping(value = "/api/shops/{shopId}/products/{productId}",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public ProductResponse updateProduct(
        @PathVariable Long shopId,
        @PathVariable Long productId,
        @Valid @RequestPart ("product")
        UpdateProductRequest request,
        @RequestPart (value = "image", required = false)
    MultipartFile image, @AuthenticationPrincipal Jwt jwt) throws IOException{

    return ProductResponse.from(productService.updateProduct(
            shopId,
            productId,
            request,
            image, jwt.getSubject()
    ));
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
public ProductResponse addStock(
        @PathVariable Long shopId,
        @PathVariable Long productId,
        @Valid @RequestBody AddStockRequest request, @AuthenticationPrincipal Jwt jwt) {

    return ProductResponse.from(productService.addStock(shopId, productId, request, jwt.getSubject()));
}

@PutMapping("/api/shops/{shopId}/products/{productId}/stock")
@PreAuthorize("hasRole('SHOPKEEPER')")
public ProductResponse setStock(@PathVariable Long shopId, @PathVariable Long productId,
        @Valid @RequestBody SetStockRequest request, @AuthenticationPrincipal Jwt jwt) {
    return ProductResponse.from(productService.setStock(shopId, productId, request, jwt.getSubject()));
}

@GetMapping("/api/shops/{shopId}/products/{productId}/stock/history")
@PreAuthorize("hasRole('SHOPKEEPER')")
public Page<StockAdjustmentResponse> stockHistory(@PathVariable Long shopId,
        @PathVariable Long productId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @AuthenticationPrincipal Jwt jwt) {
    return productService.getStockHistory(shopId, productId, jwt.getSubject(), page, size)
            .map(StockAdjustmentResponse::from);
}

@PatchMapping("/api/shops/{shopId}/products/{productId}/availability")
@PreAuthorize("hasRole('SHOPKEEPER')")
public ProductResponse setAvailability(@PathVariable Long shopId, @PathVariable Long productId,
        @RequestParam boolean available, @AuthenticationPrincipal Jwt jwt) {
    return ProductResponse.from(productService.setAvailability(shopId, productId, available, jwt.getSubject()));
}



@PostMapping("/api/shops/{shopId}/products/{productId}/discount")
@PreAuthorize("hasRole('SHOPKEEPER')")
public ProductResponse applyDiscount(
        @PathVariable Long shopId,
        @PathVariable Long productId,
        @Valid @RequestBody ApplyDiscountRequest request, @AuthenticationPrincipal Jwt jwt) {

    return ProductResponse.from(productService.applyDiscount(shopId, productId, request, jwt.getSubject()));
}

@DeleteMapping("/api/shops/{shopId}/products/{productId}/discount")
@PreAuthorize("hasRole('SHOPKEEPER')")
public ProductResponse removeDiscount(
        @PathVariable Long shopId,
        @PathVariable Long productId, @AuthenticationPrincipal Jwt jwt) {

    return ProductResponse.from(productService.removeDiscount(shopId, productId, jwt.getSubject()));
}

@GetMapping("/api/products/filter")
    public Page<ProductResponse> filterAndSortProducts(
        @RequestParam(required = false) Long shopId,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) Boolean available,
        @RequestParam(defaultValue = "false") boolean discountedOnly,
        @RequestParam(required = false) String sortBy,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

    return productService.filterAndSortProducts(
            shopId,
            minPrice,
            maxPrice,
            available,
            discountedOnly,
            sortBy,
            page,
            size
    ).map(ProductResponse::from);
}

@GetMapping("/api/products/compare")
public Page<ProductComparisonResponse> compareProducts(@RequestParam String brand,
        @RequestParam String name, @RequestParam String variant,
        @RequestParam BigDecimal packSizeValue, @RequestParam String packSizeUnit,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    return productService.compareProducts(brand, name, variant, packSizeValue, packSizeUnit, page, size)
            .map(ProductComparisonResponse::from);
}

}
