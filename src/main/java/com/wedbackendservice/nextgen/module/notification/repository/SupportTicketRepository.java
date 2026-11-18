package com.wedbackendservice.nextgen.module.notification.repository;

import com.wedbackendservice.nextgen.module.notification.entity.SupportTicket;
import com.wedbackendservice.nextgen.module.user.entity.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportTicketRepository
    extends JpaRepository<SupportTicket, Long>, JpaSpecificationExecutor<SupportTicket> {

  @Override
  @EntityGraph(attributePaths = {"user", "handledBy"})
  Page<SupportTicket> findAll(Specification<SupportTicket> spec, Pageable pageable);

  long countByStatus(String status);

  @Query("SELECT t.status, COUNT(t) FROM SupportTicket t GROUP BY t.status")
  List<Object[]> countByStatusGrouped();

  @EntityGraph(attributePaths = {"user", "handledBy"})
  List<SupportTicket> findByUserOrderByCreatedAtDesc(User user);

  @EntityGraph(attributePaths = {"user", "handledBy"})
  List<SupportTicket> findByUser_UserIdOrderByCreatedAtDesc(Long userId);
}
