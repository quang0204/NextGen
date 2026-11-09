package com.wedbackendservice.nextgen.service.client;

import com.wedbackendservice.nextgen.configuration.BankTransferConfig;
import com.wedbackendservice.nextgen.dto.client.request.payment.CreatePaymentIntentRequest;
import com.wedbackendservice.nextgen.dto.client.request.payment.SimulateCallbackRequest;
import com.wedbackendservice.nextgen.dto.client.response.payment.PaymentIntentResponse;
import com.wedbackendservice.nextgen.dto.client.response.payment.SimulateCallbackResponse;
import com.wedbackendservice.nextgen.entity.Order;
import com.wedbackendservice.nextgen.entity.Payment;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.PaymentMethod;
import com.wedbackendservice.nextgen.enums.PaymentStatus;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.OrderRepository;
import com.wedbackendservice.nextgen.repository.PaymentRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final SecurityHelper securityHelper;
    private final BankTransferConfig bankTransferConfig;

    /**
     * API 10.1: tạo thanh toán QR mô phỏng (BANK_TRANSFER) hoặc COD.
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
        BigDecimal amount = order.getFinalAmount() != null ? order.getFinalAmount() : order.getTotalAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Số tiền thanh toán không hợp lệ.");
        }

        Payment payment = findReusablePending(order, method);
        if (payment == null) {
            payment = Payment.builder()
                    .order(order)
                    .paymentMethod(method.name())
                    .paymentProvider(method == PaymentMethod.COD ? "INTERNAL" : "SIMULATED_QR")
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

        if (method == PaymentMethod.BANK_TRANSFER) {
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
        } else {
            builder.message("Đơn hàng sẽ được thanh toán khi giao hàng (COD).");
        }
        return builder.build();
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
        if (PaymentStatus.from(payment.getStatus()) != PaymentStatus.SUCCESS) {
            payment.setStatus(success ? PaymentStatus.SUCCESS.name() : PaymentStatus.FAILED.name());
            if (success) {
                payment.setPaidAt(LocalDateTime.now());
            }
            paymentRepository.save(payment);

            if (order != null) {
                order.setPaymentStatus(success ? ORDER_PAID : PaymentStatus.FAILED.name());
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
                .filter(p -> PaymentStatus.from(p.getStatus()) == PaymentStatus.PENDING)
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
