package com.wedbackendservice.nextgen.service.client;

import com.wedbackendservice.nextgen.configuration.BankTransferConfig;
import com.wedbackendservice.nextgen.dto.client.request.payment.CreatePaymentIntentRequest;
import com.wedbackendservice.nextgen.dto.client.request.payment.SimulateCallbackRequest;
import com.wedbackendservice.nextgen.dto.client.response.payment.PaymentIntentResponse;
import com.wedbackendservice.nextgen.dto.client.response.payment.SimulateCallbackResponse;
import com.wedbackendservice.nextgen.entity.Order;
import com.wedbackendservice.nextgen.entity.OrderStatusHistory;
import com.wedbackendservice.nextgen.entity.Payment;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.OrderStatus;
import com.wedbackendservice.nextgen.enums.PaymentMethod;
import com.wedbackendservice.nextgen.enums.PaymentStatus;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.OrderRepository;
import com.wedbackendservice.nextgen.repository.OrderStatusHistoryRepository;
import com.wedbackendservice.nextgen.repository.PaymentRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import com.wedbackendservice.nextgen.event.saga.KafkaTopics;
import com.wedbackendservice.nextgen.service.shared.InventoryService;
import com.wedbackendservice.nextgen.service.shared.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final DateTimeFormatter REF_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int PENDING_REUSE_MINUTES = 15;
    private static final String ORDER_PAID = "PAID";

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final SecurityHelper securityHelper;
    private final BankTransferConfig bankTransferConfig;
    private final InventoryService inventoryService;
    private final OutboxService outboxService;

    /**
     * API 10.1: tạo thanh toán QR mô phỏng (chỉ dành cho BANK_TRANSFER).
     * COD tuyệt đối không hiển thị QR và không yêu cầu thanh toán trước.
     */
    @Transactional
    public PaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest request) {
        User user = securityHelper.getCurrentUser();
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        boolean owner = order.getUser() != null && user.getUserId().equals(order.getUser().getUserId());
        if (!owner && user.getRole() != Role.ADMIN) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (ORDER_PAID.equalsIgnoreCase(order.getPaymentStatus())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Đơn hàng đã được thanh toán.");
        }
        if ("CANCELLED".equalsIgnoreCase(order.getStatus()) || "REFUNDED".equalsIgnoreCase(order.getStatus())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Đơn hàng đã huỷ, không thể thanh toán.");
        }

        PaymentMethod method = request.getPaymentMethod();
        if (method == PaymentMethod.COD) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Phương thức COD thanh toán khi nhận hàng, không hỗ trợ tạo mã QR hoặc thanh toán trước.");
        }

        BigDecimal amount = order.getFinalAmount() != null ? order.getFinalAmount() : order.getTotalAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Số tiền thanh toán không hợp lệ.");
        }

        Payment payment = findReusablePending(order, method);
        if (payment == null) {
            payment = Payment.builder()
                    .order(order)
                    .paymentMethod(method.name())
                    .paymentProvider("SIMULATED_QR")
                    .amount(amount)
                    .transactionRef(generateTransactionRef())
                    .status(PaymentStatus.PENDING.name())
                    .build();
            payment = paymentRepository.save(payment);
        }

        PaymentIntentResponse.PaymentIntentResponseBuilder builder = PaymentIntentResponse.builder()
                .paymentId(payment.getPaymentId())
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .transactionRef(payment.getTransactionRef())
                .createdAt(payment.getCreatedAt());

        if (!bankTransferConfig.isConfigured()) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Chưa cấu hình tài khoản nhận chuyển khoản (payment.bank-transfer).");
        }
        builder.bankName(bankTransferConfig.getBankName())
                .bankAccountNumber(bankTransferConfig.getAccountNumber())
                .bankAccountName(bankTransferConfig.getAccountName())
                .bankBranch(bankTransferConfig.getBranch())
                .transferContent(bankTransferConfig.buildTransferContent(payment.getTransactionRef()))
                .qrContent(bankTransferConfig.buildQrContent(payment.getTransactionRef()))
                .expiresAt(LocalDateTime.now().plusMinutes(Math.max(bankTransferConfig.getExpireMinutes(), 1)))
                .message("Quét mã QR hoặc chuyển khoản đúng nội dung để thanh toán.");

        return builder.build();
    }

    /**
     * API Giả lập thanh toán thành công (Simulate Success)
     * POST /api/v1/payments/{paymentId}/simulate-success
     */
    @Transactional
    public SimulateCallbackResponse simulateSuccess(Long paymentId) {
        User user = securityHelper.getCurrentUser();
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "Không tìm thấy giao dịch thanh toán ID " + paymentId));

        Order order = payment.getOrder();
        if (order == null) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }

        boolean isOwner = order.getUser() != null && user.getUserId().equals(order.getUser().getUserId());
        boolean isStaffOrAdmin = user.getRole() == Role.ADMIN || user.getRole() == Role.STAFF;
        if (!isOwner && !isStaffOrAdmin) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if ("COD".equalsIgnoreCase(payment.getPaymentMethod())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Đơn hàng COD thanh toán khi nhận hàng, không giả lập thanh toán QR.");
        }

        if (PaymentStatus.SUCCESS.name().equalsIgnoreCase(payment.getStatus())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Giao dịch đã được thanh toán thành công trước đó.");
        }

        if ("CANCELLED".equalsIgnoreCase(order.getStatus()) || "EXPIRED".equalsIgnoreCase(payment.getStatus())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Đơn hàng đã bị hủy hoặc hết hạn, không thể thanh toán.");
        }

        LocalDateTime now = LocalDateTime.now();
        payment.setStatus(PaymentStatus.SUCCESS.name());
        payment.setPaidAt(now);
        paymentRepository.save(payment);

        String prevStatus = order.getStatus();
        order.setPaymentStatus("PAID");
        order.setStatus(OrderStatus.WAITING_CONFIRMATION.name());
        order.setUpdatedAt(now);
        orderRepository.save(order);

        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .changedBy(user)
                .previousStatus(prevStatus)
                .newStatus(OrderStatus.WAITING_CONFIRMATION.name())
                .note("Thanh toán QR Demo thành công (Giao dịch: " + payment.getTransactionRef() + "). Đơn hàng chuyển sang Chờ xác nhận.")
                .build());

        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("paymentId", payment.getPaymentId());
        payload.put("orderId", order.getOrderId());
        payload.put("orderCode", order.getOrderCode() != null ? order.getOrderCode() : "");
        payload.put("transactionRef", payment.getTransactionRef() != null ? payment.getTransactionRef() : "");
        payload.put("amount", payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO);
        payload.put("status", "SUCCESS");
        payload.put("paidAt", now.toString());

        outboxService.saveProcessedEvent("PAYMENT",
                order.getOrderCode() != null ? order.getOrderCode() : String.valueOf(order.getOrderId()),
                "PaymentSucceeded", KafkaTopics.PAYMENT_EVENTS, payload);

        return SimulateCallbackResponse.builder()
                .paymentId(payment.getPaymentId())
                .transactionRef(payment.getTransactionRef())
                .status(PaymentStatus.SUCCESS.name())
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .orderPaymentStatus("PAID")
                .paidAt(now)
                .message("Thanh toán QR Demo thành công! Đơn hàng đã chuyển sang trạng thái Chờ xác nhận.")
                .build();
    }

    /**
     * API Giả lập thanh toán thất bại (Simulate Failure)
     * POST /api/v1/payments/{paymentId}/simulate-failure
     */
    @Transactional
    public SimulateCallbackResponse simulateFailure(Long paymentId, String reason) {
        User user = securityHelper.getCurrentUser();
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "Không tìm thấy giao dịch thanh toán ID " + paymentId));

        Order order = payment.getOrder();
        if (order == null) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }

        boolean isOwner = order.getUser() != null && user.getUserId().equals(order.getUser().getUserId());
        boolean isStaffOrAdmin = user.getRole() == Role.ADMIN || user.getRole() == Role.STAFF;
        if (!isOwner && !isStaffOrAdmin) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (PaymentStatus.SUCCESS.name().equalsIgnoreCase(payment.getStatus())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Giao dịch đã thanh toán thành công, không thể giả lập thất bại.");
        }

        LocalDateTime now = LocalDateTime.now();
        payment.setStatus(PaymentStatus.FAILED.name());
        paymentRepository.save(payment);

        String failReason = reason != null && !reason.isBlank() ? reason.trim() : "Khách hàng hủy hoặc thanh toán không thành công";

        // Giải phóng tồn kho đúng 1 lần
        inventoryService.releaseStockForOrder(order.getOrderId(), failReason);

        String prevStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED.name());
        order.setCancelReason("Thanh toán thất bại: " + failReason);
        order.setUpdatedAt(now);
        orderRepository.save(order);

        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .changedBy(user)
                .previousStatus(prevStatus)
                .newStatus(OrderStatus.CANCELLED.name())
                .note("Thanh toán QR Demo thất bại. Hệ thống đã tự động hủy đơn và giải phóng tồn kho.")
                .build());

        Map<String, Object> failPayload = new java.util.HashMap<>();
        failPayload.put("paymentId", payment.getPaymentId());
        failPayload.put("orderId", order.getOrderId());
        failPayload.put("orderCode", order.getOrderCode() != null ? order.getOrderCode() : "");
        failPayload.put("transactionRef", payment.getTransactionRef() != null ? payment.getTransactionRef() : "");
        failPayload.put("reason", failReason);
        failPayload.put("status", "FAILED");

        outboxService.saveProcessedEvent("PAYMENT",
                order.getOrderCode() != null ? order.getOrderCode() : String.valueOf(order.getOrderId()),
                "PaymentFailed", KafkaTopics.PAYMENT_EVENTS, failPayload);

        return SimulateCallbackResponse.builder()
                .paymentId(payment.getPaymentId())
                .transactionRef(payment.getTransactionRef())
                .status(PaymentStatus.FAILED.name())
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .orderPaymentStatus("FAILED")
                .paidAt(null)
                .message("Đã ghi nhận thanh toán thất bại. Đơn hàng đã được hủy và tồn kho đã được giải phóng.")
                .build();
    }

    /**
     * API 10.2: mô phỏng callback xác nhận thanh toán.
     */
    @Transactional
    public SimulateCallbackResponse simulateCallback(SimulateCallbackRequest request) {
        securityHelper.authenticateAdmin();
        Payment payment = paymentRepository.findByTransactionRef(request.getTransactionRef())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST,
                        "Không tìm thấy giao dịch thanh toán."));

        Order order = payment.getOrder();
        if (request.getAmount() != null && payment.getAmount() != null
                && request.getAmount().compareTo(payment.getAmount()) != 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Số tiền callback không khớp giao dịch.");
        }

        boolean success = request.getSuccess() == null || request.getSuccess();
        if (!PaymentStatus.SUCCESS.name().equalsIgnoreCase(payment.getStatus())) {
            payment.setStatus(success ? PaymentStatus.SUCCESS.name() : PaymentStatus.FAILED.name());
            if (success) {
                payment.setPaidAt(LocalDateTime.now());
            }
            paymentRepository.save(payment);

            if (order != null) {
                order.setPaymentStatus(success ? ORDER_PAID : PaymentStatus.FAILED.name());
                if (success && OrderStatus.PENDING_PAYMENT.name().equalsIgnoreCase(order.getStatus())) {
                    String prevStatus = order.getStatus();
                    order.setStatus(OrderStatus.WAITING_CONFIRMATION.name());
                    orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                            .order(order)
                            .previousStatus(prevStatus)
                            .newStatus(OrderStatus.WAITING_CONFIRMATION.name())
                            .note("Thanh toán chuyển khoản thành công qua callback (" + payment.getTransactionRef() + ")")
                            .build());
                }
                orderRepository.save(order);
            }
        }

        return SimulateCallbackResponse.builder()
                .paymentId(payment.getPaymentId())
                .transactionRef(payment.getTransactionRef())
                .status(payment.getStatus())
                .orderId(order != null ? order.getOrderId() : null)
                .orderCode(order != null ? order.getOrderCode() : null)
                .orderPaymentStatus(order != null ? order.getPaymentStatus() : null)
                .paidAt(payment.getPaidAt())
                .message(success ? "Xác nhận thanh toán thành công." : "Đã ghi nhận thanh toán thất bại.")
                .build();
    }

    private Payment findReusablePending(Order order, PaymentMethod method) {
        return paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(order.getOrderId())
                .filter(p -> PaymentStatus.PENDING.name().equalsIgnoreCase(p.getStatus()))
                .filter(p -> method.name().equalsIgnoreCase(p.getPaymentMethod()))
                .filter(p -> p.getCreatedAt() == null
                        || p.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(PENDING_REUSE_MINUTES)))
                .orElse(null);
    }

    private String generateTransactionRef() {
        String ref;
        do {
            ref = "PAY" + LocalDateTime.now().format(REF_TIME)
                    + String.format("%04d", ThreadLocalRandom.current().nextInt(10_000));
        } while (paymentRepository.existsByTransactionRef(ref));
        return ref;
    }
}
