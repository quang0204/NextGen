package com.wedbackendservice.nextgen.module.notification.repository;

import com.wedbackendservice.nextgen.module.notification.entity.InboxEvent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InboxEventRepository extends JpaRepository<InboxEvent, Long> {

  boolean existsByEventIdAndConsumerGroup(String eventId, String consumerGroup);

  Optional<InboxEvent> findByEventIdAndConsumerGroup(String eventId, String consumerGroup);
}
