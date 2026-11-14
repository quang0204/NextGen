package com.wedbackendservice.nextgen.enums;

public enum OrderStatus {
    PENDING_PAYMENT,
    WAITING_CONFIRMATION,
    CONFIRMED,
    PREPARING,
    PICKED_UP,
    SHIPPING,
    COMPLETED,
    CANCELLED;

    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }

    public static OrderStatus from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (OrderStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        return null;
    }
}
