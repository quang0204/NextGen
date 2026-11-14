package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.SupportTicket;
import com.wedbackendservice.nextgen.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long>,
        JpaSpecificationExecutor<SupportTicket> {

    long countByStatus(String status);

    @Query("SELECT t.status, COUNT(t) FROM SupportTicket t GROUP BY t.status")
    List<Object[]> countByStatusGrouped();

    List<SupportTicket> findByUserOrderByCreatedAtDesc(User user);

    List<SupportTicket> findByUser_UserIdOrderByCreatedAtDesc(Long userId);
}
