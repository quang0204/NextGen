package com.wedbackendservice.nextgen.module.product.repository;

import com.wedbackendservice.nextgen.module.product.entity.Brand;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

  @EntityGraph(attributePaths = {"categories"})
  @Query("SELECT b FROM Brand b WHERE b.isActive = true")
  List<Brand> findAllActive();

  @EntityGraph(attributePaths = {"categories"})
  @Override
  List<Brand> findAll();

  @EntityGraph(attributePaths = {"categories"})
  @Override
  Optional<Brand> findById(Long id);

  @EntityGraph(attributePaths = {"categories"})
  @Query("SELECT b FROM Brand b JOIN b.categories c WHERE c.categoryId = :categoryId AND b.isActive = true")
  List<Brand> findByCategoryIdActive(Long categoryId);

  boolean existsByBrandNameIgnoreCase(String brandName);

  boolean existsByBrandNameIgnoreCaseAndBrandIdNot(String brandName, Long brandId);

  boolean existsBySlugIgnoreCase(String slug);

  boolean existsBySlugIgnoreCaseAndBrandIdNot(String slug, Long brandId);
}
