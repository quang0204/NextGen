package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByVariant_VariantId(Long variantId);

    List<Inventory> findByVariant_VariantIdIn(Collection<Long> variantIds);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Inventory i SET i.quantity = i.quantity + :qty, i.updatedAt = :now WHERE i.variant.variantId = :variantId")
    int increaseQuantityAtomic(@Param("variantId") Long variantId, @Param("qty") int qty, @Param("now") LocalDateTime now);
}
