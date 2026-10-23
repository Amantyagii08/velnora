
package com.ruralmarketplace.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruralmarketplace.dto.CreateOrderRequest;
import com.ruralmarketplace.entity.FulfillmentType;
import com.ruralmarketplace.entity.Order;
import com.ruralmarketplace.entity.OrderItem;
import com.ruralmarketplace.entity.OrderStatus;
import com.ruralmarketplace.entity.Product;
import com.ruralmarketplace.entity.Shop;
import com.ruralmarketplace.repository.OrderRepository;
import com.ruralmarketplace.repository.ProductRepository;
import com.ruralmarketplace.repository.ShopRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;

    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            ShopRepository shopRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.shopRepository = shopRepository;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {

        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Shop not found: " + request.getShopId()));

        Order order = new Order();
        order.setShop(shop);
        order.setCustomerName(request.getCustomerName().trim());
        order.setCustomerPhone(request.getCustomerPhone().trim());
        order.setDeliveryAddress(
                request.getDeliveryAddress().trim());

        order.setFulfillmentType(
                FulfillmentType.valueOf(
                        request.getFulfillmentType().name()));

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest itemRequest
                : request.getItems()) {

            Product product = productRepository
                    .findById(itemRequest.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Product not found: "
                                    + itemRequest.getProductId()));

            if (!product.getShop().getId().equals(shop.getId())) {
                throw new IllegalArgumentException(
                        "All products must belong to the selected shop");
            }

            if (!Boolean.TRUE.equals(product.getAvailable())) {
                throw new IllegalArgumentException(
                        "Product is unavailable: " + product.getName());
            }

            int quantity = itemRequest.getQuantity();

            if (product.getStockQuantity() == null
                    || product.getStockQuantity() < quantity) {
                throw new IllegalArgumentException(
                        "Insufficient stock for: " + product.getName());
            }

            BigDecimal unitPrice = product.getPrice();

            if (Boolean.TRUE.equals(product.getDiscountActive())
                    && product.getDiscountAmount() != null) {
                unitPrice = unitPrice.subtract(
                        product.getDiscountAmount());
            }

            if (unitPrice.signum() < 0) {
                throw new IllegalArgumentException(
                        "Invalid effective price for: "
                                + product.getName());
            }

            BigDecimal subtotal = unitPrice.multiply(
                    BigDecimal.valueOf(quantity));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(quantity);
            orderItem.setPriceAtPurchase(unitPrice);
            orderItem.setSubtotal(subtotal);

            orderItems.add(orderItem);
            total = total.add(subtotal);

            product.setStockQuantity(
                    product.getStockQuantity() - quantity);

            if (product.getStockQuantity() == 0) {
                product.setAvailable(false);
            }
        }

        order.setTotalAmount(total);
        order.getItems().addAll(orderItems);

        return orderRepository.save(order);
    }

    public List<Order> getOrdersByShop(Long shopId) {

        if (!shopRepository.existsById(shopId)) {
            throw new IllegalArgumentException(
                    "Shop not found: " + shopId);
        }

        return orderRepository
                .findByShop_IdOrderByCreatedAtDesc(shopId);
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Order not found: " + orderId));
    }


@Transactional
public Order updateOrderStatus(
        Long orderId,
        com.ruralmarketplace.entity.OrderStatus newStatus) {

    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException(
                    "Order not found: " + orderId));

    if (newStatus == null) {
        throw new IllegalArgumentException(
                "Order status is required");
    }

    if (order.getStatus() == OrderStatus.COMPLETED
            || order.getStatus() == OrderStatus.CANCELLED) {
        throw new IllegalArgumentException(
                "Completed or cancelled orders cannot be changed");
    }

    if (newStatus == OrderStatus.PLACED) {
        throw new IllegalArgumentException(
                "An order cannot be moved back to PLACED");
    }

    if (newStatus == OrderStatus.CANCELLED) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();

            product.setStockQuantity(
                    product.getStockQuantity() + item.getQuantity());

            product.setAvailable(true);
        }
    }

    order.setStatus(newStatus);
    return orderRepository.save(order);
}

}







