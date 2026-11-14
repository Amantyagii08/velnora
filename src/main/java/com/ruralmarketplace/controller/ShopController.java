
package com.ruralmarketplace.controller;

import com.ruralmarketplace.dto.CreateShopRequest;
import com.ruralmarketplace.dto.UpdateShopRequest;
import com.ruralmarketplace.entity.Shop;
import com.ruralmarketplace.service.ShopService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.IOException;
import java.util.List;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/shops")
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public Shop createShop(@Valid @RequestPart("shop") CreateShopRequest request,
                            @RequestPart (value = "image",required = false)
                            MultipartFile image,
                            @AuthenticationPrincipal Jwt jwt) throws IOException{
        return shopService.createShop(request,image,jwt.getSubject());
    }

    @GetMapping
    public List<Shop> getAllShops() {
        return shopService.getAllShops();
    }

    
    @GetMapping("/{shopId}")
    public Shop getShopById(@PathVariable("shopId") Long shopId) {
        return shopService.getShopById(shopId);
    }

    @GetMapping("/my-shop")
    @PreAuthorize("hasRole('SHOPKEEPER')")
    public Shop getMyShop(@AuthenticationPrincipal Jwt jwt) {
        return shopService.getMyShop(jwt.getSubject());
    }

    
    @GetMapping("/search")
    public List<Shop> searchShops(
            @RequestParam(required = false) String keyword) {
        return shopService.searchShops(keyword);
    }

    
    @GetMapping("/category/{categoryId}")
    public List<Shop> getShopsByCategory(
            @PathVariable Long categoryId) {
        return shopService.getShopsByCategory(categoryId);
    }

    @PutMapping(
    value = "/{shopId}",
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE
)
@PreAuthorize("hasRole('SHOPKEEPER')")
public Shop updateShop(
        @PathVariable Long shopId,
        @Valid @RequestPart("shop")
        UpdateShopRequest request,
        @RequestPart(value = "image", required = false)
        MultipartFile image,
        @AuthenticationPrincipal Jwt jwt) throws IOException {

    return shopService.updateShop(
            shopId, request, image, jwt.getSubject());
}

}

