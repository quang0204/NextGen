package com.wedbackendservice.nextgen.scheduler;

import com.wedbackendservice.nextgen.service.shared.OrdersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpirationScheduler {

    private final OrdersService ordersService;

    /**
     * Tự động quét và giải phóng tồn kho cho các đơn hàng chuyển khoản QR hết hạn mỗi 30 giây
     */
    @Scheduled(fixedDelay = 30000)
    public void scanAndCancelExpiredOrders() {
        try {
            int cancelled = ordersService.cancelExpiredOrders();
            if (cancelled > 0) {
                log.info("[OrderExpirationScheduler] Đã tự động hủy {} đơn hàng hết hạn QR và nhả tồn kho giữ chỗ thành công", cancelled);
            }
        } catch (Exception e) {
            log.error("[OrderExpirationScheduler] Lỗi khi quét và xử lý đơn hàng hết hạn QR", e);
        }
    }
}
