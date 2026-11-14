package com.wedbackendservice.nextgen.enums;

public enum InventoryTransactionType {
    IMPORT,
    EXPORT,
    ADJUST,
    RETURN;

    public static InventoryTransactionType from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (InventoryTransactionType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        return null;
    }
}
