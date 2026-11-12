
package com.ruralmarketplace.repository;

import com.ruralmarketplace.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import com.ruralmarketplace.entity.ShopApprovalStatus;

public interface ShopRepository extends JpaRepository<Shop, Long> {
    List<Shop> findByNameContainingIgnoreCase(String keyword);

    List<Shop> findByCategory_Id(Long categoryId);
    List<Shop> findByApprovalStatusAndActiveTrue(ShopApprovalStatus status);
    Optional<Shop> findByOwner_Id(Long ownerId);
    List<Shop> findByApprovalStatus(ShopApprovalStatus status);
}

