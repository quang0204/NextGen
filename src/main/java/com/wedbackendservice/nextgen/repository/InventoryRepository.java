package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {
    Optional<Inventory> findByVariant_VariantId(Long variantId);

    List<Inventory> findByVariant_VariantIdIn(Collection<Long> variantIds);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Inventory i SET i.quantity = i.quantity + :qty, i.updatedAt = :now WHERE i.variant.variantId = :variantId")
    int increaseQuantityAtomic(@Param("variantId") Long variantId, @Param("qty") int qty, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Inventory i SET i.reservedQuantity = i.reservedQuantity + :qty, i.updatedAt = :now " +
            "WHERE i.variant.variantId = :variantId AND (i.quantity - i.reservedQuantity) >= :qty")
    int reserveStockAtomic(@Param("variantId") Long variantId, @Param("qty") int qty, @Param("now") LocalDateTime now);

    @Query("SELECT COALESCE(SUM(i.quantity), 0), COALESCE(SUM(i.reservedQuantity), 0), " +
            "COALESCE(SUM(CASE WHEN (i.quantity - i.reservedQuantity) > 0 THEN (i.quantity - i.reservedQuantity) ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN (i.quantity - i.reservedQuantity) <= COALESCE(i.minThreshold, 5) THEN 1 ELSE 0 END), 0) " +
            "FROM Inventory i")
    List<Object[]> getInventorySummaryStats();

    @Override
    @EntityGraph(attributePaths = {"variant", "variant.product"})
    Page<Inventory> findAll(Specification<Inventory> spec, Pageable pageable);
}
