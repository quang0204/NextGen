package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    List<OrderStatusHistory> findByOrder_OrderIdOrderByCreatedAtDesc(Long orderId);
}
