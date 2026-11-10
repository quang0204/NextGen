package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.orderId = :orderId")
    List<OrderItem> findByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.orderId IN :orderIds")
    List<OrderItem> findByOrderIdIn(@Param("orderIds") Collection<Long> orderIds);

    @Query("SELECT oi.variant.product.productId, COALESCE(SUM(oi.quantity), 0) " +
           "FROM OrderItem oi " +
           "WHERE oi.order.status <> 'CANCELLED' AND oi.variant.product.productId IN :productIds " +
           "GROUP BY oi.variant.product.productId")
    List<Object[]> countSoldByProductIds(@Param("productIds") Collection<Long> productIds);
}
