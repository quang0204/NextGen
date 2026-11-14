package com.wedbackendservice.nextgen.enums;

public enum TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT;

    public static TicketPriority from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (TicketPriority priority : values()) {
            if (priority.name().equalsIgnoreCase(value.trim())) {
                return priority;
            }
        }
        return null;
    }
}
