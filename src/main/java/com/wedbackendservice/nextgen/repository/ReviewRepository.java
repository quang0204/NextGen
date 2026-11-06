package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    Optional<Review> findByOrderItem_OrderItemId(Long orderItemId);

    boolean existsByOrderItem_OrderItemId(Long orderItemId);

    Page<Review> findByOrderItem_Variant_Product_ProductIdAndStatus(
            Long productId, String status, Pageable pageable);

    @Query("""
            SELECT COALESCE(AVG(r.rating), 0)
            FROM Review r
            WHERE r.orderItem.variant.product.productId = :productId
              AND r.status = 'VISIBLE'
            """)
    Double averageRatingByProductId(@Param("productId") Long productId);
}
