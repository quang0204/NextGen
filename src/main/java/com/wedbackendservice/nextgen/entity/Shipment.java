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
@Table(name = "shipments")
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Shipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long shipmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    User updatedBy;

    String trackingNumber;
    String carrierName;
    String shippingStatus;
    LocalDateTime estimatedDelivery;
    LocalDateTime shippedAt;
    LocalDateTime deliveredAt;
    String failReason;
    String notes;

    @Column(name = "recipient_name")
    String recipientName;

    @Column(name = "recipient_phone")
    String recipientPhone;

    @Column(name = "shipping_address")
    String shippingAddress;

    @Column(name = "cod_amount")
    BigDecimal codAmount;

    @Version
    Long version;

    @CreatedDate
    LocalDateTime createdAt;

    @LastModifiedDate
    LocalDateTime updatedAt;

    public String getTrackingCode() {
        return trackingNumber;
    }

    public void setTrackingCode(String code) {
        this.trackingNumber = code;
    }

    public String getCarrier() {
        return carrierName;
    }

    public void setCarrier(String carrier) {
        this.carrierName = carrier;
    }

    public String getStatus() {
        return shippingStatus;
    }

    public void setStatus(String status) {
        this.shippingStatus = status;
    }
}
