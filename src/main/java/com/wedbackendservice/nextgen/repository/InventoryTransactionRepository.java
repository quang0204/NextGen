package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    List<InventoryTransaction> findByOrder_OrderId(Long orderId);
}
