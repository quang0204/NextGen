package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    long countByCategory_CategoryId(Long categoryId);
    long countByBrand_BrandId(Long brandId);

    @Query("SELECT p.category.categoryId, COUNT(p) FROM Product p WHERE p.category IS NOT NULL GROUP BY p.category.categoryId")
    List<Object[]> countProductsGroupedByCategory();

    @Query("SELECT p.brand.brandId, COUNT(p) FROM Product p WHERE p.brand IS NOT NULL GROUP BY p.brand.brandId")
    List<Object[]> countProductsGroupedByBrand();

    Optional<Product> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndProductIdNot(String slug, Long productId);

    @Query("SELECT p FROM Product p WHERE p.productId <> :productId AND p.status = 'ACTIVE' " +
           "AND (:categoryId IS NULL OR p.category.categoryId = :categoryId OR :brandId IS NULL OR p.brand.brandId = :brandId) " +
           "ORDER BY CASE WHEN :categoryId IS NOT NULL AND p.category.categoryId = :categoryId THEN 0 ELSE 1 END, p.createdAt DESC")
    List<Product> findRelatedProducts(@Param("productId") Long productId,
                                      @Param("categoryId") Long categoryId,
                                      @Param("brandId") Long brandId,
                                      Pageable pageable);

    @Query("SELECT p.productId, SUM(oi.quantity) as totalSold " +
           "FROM OrderItem oi " +
           "JOIN oi.variant v " +
           "JOIN v.product p " +
           "WHERE p.status = 'ACTIVE' " +
           "GROUP BY p.productId " +
           "ORDER BY totalSold DESC")
    List<Object[]> findBestSellingProductIds(Pageable pageable);

    List<Product> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
}
