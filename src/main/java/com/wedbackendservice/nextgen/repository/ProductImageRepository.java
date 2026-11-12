package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    Optional<ProductImage> findFirstByProduct_ProductIdAndIsPrimaryTrue(Long productId);
    Optional<ProductImage> findFirstByProduct_ProductIdOrderByDisplayOrderAsc(Long productId);
    List<ProductImage> findByProduct_ProductIdInAndIsPrimaryTrue(Collection<Long> productIds);
    List<ProductImage> findByProduct_ProductIdOrderByDisplayOrderAsc(Long productId);
    List<ProductImage> findByProduct_ProductIdIn(Collection<Long> productIds);
    void deleteByProduct_ProductId(Long productId);
    Optional<ProductImage> findByImageIdAndProduct_ProductId(Long imageId, Long productId);
    long countByProduct_ProductId(Long productId);

    @Modifying
    @Query("UPDATE ProductImage pi SET pi.isPrimary = false WHERE pi.product.productId = :productId AND pi.isPrimary = true")
    void clearPrimaryByProductId(@Param("productId") Long productId);
}
