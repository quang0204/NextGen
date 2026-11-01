package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByVariant_VariantId(Long variantId);
}
