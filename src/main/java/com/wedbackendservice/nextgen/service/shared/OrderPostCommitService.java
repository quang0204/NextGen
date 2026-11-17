package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.entity.Notification;
import com.wedbackendservice.nextgen.entity.Order;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.repository.CartItemRepository;
import com.wedbackendservice.nextgen.repository.CartRepository;
import com.wedbackendservice.nextgen.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderPostCommitService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final NotificationRepository notificationRepository;

    /**
     * Dọn giỏ hàng và gửi notification cho khách hàng bất đồng bộ sau khi đơn hàng đã COMMIT thành công.
     * Hoàn toàn độc lập, đảm bảo tuyệt đối không làm ảnh hưởng đến HTTP response của đơn hàng.
     */
    public void cleanupCartAndSendNotification(Long userId, Collection<Long> variantIds,
                                               Long orderId, String orderCode,
                                               boolean isTransfer, int expireMinutes) {
        CompletableFuture.runAsync(() -> {
            // 1. Dọn dẹp giỏ hàng
            if (userId != null && variantIds != null && !variantIds.isEmpty()) {
                try {
                    cartRepository.findByUserId(userId).ifPresent(cart -> {
                        for (Long vId : variantIds) {
                            cartItemRepository.findByCartIdAndVariantId(cart.getCartId(), vId)
                                    .ifPresent(cartItemRepository::delete);
                        }
                    });
                } catch (Exception ex) {
                    log.warn("[OrderPostCommitService] Dọn giỏ hàng thất bại cho user {}: {}", userId, ex.getMessage());
                }
            }

            // 2. Gửi thông báo cho khách hàng
            if (userId != null && orderId != null) {
                try {
                    String notifTitle = isTransfer ? "Đơn hàng đang chờ thanh toán" : "Đặt hàng thành công";
                    String notifContent = isTransfer
                            ? "Đơn hàng " + orderCode + " đã được tạo. Vui lòng chuyển khoản trong vòng " + expireMinutes + " phút."
                            : "Đơn hàng " + orderCode + " đã được tạo thành công và đang chờ xác nhận.";

                    notificationRepository.save(Notification.builder()
                            .user(User.builder().userId(userId).build())
                            .order(Order.builder().orderId(orderId).build())
                            .title(notifTitle)
                            .content(notifContent)
                            .isRead(false)
                            .build());
                } catch (Exception ex) {
                    log.warn("[OrderPostCommitService] Gửi thông báo đặt hàng thất bại cho user {}: {}", userId, ex.getMessage());
                }
            }
        });
    }
}
