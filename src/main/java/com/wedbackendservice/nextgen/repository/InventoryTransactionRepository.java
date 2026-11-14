package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long>, JpaSpecificationExecutor<InventoryTransaction> {
    List<InventoryTransaction> findByOrder_OrderId(Long orderId);

    @EntityGraph(attributePaths = {"performedBy", "order"})
    List<InventoryTransaction> findTop5ByVariant_VariantIdOrderByTxnIdDesc(Long variantId);

    Page<InventoryTransaction> findByVariant_VariantIdOrderByTxnIdDesc(Long variantId, Pageable pageable);

    @Query("SELECT DISTINCT t.variant.variantId FROM InventoryTransaction t WHERE t.variant.variantId IN :variantIds")
    List<Long> findVariantIdsHavingTransactions(@Param("variantIds") Collection<Long> variantIds);

    @Override
    @EntityGraph(attributePaths = {"variant", "variant.product", "performedBy", "order"})
    Page<InventoryTransaction> findAll(Specification<InventoryTransaction> spec, Pageable pageable);
}
