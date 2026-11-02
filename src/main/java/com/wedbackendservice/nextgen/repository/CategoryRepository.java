package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.isActive = true")
    List<Category> findAllActive();

    List<Category> findByParent_CategoryId(Long parentId);

    long countByParent_CategoryId(Long parentId);

    boolean existsByCategoryNameIgnoreCaseAndParentIsNull(String categoryName);

    boolean existsByCategoryNameIgnoreCaseAndParent_CategoryId(String categoryName, Long parentId);

    boolean existsByCategoryNameIgnoreCaseAndParentIsNullAndCategoryIdNot(String categoryName, Long categoryId);

    boolean existsByCategoryNameIgnoreCaseAndParent_CategoryIdAndCategoryIdNot(String categoryName, Long parentId, Long categoryId);

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCaseAndCategoryIdNot(String slug, Long categoryId);
}
