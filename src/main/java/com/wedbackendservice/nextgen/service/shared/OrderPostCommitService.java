package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.port.out.cart.CartPort;
import com.wedbackendservice.nextgen.port.out.notification.NotificationPort;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderPostCommitService {

  private final CartPort cartPort;
  private final NotificationPort notificationPort;

  public void cleanupCartAndSendNotification(
      Long userId,
      Collection<Long> variantIds,
      Long orderId,
      String orderCode,
      boolean isTransfer,
      int expireMinutes) {
    CompletableFuture.runAsync(
        () -> {
          // 1. Dọn dẹp giỏ hàng via CartPort
          if (userId != null && variantIds != null && !variantIds.isEmpty()) {
            cartPort.clearCartItems(userId, variantIds);
          }

          // 2. Gửi thông báo cho khách hàng via NotificationPort
          if (userId != null && orderId != null) {
            String notifTitle = isTransfer ? "Đơn hàng đang chờ thanh toán" : "Đặt hàng thành công";
            String notifContent =
                isTransfer
                    ? "Đơn hàng "
                        + orderCode
                        + " đã được tạo. Vui lòng chuyển khoản trong vòng "
                        + expireMinutes
                        + " phút."
                    : "Đơn hàng " + orderCode + " đã được tạo thành công và đang chờ xác nhận.";

            notificationPort.sendNotification(userId, orderId, notifTitle, notifContent);
          }
        });
  }
}
