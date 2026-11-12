
package com.ruralmarketplace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ruralmarketplace.entity.Order;
import com.ruralmarketplace.entity.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Get all orders belonging to a particular shop
    List<Order> findByShop_IdOrderByCreatedAtDesc(Long shopId);

    // Find an order only if it belongs to the given customer
    Optional<Order> findByIdAndCustomer_Email(
            Long orderId,
            String email
    );

    // Get all orders placed by a particular customer
    List<Order> findByCustomer_EmailOrderByCreatedAtDesc(
            String email
    );
    long countByStatus(OrderStatus status);
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
}
