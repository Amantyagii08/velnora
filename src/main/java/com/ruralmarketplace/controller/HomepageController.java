package com.ruralmarketplace.controller;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruralmarketplace.dto.ProductResponse;
import com.ruralmarketplace.dto.ShopResponse;
import com.ruralmarketplace.service.HomepageService;

@RestController
@RequestMapping("/api/homepage")
public class HomepageController {
    private final HomepageService homepageService;
    public HomepageController(HomepageService homepageService) { this.homepageService = homepageService; }
    @GetMapping public Map<String, Object> getHomepage(@RequestParam(defaultValue = "8") int limit) {
        return homepageService.getHomepage(validateLimit(limit));
    }
    @GetMapping("/popular-products") public List<ProductResponse> popularProducts(@RequestParam(defaultValue = "8") int limit) {
        return homepageService.getPopularProducts(validateLimit(limit));
    }
    @GetMapping("/featured-shops") public List<ShopResponse> featuredShops(@RequestParam(defaultValue = "8") int limit) {
        return homepageService.getFeaturedShops(validateLimit(limit));
    }
    private int validateLimit(int limit) {
        if (limit < 1 || limit > 50) throw new IllegalArgumentException("Limit must be between 1 and 50");
        return limit;
    }
}
