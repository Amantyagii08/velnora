package com.ruralmarketplace.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
                "discountedProducts", products.findAll().stream().filter(this::publicProduct)
                        .filter(p -> Boolean.TRUE.equals(p.getDiscountActive())).limit(limit).toList(),
                "clearanceDeals", productService.getClearanceDeals(),
                "allShops", shops.findByApprovalStatusAndActiveTrue(ShopApprovalStatus.APPROVED));
    }

    @Transactional(readOnly = true)
    public List<Product> getPopularProducts(int limit) {
        Map<Long, Long> sales = orders.findAll().stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .flatMap(o -> o.getItems().stream())
                .collect(Collectors.groupingBy(item -> item.getProduct().getId(), Collectors.summingLong(OrderItem::getQuantity)));
        return products.findAll().stream().filter(this::publicProduct)
                .sorted(Comparator.<Product>comparingLong(p -> sales.getOrDefault(p.getId(), 0L)).reversed()
                        .thenComparing(Product::getId, Comparator.reverseOrder()))
                .limit(limit).toList();
    }

    @Transactional(readOnly = true)
    public List<Shop> getFeaturedShops(int limit) {
        Map<Long, Long> sales = orders.findAll().stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.groupingBy(o -> o.getShop().getId(), Collectors.counting()));
        return shops.findByApprovalStatusAndActiveTrue(ShopApprovalStatus.APPROVED).stream()
                .sorted(Comparator.<Shop>comparingLong(s -> sales.getOrDefault(s.getId(), 0L)).reversed()
                        .thenComparing(Shop::getId, Comparator.reverseOrder()))
                .limit(limit).toList();
    }

    private boolean publicProduct(Product product) {
        return product.isActive() && product.getShop().isActive()
                && product.getShop().getApprovalStatus() == ShopApprovalStatus.APPROVED;
    }
}
