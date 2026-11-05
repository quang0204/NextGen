package com.wedbackendservice.nextgen.enums;

/**
 * Trạng thái xử lý ticket hỗ trợ khách hàng.
 */
public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    public boolean isFinal() {
        return this == RESOLVED || this == CLOSED;
    }

    public static TicketStatus from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (TicketStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        return null;
    }
}
