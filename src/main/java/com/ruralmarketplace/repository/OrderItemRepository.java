
package com.ruralmarketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ruralmarketplace.entity.OrderItem;

public interface OrderItemRepository
        extends JpaRepository<OrderItem, Long> {
}

