package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    long countByCategory_CategoryId(Long categoryId);
    long countByBrand_BrandId(Long brandId);

    @Query("SELECT p.category.categoryId, COUNT(p) FROM Product p WHERE p.category IS NOT NULL GROUP BY p.category.categoryId")
    List<Object[]> countProductsGroupedByCategory();

    @Query("SELECT p.brand.brandId, COUNT(p) FROM Product p WHERE p.brand IS NOT NULL GROUP BY p.brand.brandId")
    List<Object[]> countProductsGroupedByBrand();
}
