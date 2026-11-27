package com.wedbackendservice.nextgen.module.order.adapter.out.persistence;

import com.wedbackendservice.nextgen.core.event.saga.OrderCreatedEvent;
import com.wedbackendservice.nextgen.module.inventory.service.InventoryService;
import com.wedbackendservice.nextgen.module.order.dto.portal.response.ExportedItemResponse;
import com.wedbackendservice.nextgen.module.order.port.out.inventory.InventoryPort;
import com.wedbackendservice.nextgen.module.order.port.out.inventory.InventoryUnavailableException;
import com.wedbackendservice.nextgen.module.order.port.out.inventory.StockItemDto;
import com.wedbackendservice.nextgen.module.user.entity.User;
import jakarta.persistence.PersistenceException;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryPersistenceAdapter implements InventoryPort {

  private final InventoryService inventoryService;

  @Value("${order.inventory.resilience.enabled:true}")
  private boolean resilienceEnabled = true;

  @Value("${order.inventory.resilience.failure-threshold:5}")
  private int failureThreshold = 5;

  @Value("${order.inventory.resilience.open-duration-ms:15000}")
  private long openDurationMs = 15000L;

  @Value("${order.inventory.resilience.max-concurrent:100}")
  private int maxConcurrent = 100;

  @Value("${order.inventory.resilience.acquire-timeout-ms:2000}")
  private long acquireTimeoutMs = 2000L;

  private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
  private final AtomicBoolean halfOpenInFlight = new AtomicBoolean(false);
  private final AtomicReference<Semaphore> bulkheadRef = new AtomicReference<>();
  private volatile long openUntilEpochMs = 0L;

  @Override
  public boolean isReserveAvailable() {
    return !resilienceEnabled
        || openUntilEpochMs <= 0L
        || System.currentTimeMillis() >= openUntilEpochMs;
  }

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

    if (!resilienceEnabled) {
      return inventoryService.reserveStockForOrder(orderId, eventItems, holdMinutes);
    }

    boolean isHalfOpenProbe = acquireCircuitPermit();
    Semaphore bulkhead = getBulkhead();
    boolean semaphoreAcquired = false;
    try {
      semaphoreAcquired =
          bulkhead.tryAcquire(Math.max(acquireTimeoutMs, 1L), TimeUnit.MILLISECONDS);
      if (!semaphoreAcquired) {
        recordFailure();
        throw new InventoryUnavailableException(
            "Luồng giữ chỗ tồn kho đang quá tải (bulkhead timeout " + acquireTimeoutMs + "ms)");
      }

      boolean reserved = inventoryService.reserveStockForOrder(orderId, eventItems, holdMinutes);
      recordSuccess();
      return reserved;
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      recordFailure();
      throw new InventoryUnavailableException("Bị ngắt khi chờ luồng giữ chỗ tồn kho", ex);
    } catch (DataAccessException | PersistenceException | TransactionException ex) {
      recordFailure();
      log.warn(
          "[InventoryCircuitBreaker] Lỗi kỹ thuật khi giữ kho đơn #{}: {}",
          orderId,
          ex.getMessage());
      throw new InventoryUnavailableException("Lỗi kết nối/timeout khi giữ chỗ tồn kho", ex);
    } finally {
      if (isHalfOpenProbe) {
        halfOpenInFlight.set(false);
      }
      if (semaphoreAcquired) {
        bulkhead.release();
      }
    }
  }

  private boolean acquireCircuitPermit() {
    long openUntil = openUntilEpochMs;
    if (openUntil <= 0L) {
      return false;
    }
    long now = System.currentTimeMillis();
    if (now < openUntil) {
      throw new InventoryUnavailableException(
          "Circuit Breaker tồn kho đang OPEN, tạm chuyển sang xử lý bất đồng bộ");
    }
    // Cho phép đúng 1 request thăm dò HALF_OPEN khi hết cửa sổ mở mạch
    if (halfOpenInFlight.compareAndSet(false, true)) {
      return true;
    }
    throw new InventoryUnavailableException(
        "Circuit Breaker tồn kho đang thăm dò HALF_OPEN, chuyển sang xử lý bất đồng bộ");
  }

  private void recordSuccess() {
    consecutiveFailures.set(0);
    openUntilEpochMs = 0L;
  }

  private void recordFailure() {
    int threshold = Math.max(failureThreshold, 1);
    int failures = consecutiveFailures.incrementAndGet();
    if (failures >= threshold || openUntilEpochMs > 0L) {
      openUntilEpochMs = System.currentTimeMillis() + Math.max(openDurationMs, 100L);
      log.warn(
          "[InventoryCircuitBreaker] Mở mạch OPEN trong {}ms sau {} lỗi kỹ thuật liên tiếp",
          openDurationMs,
          failures);
    }
  }

  private Semaphore getBulkhead() {
    Semaphore current = bulkheadRef.get();
    if (current == null) {
      Semaphore created = new Semaphore(Math.max(maxConcurrent, 1));
      if (bulkheadRef.compareAndSet(null, created)) {
        return created;
      }
      return bulkheadRef.get();
    }
    return current;
  }

  void configureResilienceForTest(
      int failureThreshold, long openDurationMs, int maxConcurrent, long acquireTimeoutMs) {
    this.failureThreshold = failureThreshold;
    this.openDurationMs = openDurationMs;
    this.maxConcurrent = maxConcurrent;
    this.acquireTimeoutMs = acquireTimeoutMs;
    this.bulkheadRef.set(new Semaphore(Math.max(maxConcurrent, 1)));
    this.consecutiveFailures.set(0);
    this.openUntilEpochMs = 0L;
    this.halfOpenInFlight.set(false);
  }

  boolean isCircuitOpen() {
    return openUntilEpochMs > System.currentTimeMillis();
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

  @Override
  public int restockReturnOrder(
      Long orderId, User operator, String returnReason, boolean isIntact) {
    return inventoryService.restockReturnOrder(orderId, operator, returnReason, isIntact);
  }
}
