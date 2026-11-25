package com.wedbackendservice.nextgen.module.order.port.out.inventory;

import com.wedbackendservice.nextgen.module.order.dto.portal.response.ExportedItemResponse;
import com.wedbackendservice.nextgen.module.user.entity.User;
import java.util.List;

public interface InventoryPort {
  default boolean isReserveAvailable() {
    return true;
  }

  boolean reserveStockForOrder(Long orderId, List<StockItemDto> items, int holdMinutes);

  boolean releaseStockForOrder(Long orderId, String reason);

  List<ExportedItemResponse> exportStockForOrderWithDetails(
      Long orderId, User operator, String exportNote);

  int restockReturnOrder(Long orderId, User operator, String returnReason);
}
