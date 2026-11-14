
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
import com.ruralmarketplace.entity.ShopApprovalStatus;
import com.ruralmarketplace.entity.User;
import com.ruralmarketplace.repository.OrderRepository;
import com.ruralmarketplace.repository.ProductRepository;
import com.ruralmarketplace.repository.ShopRepository;
import com.ruralmarketplace.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import com.ruralmarketplace.exception.ResourceNotFoundException;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;

    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            ShopRepository shopRepository,
            UserRepository userRepository) {

        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Order createOrder(
            CreateOrderRequest request,
            String customerEmail) {

        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated customer not found"));

        if (customer.getRole() != com.ruralmarketplace.entity.UserRole.CUSTOMER) {
            throw new IllegalArgumentException(
                    "Only customers can place orders");
        }

        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Shop not found: " + request.getShopId()));
        if (shop.getApprovalStatus() != ShopApprovalStatus.APPROVED || !shop.isActive()) {
            throw new IllegalArgumentException("Orders can only be placed with approved active shops");
        }
        if (request.getFulfillmentType() == CreateOrderRequest.FulfillmentType.DELIVERY
                && (request.getDeliveryAddress() == null || request.getDeliveryAddress().isBlank())) {
            throw new IllegalArgumentException("Delivery address is required for delivery orders");
        }

        Order order = new Order();
        order.setShop(shop);
        order.setCustomer(customer);

        // Customer details come from the authenticated account.
        order.setCustomerName(customer.getFullName());
        order.setCustomerPhone(customer.getPhone());

        order.setDeliveryAddress(request.getFulfillmentType() == CreateOrderRequest.FulfillmentType.PICKUP
                ? null : request.getDeliveryAddress().trim());

        order.setFulfillmentType(
                FulfillmentType.valueOf(
                        request.getFulfillmentType().name()));

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest itemRequest
                : request.getItems().stream()
                        .sorted(java.util.Comparator.comparing(CreateOrderRequest.OrderItemRequest::getProductId))
                        .toList()) {

            int quantity = itemRequest.getQuantity();

            if (quantity <= 0) {
                throw new IllegalArgumentException(
                        "Quantity must be greater than zero");
            }

            Product product = productRepository
                    .findByIdForUpdate(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found: "
                                    + itemRequest.getProductId()));

            if (!product.getShop().getId().equals(shop.getId())) {
                throw new IllegalArgumentException(
                        "All products must belong to the selected shop");
            }

            if (!product.isManuallyAvailable() || product.getStockQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Product is unavailable: " + product.getName());
            }

            if (product.getStockQuantity() == null
                    || product.getStockQuantity() < quantity) {
                throw new IllegalArgumentException(
                        "Insufficient stock for: " + product.getName());
            }

            if (!product.isActive()) throw new IllegalArgumentException("Product is unavailable: " + product.getName());
            BigDecimal unitPrice = product.getCurrentPrice();

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

        }

        order.setTotalAmount(total);
        order.getItems().addAll(orderItems);

        return orderRepository.save(order);
    }

    public List<Order> getOrdersByShop(Long shopId, String actorEmail) {

        if (!shopRepository.existsById(shopId)) {
            throw new ResourceNotFoundException(
                    "Shop not found: " + shopId);
        }

        User actor = userRepository.findByEmail(actorEmail).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new ResourceNotFoundException("Shop not found"));
        if (actor.getRole() != com.ruralmarketplace.entity.UserRole.ADMIN
                && (actor.getRole() != com.ruralmarketplace.entity.UserRole.SHOPKEEPER
                || !shop.getOwner().getId().equals(actor.getId()))) {
            throw new AccessDeniedException("You can only view orders for your own shop");
        }

        if (actor.getRole() == com.ruralmarketplace.entity.UserRole.SHOPKEEPER
                && shop.getApprovalStatus() != ShopApprovalStatus.APPROVED) {
            throw new IllegalArgumentException("Shop application is not approved yet");
        }
        return orderRepository.findByShop_IdOrderByCreatedAtDesc(shopId);
    }

    
    

    @Transactional
    public Order updateOrderStatus(
            Long orderId,
            OrderStatus newStatus,
            String actorEmail) {

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
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

        User actor = userRepository.findByEmail(actorEmail).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        boolean admin = actor.getRole() == com.ruralmarketplace.entity.UserRole.ADMIN;
        boolean owner = actor.getRole() == com.ruralmarketplace.entity.UserRole.SHOPKEEPER
                && order.getShop().getOwner().getId().equals(actor.getId());
        boolean customer = actor.getRole() == com.ruralmarketplace.entity.UserRole.CUSTOMER
                && order.getCustomer().getId().equals(actor.getId());
        if (!admin && !owner && !customer) throw new AccessDeniedException("You cannot update this order");

        if (newStatus == OrderStatus.CANCELLED) {
            if (!admin && !(customer && order.getStatus() == OrderStatus.PLACED)
                    && !(owner && (order.getStatus() == OrderStatus.PLACED || order.getStatus() == OrderStatus.ACCEPTED))) {
                throw new AccessDeniedException("This order can no longer be cancelled");
            }
        } else {
            if (!admin && !owner) throw new AccessDeniedException("Only the shopkeeper can update order progress");
            OrderStatus expected = switch (order.getStatus()) {
                case PLACED -> OrderStatus.ACCEPTED;
                case ACCEPTED -> OrderStatus.PREPARING;
                case PREPARING -> OrderStatus.READY;
                case READY -> OrderStatus.COMPLETED;
                default -> null;
            };
            if (newStatus != expected) throw new IllegalArgumentException("Order status must follow the required progress sequence");
        }

        if (newStatus == OrderStatus.CANCELLED) {

            List<OrderItem> items = order.getItems().stream()
                    .sorted(java.util.Comparator.comparing(item -> item.getProduct().getId()))
                    .toList();
            for (OrderItem item : items) {
                Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ordered product no longer exists"));
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            }
        }

        order.setStatus(newStatus);

        Order savedOrder = orderRepository.save(order);
        initializeOrderResponseFields(savedOrder);
        return savedOrder;
    }


public List<Order> getMyOrders(String customerEmail) {

    User customer = userRepository.findByEmail(customerEmail)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Customer not found"));

    if (customer.getRole() != com.ruralmarketplace.entity.UserRole.CUSTOMER) {
        throw new IllegalArgumentException(
                "Only customers can access customer order history");
    }

    return orderRepository
            .findByCustomer_EmailOrderByCreatedAtDesc(customerEmail);
}

private void initializeOrderResponseFields(Order order) {
    order.getShop().getName();
    order.getShop().getContactPhone();
    order.getShop().getContactEmail();
    order.getItems().forEach(item -> item.getProduct().getName());
}

public Order getCustomerOrderById(
        Long orderId,
        String customerEmail) {

    return orderRepository
            .findByIdAndCustomer_Email(orderId, customerEmail)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Order not found for this customer"));
}

public List<Order> getMyShopOrders(String email) {
    User owner = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Shopkeeper not found"));
    if (owner.getRole() != com.ruralmarketplace.entity.UserRole.SHOPKEEPER) throw new IllegalArgumentException("Only shopkeepers can access shop orders");
    Shop shop = shopRepository.findByOwner_Id(owner.getId()).orElseThrow(() -> new ResourceNotFoundException("No shop application found"));
    return orderRepository.findByShop_IdOrderByCreatedAtDesc(shop.getId());
}

}
