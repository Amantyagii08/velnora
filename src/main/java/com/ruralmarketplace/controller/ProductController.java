
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

@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }


    @PostMapping(value = "/api/shops/{shopId}/products",
                    consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Product createProduct(
            @PathVariable Long shopId,
            @Valid @RequestPart("products") CreateProductRequest request,
            @RequestPart (value = "image", required = false)
            MultipartFile image)throws IOException {
        return productService.createProduct(shopId, request, image);
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

    
    @PutMapping(value = "/api/shops/{shopId}/products/{productId}",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Product updateProduct(
        @PathVariable Long shopId,
        @PathVariable Long productId,
        @Valid @RequestPart ("product")
        UpdateProductRequest request,
        @RequestPart (value = "image", required = false)
    MultipartFile image) throws IOException{

    return productService.updateProduct(
            shopId,
            productId,
            request,
            image
    );
}
    
    
@DeleteMapping("/api/shops/{shopId}/products/{productId}")
public ResponseEntity<Void> deleteProduct(
        @PathVariable Long shopId,
        @PathVariable Long productId) {

    productService.deleteProduct(shopId, productId);
    return ResponseEntity.noContent().build();
}


@GetMapping("/api/shops/{shopId}/products/{productId}/stock")
public Integer getProductStock(
        @PathVariable Long shopId,
        @PathVariable Long productId) {

    return productService.getProductStock(shopId, productId);
}

@PostMapping("/api/shops/{shopId}/products/{productId}/stock")
public Product addStock(
        @PathVariable Long shopId,
        @PathVariable Long productId,
        @Valid @RequestBody AddStockRequest request) {

    return productService.addStock(shopId, productId, request);
}



@PostMapping("/api/shops/{shopId}/products/{productId}/discount")
public Product applyDiscount(
        @PathVariable Long shopId,
        @PathVariable Long productId,
        @Valid @RequestBody ApplyDiscountRequest request) {

    return productService.applyDiscount(shopId, productId, request);
}

@DeleteMapping("/api/shops/{shopId}/products/{productId}/discount")
public Product removeDiscount(
        @PathVariable Long shopId,
        @PathVariable Long productId) {

    return productService.removeDiscount(shopId, productId);
}

@GetMapping("/filter")
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

}
