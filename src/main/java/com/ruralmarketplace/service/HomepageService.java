package com.ruralmarketplace.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruralmarketplace.entity.*;
import com.ruralmarketplace.repository.*;

@Service
public class HomepageService {
    private final ProductRepository products;
    private final ShopRepository shops;
    private final CategoryRepository categories;
    private final OrderRepository orders;
    private final ProductService productService;

    public HomepageService(ProductRepository products, ShopRepository shops, CategoryRepository categories,
            OrderRepository orders, ProductService productService) {
        this.products = products; this.shops = shops; this.categories = categories; this.orders = orders; this.productService = productService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getHomepage(int limit) {
        return Map.of(
                "categories", categories.findByActiveTrue(),
                "popularProducts", getPopularProducts(limit),
                "featuredShops", getFeaturedShops(limit),
                "discountedProducts", products.findPublicDiscountedProducts(ShopApprovalStatus.APPROVED,
                        PageRequest.of(0, limit)).getContent(),
                "clearanceDeals", productService.getClearanceDeals(limit),
                "allShops", shops.findByApprovalStatusAndActiveTrue(ShopApprovalStatus.APPROVED,
                        PageRequest.of(0, limit)).getContent());
    }

    @Transactional(readOnly = true)
    public List<Product> getPopularProducts(int limit) {
        List<Long> popularIds = orders.findTopProductSales(OrderStatus.CANCELLED, ShopApprovalStatus.APPROVED,
                PageRequest.of(0, limit)).stream().map(row -> ((Number) row[0]).longValue()).toList();
        Map<Long, Product> found = new LinkedHashMap<>();
        products.findAllById(popularIds).forEach(product -> found.put(product.getId(), product));
        List<Product> result = popularIds.stream().map(found::get).filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
        if (result.size() < limit) {
            products.findPublicProducts(ShopApprovalStatus.APPROVED, PageRequest.of(0, limit)).getContent().stream()
                    .filter(product -> !found.containsKey(product.getId()))
                    .limit(limit - result.size()).forEach(result::add);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Shop> getFeaturedShops(int limit) {
        List<Long> featuredIds = orders.findTopShopOrderCounts(OrderStatus.CANCELLED,
                ShopApprovalStatus.APPROVED, PageRequest.of(0, limit)).stream()
                .map(row -> ((Number) row[0]).longValue()).toList();
        Map<Long, Shop> found = new LinkedHashMap<>();
        shops.findAllById(featuredIds).forEach(shop -> found.put(shop.getId(), shop));
        List<Shop> result = featuredIds.stream().map(found::get).filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
        if (result.size() < limit) {
            shops.findByApprovalStatusAndActiveTrue(ShopApprovalStatus.APPROVED, PageRequest.of(0, limit))
                    .getContent().stream().filter(shop -> !found.containsKey(shop.getId()))
                    .limit(limit - result.size()).forEach(result::add);
        }
        return result;
    }
}
