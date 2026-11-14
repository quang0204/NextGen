package com.wedbackendservice.nextgen.enums;

public enum ReturnStatus {
    PENDING,
    INSPECTING,
    APPROVED,
    REJECTED;

    public static ReturnStatus from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (ReturnStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        return null;
    }
}
