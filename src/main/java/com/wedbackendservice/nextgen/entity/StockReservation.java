package com.wedbackendservice.nextgen.entity;

import com.wedbackendservice.nextgen.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "stock_reservations", indexes = {
    @Index(name = "idx_stock_res_order_status", columnList = "order_order_id, status"),
    @Index(name = "idx_stock_res_status_expires", columnList = "status, expires_at")
})
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long reservationId;

    @ManyToOne(fetch = FetchType.LAZY)
    Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    ProductVariant variant;

    Integer quantity;

    @Enumerated(EnumType.STRING)
    ReservationStatus status;

    LocalDateTime expiresAt;

    @CreatedDate
    LocalDateTime createdAt;

    @LastModifiedDate
    LocalDateTime updatedAt;
}
