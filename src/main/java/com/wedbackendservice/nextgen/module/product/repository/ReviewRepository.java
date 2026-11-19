package com.wedbackendservice.nextgen.module.product.repository;

import com.wedbackendservice.nextgen.module.product.entity.Review;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository
    extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

  @Override
  @EntityGraph(
      attributePaths = {
        "orderItem",
        "orderItem.order",
        "orderItem.order.user",
        "orderItem.variant"
      })
  Page<Review> findAll(Specification<Review> spec, Pageable pageable);

  Optional<Review> findByOrderItem_OrderItemId(Long orderItemId);

  boolean existsByOrderItem_OrderItemId(Long orderItemId);

  @EntityGraph(
      attributePaths = {
        "orderItem",
        "orderItem.order",
        "orderItem.order.user",
        "orderItem.variant"
      })
  Page<Review> findByOrderItem_Variant_Product_ProductIdAndStatus(
      Long productId, String status, Pageable pageable);

  @Query(
      """
            SELECT COALESCE(AVG(r.rating), 0)
            FROM Review r
            WHERE r.orderItem.variant.product.productId = :productId
              AND r.status = 'VISIBLE'
            """)
  Double averageRatingByProductId(@Param("productId") Long productId);

  @Query(
      value =
          """
            SELECT pv.product_product_id AS productId, COUNT(r.review_id) AS reviewCount, AVG(r.rating) AS avgRating
            FROM reviews r
            JOIN order_items oi ON r.order_item_order_item_id = oi.order_item_id
            JOIN product_variants pv ON oi.variant_variant_id = pv.variant_id
            WHERE r.status = 'VISIBLE' AND pv.product_product_id IN (:productIds)
            GROUP BY pv.product_product_id
            """,
      nativeQuery = true)
  java.util.List<Object[]> findRatingStatsByProductIds(
      @Param("productIds") java.util.Collection<Long> productIds);

  @Query(
      """
            SELECT r.orderItem.orderItemId
            FROM Review r
            WHERE r.orderItem.order.user.userId = :userId
              AND r.orderItem.orderItemId IS NOT NULL
            """)
  java.util.List<Long> findReviewedOrderItemIdsByUserId(@Param("userId") Long userId);
}
