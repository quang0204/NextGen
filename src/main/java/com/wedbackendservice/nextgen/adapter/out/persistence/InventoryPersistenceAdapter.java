package com.wedbackendservice.nextgen.adapter.out.persistence;

import com.wedbackendservice.nextgen.dto.portal.response.order.ExportedItemResponse;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.event.saga.OrderCreatedEvent;
import com.wedbackendservice.nextgen.port.out.inventory.InventoryPort;
import com.wedbackendservice.nextgen.port.out.inventory.StockItemDto;
import com.wedbackendservice.nextgen.service.shared.InventoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryPersistenceAdapter implements InventoryPort {

  private final InventoryService inventoryService;

  @Override
  public boolean reserveStockForOrder(Long orderId, List<StockItemDto> items, int holdMinutes) {
    List<OrderCreatedEvent.OrderItemDto> eventItems =
        items == null
            ? List.of()
            : items.stream()
                .map(
                    item ->
                        OrderCreatedEvent.OrderItemDto.builder()
                            .variantId(item.getVariantId())
                            .quantity(item.getQuantity())
                            .build())
                .toList();
    return inventoryService.reserveStockForOrder(orderId, eventItems, holdMinutes);
  }

  @Override
  public boolean releaseStockForOrder(Long orderId, String reason) {
    return inventoryService.releaseStockForOrder(orderId, reason);
  }

  @Override
  public List<ExportedItemResponse> exportStockForOrderWithDetails(
      Long orderId, User operator, String exportNote) {
    return inventoryService.exportStockForOrderWithDetails(orderId, operator, exportNote);
  }

  @Override
  public int restockReturnOrder(Long orderId, User operator, String returnReason) {
    return inventoryService.restockReturnOrder(orderId, operator, returnReason);
  }
}
