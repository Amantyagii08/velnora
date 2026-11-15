package com.ruralmarketplace.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ruralmarketplace.entity.StockAdjustment;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {
    @EntityGraph(attributePaths = {"actor"})
    Page<StockAdjustment> findByProduct_IdOrderByCreatedAtDesc(Long productId, Pageable pageable);
}
