package com.wedbackendservice.nextgen.module.order.service;

import static org.mockito.Mockito.*;

import com.wedbackendservice.nextgen.module.order.scheduler.OrderExpirationScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderExpirationSchedulerTest {

  @Mock private OrdersService ordersService;

  @InjectMocks private OrderExpirationScheduler scheduler;

  @Test
  @DisplayName("Scheduler tự động quét và gọi cancelExpiredOrders")
  void autoCancelExpiredOrders_CallsService() {
    when(ordersService.cancelExpiredOrders()).thenReturn(3);

    scheduler.autoCancelExpiredOrders();

    verify(ordersService, times(1)).cancelExpiredOrders();
  }

  @Test
  @DisplayName("Scheduler xử lý an toàn khi service ném ngoại lệ")
  void autoCancelExpiredOrders_HandlesExceptionGracefully() {
    when(ordersService.cancelExpiredOrders())
        .thenThrow(new RuntimeException("DB Connection error"));

    // Should not throw exception
    scheduler.autoCancelExpiredOrders();

    verify(ordersService, times(1)).cancelExpiredOrders();
  }
}
