
package com.ruralmarketplace.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ruralmarketplace.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByShop_IdOrderByCreatedAtDesc(Long shopId);
}

