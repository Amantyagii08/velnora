
package com.ruralmarketplace.controller;

import java.io.IOException;

import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ruralmarketplace.dto.CreateShopRequest;
import com.ruralmarketplace.dto.ShopApplicationResponse;
import com.ruralmarketplace.dto.ShopResponse;
import com.ruralmarketplace.dto.UpdateShopRequest;
import com.ruralmarketplace.service.ShopService;

import jakarta.validation.Valid;

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

