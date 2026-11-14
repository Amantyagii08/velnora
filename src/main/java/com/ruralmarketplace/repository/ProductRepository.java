
package com.ruralmarketplace.repository;

import com.ruralmarketplace.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;

import java.math.BigDecimal;
import java.util.List;


public interface ProductRepository extends JpaRepository<Product, Long> {

    @Override
    @EntityGraph(attributePaths = {"shop", "shop.category", "shop.owner"})
    List<Product> findAll();
    @Override
    @EntityGraph(attributePaths = {"shop", "shop.category", "shop.owner"})
    java.util.Optional<Product> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :productId")
    java.util.Optional<Product> findByIdForUpdate(@Param("productId") Long productId);

    List<Product> findByShop_Id(Long shopId);

    List<Product> findByNameContainingIgnoreCase(String keyword);

    List<Product> findByShop_IdAndNameContainingIgnoreCase(
            Long shopId, String keyword);

            @Query("""
    SELECT p FROM Product p
    WHERE (:shopId IS NULL OR p.shop.id = :shopId)
    AND (
        :minPrice IS NULL OR
        (
            CASE
                WHEN p.clearanceActive = true AND (p.clearanceExpiresAt IS NULL OR p.clearanceExpiresAt > CURRENT_TIMESTAMP)
                THEN p.price - p.clearanceDiscountAmount
                WHEN p.discountActive = true
                THEN p.price - p.discountAmount
                ELSE p.price
            END
        ) >= :minPrice
    )
    AND (
        :maxPrice IS NULL OR
        (
            CASE
                WHEN p.clearanceActive = true AND (p.clearanceExpiresAt IS NULL OR p.clearanceExpiresAt > CURRENT_TIMESTAMP)
                THEN p.price - p.clearanceDiscountAmount
                WHEN p.discountActive = true
                THEN p.price - p.discountAmount
                ELSE p.price
            END
        ) <= :maxPrice
    )
    AND (:available IS NULL OR (:available = true AND p.available = true AND p.stockQuantity > 0)
        OR (:available = false AND (p.available = false OR p.stockQuantity = 0)))
    AND (:discountedOnly = false OR p.discountActive = true OR
        (p.clearanceActive = true AND (p.clearanceExpiresAt IS NULL OR p.clearanceExpiresAt > CURRENT_TIMESTAMP)))
    ORDER BY
        CASE
            WHEN :sortBy = 'PRICE_LOW_TO_HIGH'
            THEN
                CASE
                    WHEN p.clearanceActive = true AND (p.clearanceExpiresAt IS NULL OR p.clearanceExpiresAt > CURRENT_TIMESTAMP)
                    THEN p.price - p.clearanceDiscountAmount
                    WHEN p.discountActive = true
                    THEN p.price - p.discountAmount
                    ELSE p.price
                END
        END ASC,
        CASE
            WHEN :sortBy = 'PRICE_HIGH_TO_LOW'
            THEN
                CASE
                    WHEN p.clearanceActive = true AND (p.clearanceExpiresAt IS NULL OR p.clearanceExpiresAt > CURRENT_TIMESTAMP)
                    THEN p.price - p.clearanceDiscountAmount
                    WHEN p.discountActive = true
                    THEN p.price - p.discountAmount
                    ELSE p.price
                END
        END DESC,
        CASE
            WHEN :sortBy = 'TOP_DISCOUNT'
            THEN CASE WHEN p.clearanceActive = true AND (p.clearanceExpiresAt IS NULL OR p.clearanceExpiresAt > CURRENT_TIMESTAMP) THEN p.clearanceDiscountAmount ELSE p.discountAmount END
        END DESC,
        p.id ASC
    """)
List<Product> findProductsWithFilters(
    @Param("shopId") Long shopId,
    @Param("minPrice") BigDecimal minPrice,
    @Param("maxPrice") BigDecimal maxPrice,
    @Param("available") Boolean available,
    @Param("discountedOnly") boolean discountedOnly,
    @Param("sortBy") String sortBy
);
}

