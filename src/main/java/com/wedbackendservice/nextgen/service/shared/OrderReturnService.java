package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.dto.client.response.order.OrderDetailResponse;
import com.wedbackendservice.nextgen.entity.Order;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.OrderStatus;
import com.wedbackendservice.nextgen.enums.PaymentStatus;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.OrderRepository;
import com.wedbackendservice.nextgen.repository.PaymentRepository;
import com.wedbackendservice.nextgen.repository.VoucherRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderReturnService {

    private final SecurityHelper securityHelper;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final VoucherRepository voucherRepository;
    private final InventoryService inventoryService;
    private final OrderStateService orderStateService;
    private final OrdersQueryService ordersQueryService;
    private final Clock clock;

    @Transactional
    public OrderDetailResponse receiveReturnOrder(Long orderId, String note) {
        User operator = securityHelper.authenticateStaffOrAdmin();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ERR_ORDER_NOT_FOUND, "Không tìm thấy đơn hàng ID " + orderId + "!"));

        String currentStatus = order.getStatus();
        if (!OrderStatus.RETURNING.name().equalsIgnoreCase(currentStatus)) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Chỉ có thể xác nhận nhận hàng hoàn cho đơn hàng đang ở trạng thái RETURNING (Bưu tá giao không thành công)!");
        }

        LocalDateTime now = LocalDateTime.now(clock);

        // Kiểm tra và thực hiện hoàn kho nếu chưa hoàn (MB04) qua InventoryService
        inventoryService.restockReturnOrder(order.getOrderId(), operator, note);

        // Hoàn trả voucher nếu có
        if (order.getVoucher() != null) {
            voucherRepository.decrementUsedCountAtomic(order.getVoucher().getVoucherId());
        }

        boolean needRefund = "PAID".equalsIgnoreCase(order.getPaymentStatus())
                || PaymentStatus.SUCCESS.name().equalsIgnoreCase(order.getPaymentStatus())
                || PaymentStatus.REFUND_PENDING.name().equalsIgnoreCase(order.getPaymentStatus());
        if (needRefund) {
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING.name());
        } else {
            order.setPaymentStatus(PaymentStatus.CANCELLED.name());
        }

        String finalReason = (note != null && !note.trim().isBlank())
                ? note.trim()
                : (order.getCancelReason() != null ? order.getCancelReason() : "Bưu tá hoàn hàng về kho");
        order.setCancelReason(finalReason);
        orderStateService.transition(order, OrderStatus.CANCELLED, operator, "Xác nhận nhận hàng hoàn về kho (MB04): " + finalReason);
        Order saved = orderRepository.save(order);

        paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(order.getOrderId()).ifPresent(payment -> {
            if (needRefund) {
                payment.setStatus(PaymentStatus.REFUND_PENDING.name());
            } else {
                payment.setStatus(PaymentStatus.CANCELLED.name());
            }
            paymentRepository.save(payment);
        });

        return ordersQueryService.getOrderDetail(saved.getOrderCode());
    }
}
