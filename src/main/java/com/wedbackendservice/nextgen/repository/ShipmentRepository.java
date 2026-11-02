package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.Shipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long>, JpaSpecificationExecutor<Shipment> {

    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    List<Shipment> findAllByOrder_OrderCode(String orderCode);

    @Query("SELECT s FROM Shipment s LEFT JOIN s.order o WHERE s.trackingNumber = :code OR o.orderCode = :code")
    List<Shipment> findListByTrackingCodeOrOrderCode(@Param("code") String code);

    default Optional<Shipment> findByTrackingCodeOrOrderCode(String code) {
        List<Shipment> list = findListByTrackingCodeOrOrderCode(code);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    @EntityGraph(attributePaths = {"order", "updatedBy"})
    Page<Shipment> findAll(Specification<Shipment> spec, Pageable pageable);
}
