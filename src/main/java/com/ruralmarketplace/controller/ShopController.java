
package com.ruralmarketplace.controller;

import com.ruralmarketplace.dto.CreateShopRequest;
import com.ruralmarketplace.dto.UpdateShopRequest;
import com.ruralmarketplace.entity.Shop;
import com.ruralmarketplace.service.ShopService;
import com.ruralmarketplace.dto.ShopResponse;
import com.ruralmarketplace.dto.ShopApplicationResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.IOException;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api/shops")
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public ShopApplicationResponse createShop(@Valid @RequestPart("shop") CreateShopRequest request,
                            @RequestPart (value = "image",required = false)
                            MultipartFile image,
                            @AuthenticationPrincipal Jwt jwt) throws IOException{
        return ShopApplicationResponse.from(shopService.createShop(request,image,jwt.getSubject()));
    }

    @GetMapping
    public Page<ShopResponse> getAllShops(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return shopService.getAllShops(page, size).map(ShopResponse::from);
    }

    
    @GetMapping("/{shopId}")
    public ShopResponse getShopById(@PathVariable("shopId") Long shopId) {
        return ShopResponse.from(shopService.getShopById(shopId));
    }

    @GetMapping("/my-shop")
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public ShopApplicationResponse getMyShop(@AuthenticationPrincipal Jwt jwt) {
        return ShopApplicationResponse.from(shopService.getMyShop(jwt.getSubject()));
    }

    
    @GetMapping("/search")
    public Page<ShopResponse> searchShops(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return shopService.searchShops(keyword, page, size).map(ShopResponse::from);
    }

    @GetMapping("/search/page")
    public Page<ShopResponse> searchShopsPage(@RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return shopService.searchPublicShops(keyword, page, size).map(ShopResponse::from);
    }

    
    @GetMapping("/category/{categoryId}")
    public Page<ShopResponse> getShopsByCategory(@PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return shopService.getShopsByCategory(categoryId, page, size).map(ShopResponse::from);
    }

    @PutMapping(
    value = "/{shopId}",
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE
)
@PreAuthorize("hasRole('SHOPKEEPER')")
public ShopApplicationResponse updateShop(
        @PathVariable Long shopId,
        @Valid @RequestPart("shop")
        UpdateShopRequest request,
        @RequestPart(value = "image", required = false)
        MultipartFile image,
        @AuthenticationPrincipal Jwt jwt) throws IOException {

    return ShopApplicationResponse.from(shopService.updateShop(
            shopId, request, image, jwt.getSubject()));
}

}

