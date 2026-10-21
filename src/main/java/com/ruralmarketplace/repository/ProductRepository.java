
package com.ruralmarketplace.repository;

import com.ruralmarketplace.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;


public interface ProductRepository extends JpaRepository<Product, Long> {

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
                WHEN p.discountActive = true
                THEN p.price - p.discountAmount
                ELSE p.price
            END
        ) <= :maxPrice
    )
    AND (:available IS NULL OR p.available = :available)
    AND (:discountedOnly = false OR p.discountActive = true)
    ORDER BY
        CASE
            WHEN :sortBy = 'PRICE_LOW_TO_HIGH'
            THEN
                CASE
                    WHEN p.discountActive = true
                    THEN p.price - p.discountAmount
                    ELSE p.price
                END
        END ASC,
        CASE
            WHEN :sortBy = 'PRICE_HIGH_TO_LOW'
            THEN
                CASE
                    WHEN p.discountActive = true
                    THEN p.price - p.discountAmount
                    ELSE p.price
                END
        END DESC,
        CASE
            WHEN :sortBy = 'TOP_DISCOUNT'
            THEN p.discountAmount
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

