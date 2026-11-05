package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderCode(String orderCode);

    boolean existsByOrderCode(String orderCode);

    @Query("SELECT o FROM Order o WHERE o.user.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    Page<Order> findByUser_UserId(Long userId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.user.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        SELECT o.user.userId, COUNT(o), COALESCE(SUM(o.finalAmount), 0)
        FROM Order o
        WHERE o.user.userId IN :ids
        GROUP BY o.user.userId
        """)
    List<Object[]> statsByUserIds(@Param("ids") Collection<Long> ids);

    long countByUser_UserId(Long userId);

    long countByStatus(String status);

    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countGroupByStatus();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.userId = :userId AND o.status = :status")
    long countByUser_UserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.user.userId = :userId")
    java.math.BigDecimal sumTotalSpentByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.user.userId = :userId AND o.status = 'COMPLETED'")
    java.math.BigDecimal sumLifetimeValueByUserId(@Param("userId") Long userId);
}
