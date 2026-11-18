package com.wedbackendservice.nextgen.module.order.repository;

import com.wedbackendservice.nextgen.module.order.entity.OrderStatusHistory;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
  List<OrderStatusHistory> findByOrder_OrderIdOrderByCreatedAtDesc(Long orderId);

  List<OrderStatusHistory> findByOrder_OrderIdInOrderByCreatedAtDesc(Collection<Long> orderIds);
}
