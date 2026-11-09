package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long>,
        JpaSpecificationExecutor<SupportTicket> {

    long countByStatus(String status);
}
