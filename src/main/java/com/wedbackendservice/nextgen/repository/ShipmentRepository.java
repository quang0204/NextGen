package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long>, JpaSpecificationExecutor<Shipment> {

    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    @Query("SELECT s FROM Shipment s LEFT JOIN s.order o WHERE s.trackingNumber = :code OR o.orderCode = :code")
    Optional<Shipment> findByTrackingCodeOrOrderCode(@Param("code") String code);
}
