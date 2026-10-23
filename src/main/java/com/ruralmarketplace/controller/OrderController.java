
package com.ruralmarketplace.controller;

import java.util.List;

import com.ruralmarketplace.dto.CreateOrderRequest;
import com.ruralmarketplace.entity.Order;
import com.ruralmarketplace.service.OrderService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    @GetMapping("/{orderId}")
    public Order getOrderById(
            @PathVariable Long orderId) {
        return orderService.getOrderById(orderId);
    }

    @GetMapping("/shop/{shopId}")
    public List<Order> getOrdersByShop(
            @PathVariable Long shopId) {
        return orderService.getOrdersByShop(shopId);
    }


@PatchMapping("/{orderId}/status")
public Order updateOrderStatus(
        @PathVariable Long orderId,
        @RequestParam com.ruralmarketplace.entity.OrderStatus status) {

    return orderService.updateOrderStatus(orderId, status);
}

}
