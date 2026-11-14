package com.wedbackendservice.nextgen.enums;

public enum PromotionStatus {
    UPCOMING,
    ACTIVE,
    PAUSED,
    ENDED;

    public static PromotionStatus from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (PromotionStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        return null;
    }
}
