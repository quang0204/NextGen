package com.wedbackendservice.nextgen.module.order.enums;

public enum OrderStatus {
  PENDING,
  PENDING_PAYMENT,
  WAITING_CONFIRMATION,
  CONFIRMED,
  PREPARING,
  PICKED_UP,
  SHIPPING,
  DELIVERED,
  RETURNING,
  COMPLETED,
  CANCELLED;

  public boolean canTransitionTo(OrderStatus next) {
    if (next == null || this == next) {
      return false;
    }
    return switch (this) {
      case PENDING_PAYMENT -> next == WAITING_CONFIRMATION || next == CANCELLED;
      case WAITING_CONFIRMATION -> next == CONFIRMED || next == CANCELLED;
      case CONFIRMED -> next == PREPARING || next == CANCELLED;
      case PREPARING -> next == SHIPPING || next == CANCELLED;
      case SHIPPING ->
          next == PICKED_UP || next == DELIVERED || next == COMPLETED || next == RETURNING;
      case PICKED_UP ->
          next == SHIPPING || next == DELIVERED || next == COMPLETED || next == RETURNING;
      case DELIVERED -> next == COMPLETED || next == RETURNING;
      case RETURNING -> next == CANCELLED;
      case PENDING -> next == PENDING_PAYMENT || next == WAITING_CONFIRMATION || next == CANCELLED;
      case COMPLETED, CANCELLED -> false;
    };
  }

  public static OrderStatus fromString(String statusStr) {
    if (statusStr == null || statusStr.trim().isEmpty()) {
      return null;
    }
    for (OrderStatus status : values()) {
      if (status.name().equalsIgnoreCase(statusStr.trim())) {
        return status;
      }
    }
    return null;
  }
}
