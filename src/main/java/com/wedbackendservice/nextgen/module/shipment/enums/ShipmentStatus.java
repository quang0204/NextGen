package com.wedbackendservice.nextgen.module.shipment.enums;

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

  private static final Map<ShipmentStatus, Set<ShipmentStatus>> VALID_TRANSITIONS =
      Map.of(
          READY_TO_PICK, EnumSet.of(PICKED, CANCELLED),
          PICKED, EnumSet.of(IN_TRANSIT),
          IN_TRANSIT, EnumSet.of(DELIVERED, FAILED_DELIVERY),
          FAILED_DELIVERY, EnumSet.of(IN_TRANSIT, CANCELLED),
          DELIVERED, Collections.emptySet(),
          CANCELLED, Collections.emptySet());

  public boolean canTransitionTo(ShipmentStatus nextStatus) {
    if (this == nextStatus) {
      return false;
    }
    return VALID_TRANSITIONS.getOrDefault(this, Collections.emptySet()).contains(nextStatus);
  }
}
