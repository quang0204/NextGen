package com.wedbackendservice.nextgen.enums;

public enum ReviewStatus {
    VISIBLE,
    HIDDEN;

    public static ReviewStatus from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (ReviewStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        return null;
    }
}
