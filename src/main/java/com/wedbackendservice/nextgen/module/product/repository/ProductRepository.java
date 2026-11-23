package com.wedbackendservice.nextgen.module.product.repository;

import com.wedbackendservice.nextgen.module.product.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository
    extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
  long countByCategory_CategoryId(Long categoryId);

  long countByBrand_BrandId(Long brandId);

  @Query(
      "SELECT p.category.categoryId, COUNT(p) FROM Product p WHERE p.category IS NOT NULL GROUP BY p.category.categoryId")
  List<Object[]> countProductsGroupedByCategory();

  @Query(
      "SELECT p.brand.brandId, COUNT(p) FROM Product p WHERE p.brand IS NOT NULL GROUP BY p.brand.brandId")
  List<Object[]> countProductsGroupedByBrand();

  Optional<Product> findBySlug(String slug);

  boolean existsBySlug(String slug);

  boolean existsBySlugAndProductIdNot(String slug, Long productId);

  @Query(
      "SELECT DISTINCT p FROM Product p "
          + "LEFT JOIN p.category c "
          + "LEFT JOIN c.parent parent "
          + "WHERE p.productId <> :productId AND p.status = 'ACTIVE' "
          + "AND ("
          + "  (:subCategoryId IS NOT NULL AND c.categoryId = :subCategoryId) "
          + "  OR (:parentCategoryId IS NOT NULL AND (c.categoryId = :parentCategoryId OR parent.categoryId = :parentCategoryId))"
          + ") "
          + "ORDER BY CASE WHEN :subCategoryId IS NOT NULL AND c.categoryId = :subCategoryId THEN 0 ELSE 1 END, p.createdAt DESC")
  List<Product> findRelatedProducts(
      @Param("productId") Long productId,
      @Param("subCategoryId") Long subCategoryId,
      @Param("parentCategoryId") Long parentCategoryId,
      Pageable pageable);

  @Query(
      "SELECT p.productId, SUM(oi.quantity) as totalSold "
          + "FROM OrderItem oi "
          + "JOIN oi.variant v "
          + "JOIN v.product p "
          + "WHERE p.status = 'ACTIVE' "
          + "GROUP BY p.productId "
          + "ORDER BY totalSold DESC")
  List<Object[]> findBestSellingProductIds(Pageable pageable);

  List<Product> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
}
