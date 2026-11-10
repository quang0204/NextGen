package com.wedbackendservice.nextgen.repository;

import com.wedbackendservice.nextgen.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long>, JpaSpecificationExecutor<Voucher> {

    Optional<Voucher> findByVoucherCodeIgnoreCase(String code);

    boolean existsByVoucherCodeIgnoreCase(String code);

    @Query("SELECT v FROM Voucher v WHERE v.isActive = true " +
            "AND (v.startDate IS NULL OR v.startDate <= :now) " +
            "AND (v.endDate IS NULL OR v.endDate >= :now) " +
            "AND (v.usageLimit IS NULL OR v.usedCount < v.usageLimit)")
    List<Voucher> findValidVouchers(@Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Voucher v SET v.usedCount = COALESCE(v.usedCount, 0) + 1 " +
            "WHERE v.voucherId = :voucherId AND (v.usageLimit IS NULL OR COALESCE(v.usedCount, 0) < v.usageLimit)")
    int incrementUsedCountAtomic(@Param("voucherId") Long voucherId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Voucher v SET v.usedCount = CASE WHEN COALESCE(v.usedCount, 0) > 0 THEN v.usedCount - 1 ELSE 0 END " +
            "WHERE v.voucherId = :voucherId")
    int decrementUsedCountAtomic(@Param("voucherId") Long voucherId);
}
