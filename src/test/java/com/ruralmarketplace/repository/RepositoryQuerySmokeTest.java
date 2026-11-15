package com.ruralmarketplace.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.ruralmarketplace.TestJwtProperties;
import com.ruralmarketplace.entity.OrderStatus;
import com.ruralmarketplace.entity.ShopApprovalStatus;
import com.ruralmarketplace.repository.UserRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:velnora-repositories;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class RepositoryQuerySmokeTest {

    @DynamicPropertySource
    static void configureTestJwtSecret(DynamicPropertyRegistry registry) {
        TestJwtProperties.register(registry);
    }

    @Autowired
    private ProductRepository products;

    @Autowired
    private OrderRepository orders;

    @Autowired
    private ShopRepository shops;

    @Autowired
    private UserRepository users;

    @Test
    void publicCatalogAndHomepageQueriesRunAgainstAnEmptyDatabase() {
        assertTrue(products.findProductsWithFilters(null, null, null, null, false, "DEFAULT").isEmpty());
        assertTrue(products.searchPublicProducts(ShopApprovalStatus.APPROVED, "bread",
                PageRequest.of(0, 8)).isEmpty());
        assertTrue(products.searchAllProducts(null, PageRequest.of(0, 8)).isEmpty());
        assertTrue(products.findPublicProducts(ShopApprovalStatus.APPROVED, PageRequest.of(0, 8)).isEmpty());
        assertTrue(products.findPublicDiscountedProducts(ShopApprovalStatus.APPROVED,
                PageRequest.of(0, 8)).isEmpty());
        assertTrue(products.findPublicClearanceDeals(ShopApprovalStatus.APPROVED,
                PageRequest.of(0, 8)).isEmpty());
        assertTrue(orders.findTopProductSales(OrderStatus.CANCELLED, ShopApprovalStatus.APPROVED,
                PageRequest.of(0, 8)).isEmpty());
        assertTrue(orders.findTopShopOrderCounts(OrderStatus.CANCELLED, ShopApprovalStatus.APPROVED,
                PageRequest.of(0, 8)).isEmpty());
        assertTrue(orders.searchOrders(null, "customer", null, PageRequest.of(0, 8)).isEmpty());
        assertTrue(shops.findByApprovalStatusAndActiveTrue(ShopApprovalStatus.APPROVED,
                PageRequest.of(0, 8)).isEmpty());
        assertTrue(shops.searchPublicShops(ShopApprovalStatus.APPROVED, "market",
                PageRequest.of(0, 8)).isEmpty());
        assertTrue(users.searchUsers(null, "customer", PageRequest.of(0, 8)).isEmpty());
    }
}
