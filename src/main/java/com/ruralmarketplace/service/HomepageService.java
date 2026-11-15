package com.ruralmarketplace.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruralmarketplace.dto.CategoryResponse;
import com.ruralmarketplace.dto.ProductResponse;
import com.ruralmarketplace.dto.ShopResponse;
import com.ruralmarketplace.entity.OrderStatus;
import com.ruralmarketplace.entity.Product;
import com.ruralmarketplace.entity.Shop;
import com.ruralmarketplace.entity.ShopApprovalStatus;
import com.ruralmarketplace.repository.CategoryRepository;
import com.ruralmarketplace.repository.OrderRepository;
import com.ruralmarketplace.repository.ProductRepository;
import com.ruralmarketplace.repository.ShopRepository;

@Service
public class HomepageService {
    private final ProductRepository products;
    private final ShopRepository shops;
    private final CategoryRepository categories;
    private final OrderRepository orders;
    private final ProductService productService;

    public HomepageService(ProductRepository products, ShopRepository shops, CategoryRepository categories,
            OrderRepository orders, ProductService productService) {
        this.products = products;
        this.shops = shops;
        this.categories = categories;
        this.orders = orders;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getHomepage(int limit) {
        var page = PageRequest.of(0, limit);
        return Map.of(
                "categories", categories.findByActiveTrue(page).map(CategoryResponse::from).getContent(),
                "popularProducts", getPopularProducts(limit),
                "featuredShops", getFeaturedShops(limit),
                "discountedProducts", products.findPublicDiscountedProducts(ShopApprovalStatus.APPROVED,
                        page).map(ProductResponse::from).getContent(),
                "clearanceDeals", productService.getClearanceDeals(limit).stream().map(ProductResponse::from).toList(),
                "allShops", shops.findByApprovalStatusAndActiveTrue(ShopApprovalStatus.APPROVED, page)
                        .map(ShopResponse::from).getContent());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getPopularProducts(int limit) {
        List<Long> popularIds = orders.findTopProductSales(OrderStatus.CANCELLED, ShopApprovalStatus.APPROVED,
                PageRequest.of(0, limit)).stream().map(row -> ((Number) row[0]).longValue()).toList();
        Map<Long, Product> found = new LinkedHashMap<>();
        products.findByIdIn(popularIds).forEach(product -> found.put(product.getId(), product));
        List<Product> result = new ArrayList<>();
        popularIds.stream().map(found::get).filter(java.util.Objects::nonNull).forEach(result::add);
        if (result.size() < limit) {
            products.findPublicProducts(ShopApprovalStatus.APPROVED, PageRequest.of(0, limit)).getContent().stream()
                    .filter(product -> !found.containsKey(product.getId()))
                    .limit(limit - result.size()).forEach(result::add);
        }
        return result.stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ShopResponse> getFeaturedShops(int limit) {
        List<Long> featuredIds = orders.findTopShopOrderCounts(OrderStatus.CANCELLED,
                ShopApprovalStatus.APPROVED, PageRequest.of(0, limit)).stream()
                .map(row -> ((Number) row[0]).longValue()).toList();
        Map<Long, Shop> found = new LinkedHashMap<>();
        shops.findByIdIn(featuredIds).stream().filter(Shop::isActive)
                .filter(shop -> shop.getApprovalStatus() == ShopApprovalStatus.APPROVED)
                .forEach(shop -> found.put(shop.getId(), shop));
        List<Shop> result = new ArrayList<>();
        featuredIds.stream().map(found::get).filter(java.util.Objects::nonNull).forEach(result::add);
        if (result.size() < limit) {
            shops.findByApprovalStatusAndActiveTrue(ShopApprovalStatus.APPROVED, PageRequest.of(0, limit))
                    .getContent().stream().filter(shop -> !found.containsKey(shop.getId()))
                    .limit(limit - result.size()).forEach(result::add);
        }
        return result.stream().map(ShopResponse::from).toList();
    }
}
