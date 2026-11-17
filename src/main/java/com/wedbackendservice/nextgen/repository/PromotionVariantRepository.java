package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.PromotionVariant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionVariantRepository
    extends JpaRepository<PromotionVariant, Long>, JpaSpecificationExecutor<PromotionVariant> {

  List<PromotionVariant> findByPromotion_PromotionId(Long promotionId);

  @Query(
      "SELECT pv FROM PromotionVariant pv JOIN FETCH pv.variant v LEFT JOIN FETCH v.product WHERE pv.promotion.promotionId IN :promotionIds")
  List<PromotionVariant> findByPromotionIdInWithVariantAndProduct(
      @Param("promotionIds") Collection<Long> promotionIds);

  long countByPromotion_PromotionId(Long promotionId);

  @Query(
      "SELECT pv.promotion.promotionId, COUNT(pv) FROM PromotionVariant pv WHERE pv.promotion.promotionId IN :promotionIds GROUP BY pv.promotion.promotionId")
  List<Object[]> countByPromotionIds(@Param("promotionIds") Collection<Long> promotionIds);

  void deleteByPromotion_PromotionId(Long promotionId);

  @Query(
      "SELECT pv FROM PromotionVariant pv "
          + "JOIN pv.promotion p "
          + "WHERE pv.variant.variantId = :variantId "
          + "AND p.isActive = true AND (p.status IS NULL OR UPPER(p.status) NOT IN ('PAUSED', 'ENDED')) "
          + "AND (p.startDate IS NULL OR p.startDate <= :now) "
          + "AND (p.endDate IS NULL OR p.endDate >= :now) "
          + "ORDER BY pv.promotionalPrice ASC")
  List<PromotionVariant> findActivePromotionsForVariant(
      @Param("variantId") Long variantId, @Param("now") LocalDateTime now);

  @Query(
      "SELECT pv FROM PromotionVariant pv "
          + "JOIN pv.promotion p "
          + "WHERE pv.variant.variantId IN :variantIds "
          + "AND p.isActive = true AND (p.status IS NULL OR UPPER(p.status) NOT IN ('PAUSED', 'ENDED')) "
          + "AND (p.startDate IS NULL OR p.startDate <= :now) "
          + "AND (p.endDate IS NULL OR p.endDate >= :now) "
          + "ORDER BY pv.promotionalPrice ASC")
  List<PromotionVariant> findActivePromotionsForVariantIds(
      @Param("variantIds") Collection<Long> variantIds, @Param("now") LocalDateTime now);

  @Query(
      "SELECT pv FROM PromotionVariant pv "
          + "JOIN FETCH pv.promotion p "
          + "JOIN FETCH pv.variant v "
          + "LEFT JOIN FETCH v.product prod "
          + "WHERE (v.variantId IN :variantIds OR prod.productId IN :productIds) "
          + "AND (p.status IS NULL OR UPPER(p.status) <> 'ENDED') "
          + "AND (:excludePromoId IS NULL OR p.promotionId <> :excludePromoId) "
          + "AND (p.endDate IS NULL OR (p.endDate > :now AND (:newStartAt IS NULL OR p.endDate > :newStartAt))) "
          + "AND (p.startDate IS NULL OR :newEndAt IS NULL OR p.startDate < :newEndAt)")
  List<PromotionVariant> findConflictingPromotionVariants(
      @Param("variantIds") Collection<Long> variantIds,
      @Param("productIds") Collection<Long> productIds,
      @Param("excludePromoId") Long excludePromoId,
      @Param("now") LocalDateTime now,
      @Param("newStartAt") LocalDateTime newStartAt,
      @Param("newEndAt") LocalDateTime newEndAt);
}
