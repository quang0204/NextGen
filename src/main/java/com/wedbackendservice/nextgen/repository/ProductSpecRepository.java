package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.ProductSpec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSpecRepository extends JpaRepository<ProductSpec, Long> {
    List<ProductSpec> findByProduct_ProductId(Long productId);
}
