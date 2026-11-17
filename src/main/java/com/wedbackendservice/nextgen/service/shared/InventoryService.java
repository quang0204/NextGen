package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.dto.portal.response.order.ExportedItemResponse;
import com.wedbackendservice.nextgen.entity.*;
import com.wedbackendservice.nextgen.enums.InventoryTransactionType;
import com.wedbackendservice.nextgen.enums.ReservationStatus;
import com.wedbackendservice.nextgen.event.saga.OrderCreatedEvent;
import com.wedbackendservice.nextgen.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository stockReservationRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * Giữ chỗ tồn kho nguyên tử theo đơn hàng (ReserveStock)
     * Trả về true nếu toàn bộ items đều giữ chỗ thành công; false nếu thiếu hàng hoặc lỗi.
     * Chống giữ hàng hai lần cho cùng 1 Order (Idempotency).
     */
    @Transactional
    public boolean reserveStockForOrder(Long orderId, List<OrderCreatedEvent.OrderItemDto> items, int expireMinutes) {
        if (items == null || items.isEmpty()) {
            return true;
        }

        // 1. Kiểm tra đã có reservation hợp lệ chưa (Idempotency chống giữ 2 lần)
        boolean alreadyReserved = stockReservationRepository.existsByOrder_OrderIdAndStatusIn(
                orderId, List.of(ReservationStatus.RESERVED, ReservationStatus.CONFIRMED));
        if (alreadyReserved) {
            log.info("[InventoryService] Order #{} already has active stock reservation, skipping duplicate reserve", orderId);
            return true;
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.error("[InventoryService] Cannot reserve stock: Order #{} not found", orderId);
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = expireMinutes > 0 ? now.plusMinutes(expireMinutes) : null;
        List<StockReservation> createdReservations = new ArrayList<>();
        boolean allReserved = true;

        // BẮT BUỘC sắp xếp variantId tăng dần để khóa hàng theo cùng một thứ tự trên DB, triệt tiêu Deadlock
        List<OrderCreatedEvent.OrderItemDto> sortedItems = items.stream()
                .sorted(java.util.Comparator.comparing(OrderCreatedEvent.OrderItemDto::getVariantId))
                .toList();

        for (OrderCreatedEvent.OrderItemDto item : sortedItems) {
            int rowsUpdated = inventoryRepository.reserveStockAtomic(item.getVariantId(), item.getQuantity(), now);
            if (rowsUpdated > 0) {
                ProductVariant variant = productVariantRepository.findById(item.getVariantId()).orElse(null);
                StockReservation res = StockReservation.builder()
                        .order(order)
                        .variant(variant)
                        .quantity(item.getQuantity())
                        .status(ReservationStatus.RESERVED)
                        .expiresAt(expiresAt)
                        .createdAt(now)
                        .build();
                stockReservationRepository.save(res);
                createdReservations.add(res);
            } else {
                allReserved = false;
                break;
            }
        }

        if (!allReserved) {
            log.warn("[InventoryService] Stock reservation failed for order #{}: rolling back partially reserved items", orderId);
            // Rollback các item đã giữ thành công trước đó
            for (StockReservation res : createdReservations) {
                inventoryRepository.releaseStockAtomic(res.getVariant().getVariantId(), res.getQuantity(), now);
                res.setStatus(ReservationStatus.RELEASED);
                stockReservationRepository.save(res);
            }
            return false;
        }

        log.info("[InventoryService] Successfully reserved stock for order #{}, items count: {}", orderId, items.size());
        return true;
    }

    /**
     * Giải phóng lượng giữ chỗ (ReleaseStock) khi hủy đơn hoặc thanh toán thất bại
     * Chống release 2 lần: chỉ release các reservation đang ở trạng thái RESERVED.
     */
    @Transactional
    public boolean releaseStockForOrder(Long orderId, String reason) {
        List<StockReservation> reservations = stockReservationRepository.findByOrder_OrderIdAndStatus(
                orderId, ReservationStatus.RESERVED);
        if (reservations.isEmpty()) {
            log.info("[InventoryService] No active RESERVED stock found for order #{}, skipping release", orderId);
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        for (StockReservation res : reservations) {
            inventoryRepository.releaseStockAtomic(res.getVariant().getVariantId(), res.getQuantity(), now);
            res.setStatus(ReservationStatus.RELEASED);
            res.setUpdatedAt(now);
            stockReservationRepository.save(res);
        }
        log.info("[InventoryService] Released {} stock reservations for order #{} (reason: {})",
                reservations.size(), orderId, reason);
        return true;
    }

    /**
     * Xác nhận xuất kho vật lý (ConfirmStock / MB03)
     * Trừ quantity vật lý và trừ reservedQuantity.
     * Chuyển reservation sang CONFIRMED.
     * Ghi nhận sổ cái InventoryTransaction (EXPORT - MB03) đúng 1 lần.
     */
    @Transactional
    public boolean exportStockForOrder(Long orderId, User operator, String exportNote) {
        boolean alreadyExported = inventoryTransactionRepository.existsByOrder_OrderIdAndTransactionType(
                orderId, InventoryTransactionType.EXPORT.name());
        if (alreadyExported) {
            log.info("[InventoryService] Order #{} already has physical stock exported (MB03), skipping duplicate", orderId);
            return true;
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        List<StockReservation> reservations = stockReservationRepository.findByOrder_OrderIdAndStatus(
                orderId, ReservationStatus.RESERVED);

        if (!reservations.isEmpty()) {
            for (StockReservation res : reservations) {
                ProductVariant variant = res.getVariant();
                Inventory inv = inventoryRepository.findWithLockByVariantId(variant.getVariantId()).orElse(null);
                int qtyBefore = inv != null ? inv.getOnHandQty() : 0;
                inventoryRepository.deductStockAndReservedAtomic(variant.getVariantId(), res.getQuantity(), now);

                res.setStatus(ReservationStatus.CONFIRMED);
                res.setUpdatedAt(now);
                stockReservationRepository.save(res);

                inventoryTransactionRepository.save(InventoryTransaction.builder()
                        .variant(variant)
                        .order(order)
                        .performedBy(operator)
                        .transactionType(InventoryTransactionType.EXPORT.name())
                        .formCode("MB03")
                        .quantity(res.getQuantity())
                        .qtyBefore(qtyBefore)
                        .qtyAfter(Math.max(0, qtyBefore - res.getQuantity()))
                        .reason(exportNote != null ? exportNote : "Xuất kho vật lý theo đơn hàng MB03")
                        .build());
            }
        } else {
            // Fallback nếu đơn hàng chưa có bản ghi reservation (ví dụ đơn cũ)
            List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
            for (OrderItem item : items) {
                ProductVariant variant = item.getVariant();
                if (variant != null) {
                    Inventory inv = inventoryRepository.findWithLockByVariantId(variant.getVariantId()).orElse(null);
                    int qtyBefore = inv != null ? inv.getOnHandQty() : 0;
                    inventoryRepository.deductStockAndReservedAtomic(variant.getVariantId(), item.getQuantity(), now);

                    inventoryTransactionRepository.save(InventoryTransaction.builder()
                            .variant(variant)
                            .order(order)
                            .performedBy(operator)
                            .transactionType(InventoryTransactionType.EXPORT.name())
                            .formCode("MB03")
                            .quantity(item.getQuantity())
                            .qtyBefore(qtyBefore)
                            .qtyAfter(Math.max(0, qtyBefore - item.getQuantity()))
                            .reason(exportNote != null ? exportNote : "Xuất kho vật lý theo đơn hàng MB03")
                            .build());
                }
            }
        }

        log.info("[InventoryService] Confirmed physical stock export (MB03) for order #{}", orderId);
        return true;
    }

    @Transactional
    public List<ExportedItemResponse> exportStockForOrderWithDetails(Long orderId, User operator, String exportNote) {
        boolean success = exportStockForOrder(orderId, operator, exportNote);
        if (!success) {
            return java.util.Collections.emptyList();
        }
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        List<ExportedItemResponse> result = new ArrayList<>();
        for (OrderItem oi : items) {
            ProductVariant v = oi.getVariant();
            if (v == null) continue;
            Inventory inv = inventoryRepository.findByVariant_VariantId(v.getVariantId()).orElse(null);
            result.add(ExportedItemResponse.builder()
                    .sku(v.getSku())
                    .quantity(oi.getQuantity())
                    .remainingOnHand(inv != null ? inv.getOnHandQty() : 0)
                    .build());
        }
        return result;
    }

    /**
     * Nhập hoàn kho (MB04 / Return restock)
     * Tăng onHandQty và ghi nhận sổ cái RETURN (MB04).
     */
    @Transactional
    public int restockReturnOrder(Long orderId, User operator, String returnReason) {
        boolean alreadyReturned = inventoryTransactionRepository.existsByOrder_OrderIdAndTransactionType(
                orderId, InventoryTransactionType.RETURN.name());
        if (alreadyReturned) {
            log.info("[InventoryService] Order #{} already has return restocked (MB04), skipping duplicate", orderId);
            return 0;
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        boolean wasExported = inventoryTransactionRepository.existsByOrder_OrderIdAndTransactionType(
                orderId, InventoryTransactionType.EXPORT.name());

        int restockedCount = 0;
        if (wasExported) {
            // Đã xuất kho vật lý -> Tăng lại số lượng on-hand và ghi phiếu MB04
            List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
            for (OrderItem item : items) {
                ProductVariant variant = item.getVariant();
                if (variant != null) {
                    Inventory inv = inventoryRepository.findWithLockByVariantId(variant.getVariantId()).orElse(null);
                    int qtyBefore = inv != null ? inv.getOnHandQty() : 0;
                    inventoryRepository.increaseQuantityAtomic(variant.getVariantId(), item.getQuantity(), now);
                    restockedCount++;

                    inventoryTransactionRepository.save(InventoryTransaction.builder()
                            .variant(variant)
                            .order(order)
                            .performedBy(operator)
                            .transactionType(InventoryTransactionType.RETURN.name())
                            .formCode("MB04")
                            .quantity(item.getQuantity())
                            .qtyBefore(qtyBefore)
                            .qtyAfter(qtyBefore + item.getQuantity())
                            .reason("Nhập hoàn kho theo biên bản MB04: " + (returnReason != null ? returnReason : "Giao hàng thất bại"))
                            .build());
                }
            }
        } else {
            // Chưa xuất kho vật lý -> Chỉ giải phóng lượng giữ chỗ (RESERVED)
            releaseStockForOrder(orderId, returnReason);
            restockedCount = 1;
        }

        log.info("[InventoryService] Restocked return items (MB04) for order #{}, items count: {}", orderId, restockedCount);
        return restockedCount;
    }
}
