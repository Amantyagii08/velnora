
package com.ruralmarketplace.controller;

import java.util.List;

import com.ruralmarketplace.dto.CreateOrderRequest;
import com.ruralmarketplace.entity.Order;
import com.ruralmarketplace.entity.OrderItem;
import com.ruralmarketplace.service.OrderService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderResponse createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String customerEmail = jwt.getSubject();

        return new OrderResponse(orderService.createOrder(request, customerEmail));
    }

    

    @GetMapping("/shop/{shopId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOPKEEPER')")
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByShop(@PathVariable Long shopId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {

        return orderService.getOrdersByShop(shopId, jwt.getSubject(), pageRequest(page, size)).map(OrderResponse::new);
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('SHOPKEEPER', 'CUSTOMER', 'ADMIN')")
    public OrderResponse updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam com.ruralmarketplace.entity.OrderStatus status,
            @AuthenticationPrincipal Jwt jwt) {

        return new OrderResponse(orderService.updateOrderStatus(orderId, status, jwt.getSubject()));
    }

    
@GetMapping("/my-orders")
@PreAuthorize("hasRole('CUSTOMER')")
@Transactional(readOnly = true)
public Page<OrderResponse> getMyOrders(@RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size, @AuthenticationPrincipal Jwt jwt) {

    return orderService.getMyOrders(jwt.getSubject(), pageRequest(page, size)).map(OrderResponse::new);
}

@GetMapping("/{orderId}")
@PreAuthorize("hasRole('CUSTOMER')")
public OrderResponse getOrderById(
        @PathVariable Long orderId,
        @AuthenticationPrincipal Jwt jwt) {

    return new OrderResponse(orderService.getCustomerOrderById(
            orderId,
            jwt.getSubject()
    ));
}

@GetMapping("/my-shop-orders")
@PreAuthorize("hasRole('SHOPKEEPER')")
@Transactional(readOnly = true)
public Page<OrderResponse> getMyShopOrders(@RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size, @AuthenticationPrincipal Jwt jwt) {
    return orderService.getMyShopOrders(jwt.getSubject(), pageRequest(page, size)).map(OrderResponse::new);
}

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 100));
    }

@GetMapping("/my-shop-orders/{orderId}")
@PreAuthorize("hasRole('SHOPKEEPER')")
public OrderResponse getMyShopOrderById(@PathVariable Long orderId, @AuthenticationPrincipal Jwt jwt) {
    return new OrderResponse(orderService.getMyShopOrderById(orderId, jwt.getSubject()));
}

    public static class OrderResponse {
        private final Long id, shopId;
        private final String shopName, shopAddress, shopContactPhone, shopContactEmail, customerName, customerPhone, fulfillmentType, deliveryAddress, status;
        private final java.math.BigDecimal totalAmount;
        private final java.time.LocalDateTime createdAt;
        private final List<OrderLineResponse> items;
        public OrderResponse(Order order) {
            id = order.getId(); shopId = order.getShop().getId(); shopName = order.getShop().getName(); shopAddress = order.getShop().getAddress();
            shopContactPhone = order.getShop().getContactPhone(); shopContactEmail = order.getShop().getContactEmail();
            customerName = order.getCustomerName(); customerPhone = order.getCustomerPhone(); fulfillmentType = order.getFulfillmentType().name();
            deliveryAddress = order.getDeliveryAddress(); totalAmount = order.getTotalAmount(); status = order.getStatus().name(); createdAt = order.getCreatedAt();
            items = order.getItems().stream().map(OrderLineResponse::new).toList();
        }
        public Long getId() { return id; } public Long getShopId() { return shopId; } public String getShopName() { return shopName; } public String getShopAddress() { return shopAddress; }
        public String getShopContactPhone() { return shopContactPhone; } public String getShopContactEmail() { return shopContactEmail; }
        public String getCustomerName() { return customerName; } public String getCustomerPhone() { return customerPhone; }
        public String getFulfillmentType() { return fulfillmentType; } public String getDeliveryAddress() { return deliveryAddress; }
        public java.math.BigDecimal getTotalAmount() { return totalAmount; } public String getStatus() { return status; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; } public List<OrderLineResponse> getItems() { return items; }
    }
    public static class OrderLineResponse {
        private final Long productId; private final String productName; private final Integer quantity;
        private final java.math.BigDecimal priceAtPurchase, subtotal;
        public OrderLineResponse(OrderItem item) { productId = item.getProduct().getId(); productName = item.getProduct().getName(); quantity = item.getQuantity(); priceAtPurchase = item.getPriceAtPurchase(); subtotal = item.getSubtotal(); }
        public Long getProductId() { return productId; } public String getProductName() { return productName; }
        public Integer getQuantity() { return quantity; } public java.math.BigDecimal getPriceAtPurchase() { return priceAtPurchase; } public java.math.BigDecimal getSubtotal() { return subtotal; }
    }

}
