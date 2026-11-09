package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long>, JpaSpecificationExecutor<ReturnRequest> {

    boolean existsByOrder_OrderIdAndStatusIn(Long orderId, Collection<String> statuses);

    @Query("SELECT COUNT(r) FROM ReturnRequest r WHERE r.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT COUNT(r) FROM ReturnRequest r WHERE r.status IN :statuses")
    long countByStatusIn(@Param("statuses") Collection<String> statuses);
}
