package com.ruralmarketplace.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruralmarketplace.entity.Product;
import com.ruralmarketplace.entity.StockAdjustment;
import com.ruralmarketplace.entity.User;
import com.ruralmarketplace.repository.StockAdjustmentRepository;

@Service
public class StockAdjustmentService {
    private final StockAdjustmentRepository repository;

    public StockAdjustmentService(StockAdjustmentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(Product product, User actor, int oldQuantity, int newQuantity, String reason) {
        if (oldQuantity == newQuantity) return;
        repository.save(new StockAdjustment(product, actor, oldQuantity, newQuantity, reason));
    }

    @Transactional(readOnly = true)
    public Page<StockAdjustment> history(Long productId, Pageable pageable) {
        return repository.findByProduct_IdOrderByCreatedAtDesc(productId, pageable);
    }
}
