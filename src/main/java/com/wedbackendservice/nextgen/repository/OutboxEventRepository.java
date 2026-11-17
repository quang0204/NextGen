package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.OutboxEvent;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
  List<OutboxEvent> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);
}
