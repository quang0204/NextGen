package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    Optional<ProductImage> findFirstByProduct_ProductIdAndIsPrimaryTrue(Long productId);
    Optional<ProductImage> findFirstByProduct_ProductIdOrderByDisplayOrderAsc(Long productId);
    List<ProductImage> findByProduct_ProductIdInAndIsPrimaryTrue(Collection<Long> productIds);
    List<ProductImage> findByProduct_ProductIdOrderByDisplayOrderAsc(Long productId);
    List<ProductImage> findByProduct_ProductIdIn(Collection<Long> productIds);
    long countByProduct_ProductId(Long productId);

    @Query("SELECT pi.objectKey FROM ProductImage pi WHERE pi.bucket = :bucket AND pi.objectKey IS NOT NULL")
    Set<String> findAllObjectKeysByBucket(@Param("bucket") String bucket);

    void deleteByProduct_ProductId(Long productId);
}
