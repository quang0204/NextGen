package com.wedbackendservice.nextgen.service.shared;

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
     * Quét và tự động hủy các đơn hàng chờ thanh toán QR quá hạn (mặc định 30 phút),
     * giải phóng số lượng tồn kho đã giữ chỗ và hoàn lại lượt voucher.
     * Chạy định kỳ mỗi 5 phút (300.000 ms).
     */
    @Scheduled(fixedDelayString = "${orders.cleanup-interval-ms:300000}")
    public void autoCancelExpiredOrders() {
        try {
            int cancelled = ordersService.cancelExpiredOrders();
            if (cancelled > 0) {
                log.info("Tự động hủy {} đơn hàng quá thời hạn thanh toán và giải phóng tồn kho giữ chỗ.", cancelled);
            }
        } catch (Exception e) {
            log.warn("Lỗi trong quá trình quét hủy đơn hàng quá hạn: {}", e.getMessage());
        }
    }
}
