package com.wedbackendservice.nextgen.port.out.inventory;

import com.wedbackendservice.nextgen.dto.portal.response.order.ExportedItemResponse;
import com.wedbackendservice.nextgen.entity.User;
import java.util.List;

public interface InventoryPort {
  boolean reserveStockForOrder(Long orderId, List<StockItemDto> items, int holdMinutes);

  boolean releaseStockForOrder(Long orderId, String reason);

  List<ExportedItemResponse> exportStockForOrderWithDetails(
      Long orderId, User operator, String exportNote);

  int restockReturnOrder(Long orderId, User operator, String returnReason);
}
