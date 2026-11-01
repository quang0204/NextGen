package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    Optional<ProductImage> findFirstByProduct_ProductIdAndIsPrimaryTrue(Long productId);
    Optional<ProductImage> findFirstByProduct_ProductIdOrderByDisplayOrderAsc(Long productId);
}
