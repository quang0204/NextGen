package com.wedbackendservice.nextgen.enums;

public enum PaymentStatus {
    UNPAID,
    PENDING,
    CUSTOMER_NOTIFIED,
    SUCCESS,
    FAILED,
    EXPIRED,
    CANCELLED,
    REFUND_PENDING,
    REFUNDED,
    PARTIALLY_REFUNDED;

    public boolean canTransitionTo(PaymentStatus next) {
        if (next == null || this == next) {
            return false;
        }
        return switch (this) {
            case UNPAID -> next == SUCCESS || next == CANCELLED;
            case PENDING -> next == SUCCESS || next == FAILED || next == EXPIRED || next == CANCELLED || next == CUSTOMER_NOTIFIED;
            case CUSTOMER_NOTIFIED -> next == SUCCESS || next == FAILED || next == CANCELLED;
            case SUCCESS -> next == REFUND_PENDING || next == PARTIALLY_REFUNDED;
            case REFUND_PENDING -> next == REFUNDED || next == PARTIALLY_REFUNDED;
            case FAILED, EXPIRED, CANCELLED, REFUNDED, PARTIALLY_REFUNDED -> false;
        };
    }

    public static PaymentStatus fromString(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty()) {
            return null;
        }
        if ("PAID".equalsIgnoreCase(statusStr.trim())) {
            return SUCCESS;
        }
        for (PaymentStatus status : values()) {
            if (status.name().equalsIgnoreCase(statusStr.trim())) {
                return status;
            }
        }
        return null;
    }
}
