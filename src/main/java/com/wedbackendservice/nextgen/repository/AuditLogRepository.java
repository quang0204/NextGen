package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Query("SELECT a FROM AuditLog a WHERE a.user.userId = :userId ORDER BY a.createdAt DESC")
    List<AuditLog> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}
