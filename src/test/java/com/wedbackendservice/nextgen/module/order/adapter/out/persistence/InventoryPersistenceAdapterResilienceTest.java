package com.wedbackendservice.nextgen.module.order.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.wedbackendservice.nextgen.module.inventory.service.InventoryService;
import com.wedbackendservice.nextgen.module.order.port.out.inventory.InventoryUnavailableException;
import com.wedbackendservice.nextgen.module.order.port.out.inventory.StockItemDto;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;

@ExtendWith(MockitoExtension.class)
class InventoryPersistenceAdapterResilienceTest {

  @Mock private InventoryService inventoryService;

  @Test
  void reserveStockForOrder_businessOutOfStock_doesNotTripCircuitBreaker() {
    InventoryPersistenceAdapter adapter = new InventoryPersistenceAdapter(inventoryService);
    adapter.configureResilienceForTest(2, 5000L, 10, 500L);

    when(inventoryService.reserveStockForOrder(eq(10L), anyList(), anyInt())).thenReturn(false);

    List<StockItemDto> items = List.of(StockItemDto.builder().variantId(1L).quantity(2).build());

    assertThat(adapter.reserveStockForOrder(10L, items, 30)).isFalse();
    assertThat(adapter.reserveStockForOrder(10L, items, 30)).isFalse();
    assertThat(adapter.reserveStockForOrder(10L, items, 30)).isFalse();
    assertThat(adapter.isCircuitOpen()).isFalse();
    assertThat(adapter.isReserveAvailable()).isTrue();
  }

  @Test
  void reserveStockForOrder_technicalFailures_tripsCircuitBreakerAndRecovers() throws Exception {
    InventoryPersistenceAdapter adapter = new InventoryPersistenceAdapter(inventoryService);
    adapter.configureResilienceForTest(2, 120L, 10, 500L);

    when(inventoryService.reserveStockForOrder(eq(20L), anyList(), anyInt()))
        .thenThrow(new QueryTimeoutException("DB lock wait timeout"))
        .thenThrow(new QueryTimeoutException("DB lock wait timeout"))
        .thenReturn(true);

    List<StockItemDto> items = List.of(StockItemDto.builder().variantId(2L).quantity(1).build());

    assertThatThrownBy(() -> adapter.reserveStockForOrder(20L, items, 30))
        .isInstanceOf(InventoryUnavailableException.class);
    assertThat(adapter.isCircuitOpen()).isFalse();

    assertThatThrownBy(() -> adapter.reserveStockForOrder(20L, items, 30))
        .isInstanceOf(InventoryUnavailableException.class);
    assertThat(adapter.isCircuitOpen()).isTrue();
    assertThat(adapter.isReserveAvailable()).isFalse();

    // Khi mạch OPEN: fail-fast không gọi xuống InventoryService
    assertThatThrownBy(() -> adapter.reserveStockForOrder(20L, items, 30))
        .isInstanceOf(InventoryUnavailableException.class);
    verify(inventoryService, times(2)).reserveStockForOrder(eq(20L), anyList(), anyInt());

    // Sau hết cửa sổ openDurationMs (120ms): probe HALF_OPEN thành công -> đóng lại mạch
    Thread.sleep(160L);
    assertThat(adapter.isReserveAvailable()).isTrue();
    assertThat(adapter.reserveStockForOrder(20L, items, 30)).isTrue();
    assertThat(adapter.isCircuitOpen()).isFalse();
  }

  @Test
  void reserveStockForOrder_bulkheadTimeout_throwsInventoryUnavailableException() throws Exception {
    InventoryPersistenceAdapter adapter = new InventoryPersistenceAdapter(inventoryService);
    adapter.configureResilienceForTest(5, 5000L, 1, 60L);

    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    when(inventoryService.reserveStockForOrder(eq(30L), anyList(), anyInt()))
        .thenAnswer(
            inv -> {
              entered.countDown();
              release.await(2, TimeUnit.SECONDS);
              return true;
            });

    List<StockItemDto> items = List.of(StockItemDto.builder().variantId(3L).quantity(1).build());
    Thread blocker =
        new Thread(
            () -> {
              adapter.reserveStockForOrder(30L, items, 0);
            });
    blocker.start();
    assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();

    try {
      assertThatThrownBy(() -> adapter.reserveStockForOrder(31L, items, 0))
          .isInstanceOf(InventoryUnavailableException.class);
    } finally {
      release.countDown();
      blocker.join(2000L);
    }
  }
}
