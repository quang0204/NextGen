package com.wedbackendservice.nextgen.module.order.port.out.inventory;

/**
 * Tín hiệu kỹ thuật khi dịch vụ tồn kho quá tải, timeout hoặc Circuit Breaker đang mở, cho phép
 * OrderCreationService hạ cấp sang nhánh tiếp nhận bất đồng bộ qua Kafka Saga.
 */
public class InventoryUnavailableException extends RuntimeException {
  public InventoryUnavailableException(String message) {
    super(message);
  }

  public InventoryUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
