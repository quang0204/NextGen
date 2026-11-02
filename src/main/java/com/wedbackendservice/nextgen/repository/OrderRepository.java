package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT o FROM Order o WHERE o.user.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    long countByUser_UserId(Long userId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.userId = :userId AND o.status = :status")
    long countByUser_UserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.user.userId = :userId")
    java.math.BigDecimal sumTotalSpentByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.user.userId = :userId AND o.status = 'COMPLETED'")
    java.math.BigDecimal sumLifetimeValueByUserId(@Param("userId") Long userId);
}
