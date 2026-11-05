package com.wedbackendservice.nextgen.enums;

/**
 * Trạng thái giao dịch thanh toán.
 */
public enum PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    REFUNDED,
    CANCELLED;

    public boolean isFinal() {
        return this == SUCCESS || this == FAILED || this == REFUNDED || this == CANCELLED;
    }

    public static PaymentStatus from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (PaymentStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        return null;
    }
}
