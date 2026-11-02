package com.wedbackendservice.nextgen.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum ShipmentStatus {
    READY_TO_PICK,
    PICKED,
    IN_TRANSIT,
    DELIVERED,
    FAILED_DELIVERY,
    CANCELLED;

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> VALID_TRANSITIONS = Map.of(
            READY_TO_PICK, EnumSet.of(PICKED, CANCELLED),
            PICKED, EnumSet.of(IN_TRANSIT, FAILED_DELIVERY, CANCELLED),
            IN_TRANSIT, EnumSet.of(DELIVERED, FAILED_DELIVERY),
            FAILED_DELIVERY, EnumSet.of(IN_TRANSIT, DELIVERED, CANCELLED),
            DELIVERED, Collections.emptySet(), // Terminal state
            CANCELLED, Collections.emptySet()  // Terminal state
    );

    public boolean canTransitionTo(ShipmentStatus nextStatus) {
        if (this == nextStatus) {
            return false;
        }
        Set<ShipmentStatus> allowed = VALID_TRANSITIONS.getOrDefault(this, Collections.emptySet());
        return allowed.contains(nextStatus);
    }

    public static ShipmentStatus fromString(String statusStr) {
        if (statusStr == null || statusStr.isBlank()) {
            return null;
        }
        try {
            return ShipmentStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
