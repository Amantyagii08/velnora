
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

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

    @EntityGraph(attributePaths = {"shop", "customer", "items", "items.product"})
    Optional<Order> findByIdAndShop_Id(Long orderId, Long shopId);

    // Get all orders placed by a particular customer
    @EntityGraph(attributePaths = {"shop", "customer", "items", "items.product"})
    List<Order> findByCustomer_EmailOrderByCreatedAtDesc(
            String email
    );
    long countByStatus(OrderStatus status);
    @EntityGraph(attributePaths = {"shop", "customer", "items", "items.product"})
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    @Query("SELECT p.id, SUM(oi.quantity) FROM OrderItem oi "
            + "JOIN oi.product p JOIN p.shop s WHERE oi.order.status <> :cancelled "
            + "AND p.active = true AND s.active = true AND s.approvalStatus = :approved "
            + "GROUP BY p.id "
            + "ORDER BY SUM(oi.quantity) DESC, p.id DESC")
    List<Object[]> findTopProductSales(@Param("cancelled") OrderStatus cancelled,
            @Param("approved") com.ruralmarketplace.entity.ShopApprovalStatus approved, Pageable pageable);

    @Query("SELECT o.shop.id, COUNT(o.id) FROM Order o "
            + "JOIN o.shop s WHERE o.status <> :cancelled AND s.active = true "
            + "AND s.approvalStatus = :approved GROUP BY s.id "
            + "ORDER BY COUNT(o.id) DESC, o.shop.id DESC")
    List<Object[]> findTopShopOrderCounts(@Param("cancelled") OrderStatus cancelled,
            @Param("approved") com.ruralmarketplace.entity.ShopApprovalStatus approved, Pageable pageable);

    @Query("SELECT o FROM Order o JOIN o.shop s WHERE (:status IS NULL OR o.status = :status) "
            + "AND (:keyword IS NULL OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR (:orderId IS NOT NULL AND o.id = :orderId)) ORDER BY o.createdAt DESC, o.id DESC")
    Page<Order> searchOrders(@Param("status") OrderStatus status, @Param("keyword") String keyword,
            @Param("orderId") Long orderId, Pageable pageable);
}
