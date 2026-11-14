package com.wedbackendservice.nextgen.enums;

public enum StockStatus {
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK;

    public static StockStatus from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (StockStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        return null;
    }
}
