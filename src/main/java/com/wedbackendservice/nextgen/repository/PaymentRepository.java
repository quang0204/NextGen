package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findFirstByOrder_OrderIdOrderByCreatedAtDesc(Long orderId);
    List<Payment> findByOrder_OrderId(Long orderId);
}
