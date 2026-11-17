package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.StockReservation;
import com.wedbackendservice.nextgen.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    List<StockReservation> findByOrder_OrderId(Long orderId);

    List<StockReservation> findByOrder_OrderIdAndStatus(Long orderId, ReservationStatus status);

    boolean existsByOrder_OrderIdAndStatusIn(Long orderId, Collection<ReservationStatus> statuses);

    List<StockReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime threshold);
}
