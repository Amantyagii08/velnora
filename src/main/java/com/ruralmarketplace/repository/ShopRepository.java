
package com.ruralmarketplace.repository;

import com.ruralmarketplace.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;
import com.ruralmarketplace.entity.ShopApprovalStatus;

public interface ShopRepository extends JpaRepository<Shop, Long> {
    @Override
    @EntityGraph(attributePaths = {"category", "owner"})
    List<Shop> findAll();
    @Override
    @EntityGraph(attributePaths = {"category", "owner"})
    java.util.Optional<Shop> findById(Long id);
    List<Shop> findByNameContainingIgnoreCase(String keyword);

    List<Shop> findByCategory_Id(Long categoryId);
    List<Shop> findByApprovalStatusAndActiveTrue(ShopApprovalStatus status);
    Optional<Shop> findByOwner_Id(Long ownerId);
    List<Shop> findByApprovalStatus(ShopApprovalStatus status);
}

