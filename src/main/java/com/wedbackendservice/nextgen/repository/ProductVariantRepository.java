package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    Optional<ProductVariant> findBySku(String sku);
    List<ProductVariant> findByProduct_ProductId(Long productId);
    List<ProductVariant> findByProduct_ProductIdIn(Collection<Long> productIds);
}
