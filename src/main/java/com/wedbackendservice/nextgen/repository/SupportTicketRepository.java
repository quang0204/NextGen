package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long>,
        JpaSpecificationExecutor<SupportTicket> {

    Page<SupportTicket> findByUser_UserId(Long userId, Pageable pageable);

    long countByUser_UserId(Long userId);

    long countByStatus(String status);
}
