package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long>, JpaSpecificationExecutor<ProductVariant> {
    Optional<ProductVariant> findBySku(String sku);
    boolean existsBySku(String sku);
    boolean existsBySkuAndVariantIdNot(String sku, Long variantId);
    List<ProductVariant> findByProduct_ProductId(Long productId);
    List<ProductVariant> findByProduct_ProductIdAndStatus(Long productId, String status);
    List<ProductVariant> findByProduct_ProductIdIn(Collection<Long> productIds);
    List<ProductVariant> findByProduct_ProductIdInAndStatus(Collection<Long> productIds, String status);
}
