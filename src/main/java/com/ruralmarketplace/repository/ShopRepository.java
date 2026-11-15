
package com.ruralmarketplace.repository;

import com.ruralmarketplace.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ruralmarketplace.entity.ShopApprovalStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    @EntityGraph(attributePaths = {"category", "owner"})
    List<Shop> findByIdIn(Collection<Long> ids);
    List<Shop> findByApprovalStatus(ShopApprovalStatus status);
    long countByApprovalStatus(ShopApprovalStatus status);
    @EntityGraph(attributePaths = {"category", "owner"})
    Page<Shop> findByApprovalStatusAndActiveTrue(ShopApprovalStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "owner"})
    Page<Shop> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = {"category", "owner"})
    Page<Shop> findByCategory_IdAndApprovalStatusAndActiveTrue(Long categoryId,
            ShopApprovalStatus approvalStatus, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "owner"})
    @Query("SELECT s FROM Shop s WHERE s.active = true AND s.approvalStatus = :approved "
            + "AND (:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!')")
    Page<Shop> searchPublicShops(@Param("approved") ShopApprovalStatus approved,
            @Param("keyword") String keyword, Pageable pageable);
}

