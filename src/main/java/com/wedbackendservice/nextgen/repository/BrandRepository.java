package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.Brand;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

  @Query("SELECT b FROM Brand b WHERE b.isActive = true")
  List<Brand> findAllActive();

  boolean existsByBrandNameIgnoreCase(String brandName);

  boolean existsByBrandNameIgnoreCaseAndBrandIdNot(String brandName, Long brandId);

  boolean existsBySlugIgnoreCase(String slug);

  boolean existsBySlugIgnoreCaseAndBrandIdNot(String slug, Long brandId);
}
