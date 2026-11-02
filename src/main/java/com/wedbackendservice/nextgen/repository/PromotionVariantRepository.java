package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.PromotionVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface PromotionVariantRepository extends JpaRepository<PromotionVariant, Long> {

    @Query("SELECT pv FROM PromotionVariant pv " +
           "JOIN pv.promotion p " +
           "WHERE pv.variant.variantId = :variantId " +
           "AND p.isActive = true " +
           "AND (p.startDate IS NULL OR p.startDate <= :now) " +
           "AND (p.endDate IS NULL OR p.endDate >= :now) " +
           "ORDER BY pv.promotionalPrice ASC")
    List<PromotionVariant> findActivePromotionsForVariant(
            @Param("variantId") Long variantId,
            @Param("now") LocalDateTime now);

    @Query("SELECT pv FROM PromotionVariant pv " +
           "JOIN pv.promotion p " +
           "WHERE pv.variant.variantId IN :variantIds " +
           "AND p.isActive = true " +
           "AND (p.startDate IS NULL OR p.startDate <= :now) " +
           "AND (p.endDate IS NULL OR p.endDate >= :now) " +
           "ORDER BY pv.promotionalPrice ASC")
    List<PromotionVariant> findActivePromotionsForVariantIds(
            @Param("variantIds") Collection<Long> variantIds,
            @Param("now") LocalDateTime now);
}
