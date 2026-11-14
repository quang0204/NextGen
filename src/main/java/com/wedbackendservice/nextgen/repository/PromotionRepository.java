package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long>, JpaSpecificationExecutor<Promotion> {

    @Query("SELECT p FROM Promotion p WHERE p.isActive = true AND (p.status IS NULL OR UPPER(p.status) NOT IN ('PAUSED', 'ENDED')) " +
            "AND (p.startDate IS NULL OR p.startDate <= :now) " +
            "AND (p.endDate IS NULL OR p.endDate >= :now)")
    List<Promotion> findActivePromotions(@Param("now") LocalDateTime now);
}
