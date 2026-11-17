package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.InboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InboxEventRepository extends JpaRepository<InboxEvent, Long> {

    boolean existsByEventIdAndConsumerGroup(String eventId, String consumerGroup);

    Optional<InboxEvent> findByEventIdAndConsumerGroup(String eventId, String consumerGroup);
}
