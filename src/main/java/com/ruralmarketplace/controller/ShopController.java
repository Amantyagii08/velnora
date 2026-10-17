
package com.ruralmarketplace.controller;

import com.ruralmarketplace.dto.CreateShopRequest;
import com.ruralmarketplace.entity.Shop;
import com.ruralmarketplace.service.ShopService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/shops")
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @PostMapping
    public Shop createShop(@Valid @RequestBody CreateShopRequest request) {
        return shopService.createShop(request);
    }

    @GetMapping
    public List<Shop> getAllShops() {
        return shopService.getAllShops();
    }

    
    @GetMapping("/{id}")
    public Shop getShopById(@PathVariable Long id) {
        return shopService.getShopById(id);
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

}

