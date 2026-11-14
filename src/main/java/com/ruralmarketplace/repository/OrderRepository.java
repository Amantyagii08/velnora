
package com.ruralmarketplace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ruralmarketplace.entity.Order;
import com.ruralmarketplace.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Override
    @EntityGraph(attributePaths = {"shop", "customer", "items", "items.product"})
    List<Order> findAll();

    @Override
    @EntityGraph(attributePaths = {"shop", "customer", "items", "items.product"})
    Optional<Order> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :orderId")
    Optional<Order> findByIdForUpdate(@Param("orderId") Long orderId);

    // Get all orders belonging to a particular shop
    @EntityGraph(attributePaths = {"shop", "customer", "items", "items.product"})
    List<Order> findByShop_IdOrderByCreatedAtDesc(Long shopId);

    // Find an order only if it belongs to the given customer
    @EntityGraph(attributePaths = {"shop", "customer", "items", "items.product"})
    Optional<Order> findByIdAndCustomer_Email(
            Long orderId,
            String email
    );

    // Get all orders placed by a particular customer
    @EntityGraph(attributePaths = {"shop", "customer", "items", "items.product"})
    List<Order> findByCustomer_EmailOrderByCreatedAtDesc(
            String email
    );
    long countByStatus(OrderStatus status);
    @EntityGraph(attributePaths = {"shop", "customer", "items", "items.product"})
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
}
