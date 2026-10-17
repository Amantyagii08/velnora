
package com.ruralmarketplace.repository;

import com.ruralmarketplace.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShopRepository extends JpaRepository<Shop, Long> {
    List<Shop> findByNameContainingIgnoreCase(String keyword);

    List<Shop> findByCategory_Id(Long categoryId);
}

