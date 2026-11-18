package com.wedbackendservice.nextgen.module.inventory.repository;

import com.wedbackendservice.nextgen.module.inventory.entity.StockReservation;
import com.wedbackendservice.nextgen.module.inventory.enums.ReservationStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

  List<StockReservation> findByOrder_OrderId(Long orderId);

  List<StockReservation> findByOrder_OrderIdAndStatus(Long orderId, ReservationStatus status);

  boolean existsByOrder_OrderIdAndStatusIn(Long orderId, Collection<ReservationStatus> statuses);

  List<StockReservation> findByStatusAndExpiresAtBefore(
      ReservationStatus status, LocalDateTime threshold);
}
