package com.wedbackendservice.nextgen.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "vouchers")
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long voucherId;
    String voucherCode;
    String voucherName;
    String discountType;
    BigDecimal discountValue;
    BigDecimal minOrderValue;
    BigDecimal maxDiscountAmount;
    Integer usageLimit;
    @Builder.Default
    Integer usedCount = 0;
    LocalDateTime startDate;
    LocalDateTime endDate;
    @Builder.Default
    Boolean isActive = true;
    @CreatedDate
    LocalDateTime createdAt;
    @LastModifiedDate
    LocalDateTime updatedAt;

    public String getCode() {
        return voucherCode;
    }

    public void setCode(String code) {
        this.voucherCode = code;
    }

    public BigDecimal getMaxDiscount() {
        return maxDiscountAmount;
    }

    public void setMaxDiscount(BigDecimal maxDiscount) {
        this.maxDiscountAmount = maxDiscount;
    }

    public LocalDateTime getStartAt() {
        return startDate;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startDate = startAt;
    }

    public LocalDateTime getExpiredAt() {
        return endDate;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.endDate = expiredAt;
    }
}
