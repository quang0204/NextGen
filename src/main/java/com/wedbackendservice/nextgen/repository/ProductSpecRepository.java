package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.ProductSpec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProductSpecRepository extends JpaRepository<ProductSpec, Long> {
    List<ProductSpec> findByProduct_ProductId(Long productId);
    List<ProductSpec> findByProduct_ProductIdIn(Collection<Long> productIds);
    List<ProductSpec> findByProduct_ProductIdOrderBySortOrderAsc(Long productId);
    void deleteByProduct_ProductId(Long productId);
}
