package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
    Optional<Payment> findFirstByOrder_OrderIdOrderByCreatedAtDesc(Long orderId);
    List<Payment> findByOrder_OrderId(Long orderId);
    Optional<Payment> findByTransactionRef(String transactionRef);
    boolean existsByTransactionRef(String transactionRef);

    @Query("SELECT p FROM Payment p WHERE p.order.orderId IN :orderIds ORDER BY p.createdAt DESC")
    List<Payment> findByOrderIdsOrderByCreatedAtDesc(@Param("orderIds") Collection<Long> orderIds);

    @Query("SELECT p.status, p.paymentMethod, COUNT(p), COALESCE(SUM(p.amount), 0) " +
           "FROM Payment p " +
           "WHERE (:from IS NULL OR p.createdAt >= :from) " +
           "  AND (:to IS NULL OR p.createdAt <= :to) " +
           "GROUP BY p.status, p.paymentMethod")
    List<Object[]> summarizePayments(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
