package com.wedbackendservice.nextgen.enums;

public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    REJECTED;

    public boolean isFinal() {
        return this == RESOLVED || this == CLOSED || this == REJECTED;
    }
}
