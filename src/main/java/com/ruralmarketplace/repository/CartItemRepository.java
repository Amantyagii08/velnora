package com.ruralmarketplace.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ruralmarketplace.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCustomer_Email(String email);
    Optional<CartItem> findByCustomer_EmailAndProduct_Id(String email, Long productId);
    void deleteByCustomer_Email(String email);
}
