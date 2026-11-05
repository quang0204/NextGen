package com.wedbackendservice.nextgen.service.payment;

import com.wedbackendservice.nextgen.configuration.BankTransferConfig;
import com.wedbackendservice.nextgen.configuration.MomoConfig;
import com.wedbackendservice.nextgen.configuration.VNPayConfig;
import com.wedbackendservice.nextgen.dto.request.payment.CreatePaymentIntentRequest;
import com.wedbackendservice.nextgen.dto.request.payment.MomoIpnRequest;
import com.wedbackendservice.nextgen.dto.response.payment.PaymentIntentResponse;
import com.wedbackendservice.nextgen.dto.response.payment.VnpayIpnResponse;
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
import java.util.Map;
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
    private final VNPayService vnPayService;
    private final MomoService momoService;
    private final VNPayConfig vnPayConfig;
    private final MomoConfig momoConfig;
    private final BankTransferConfig bankTransferConfig;

    /**
     * Tạo yêu cầu thanh toán cho một đơn hàng.
     * - VNPAY / MOMO: trả paymentUrl để FE redirect.
     * - BANK_TRANSFER: trả thông tin tài khoản + nội dung chuyển khoản.
     * - COD: ghi nhận thanh toán khi nhận hàng.
     */
    @Transactional
    public PaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest request, String clientIp) {
        User user = securityHelper.getCurrentUser();
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        boolean isOwner = order.getUser() != null && user.getUserId().equals(order.getUser().getUserId());
        if (!isOwner && user.getRole() != Role.ADMIN) {
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
                    .paymentProvider(resolveProvider(method))
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
                .paymentProvider(payment.getPaymentProvider())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .transactionRef(payment.getTransactionRef())
                .createdAt(payment.getCreatedAt());

        switch (method) {
            case VNPAY -> builder
                    .paymentUrl(vnPayService.buildPaymentUrl(payment, order, clientIp, request.getReturnUrl()))
                    .expiresAt(LocalDateTime.now().plusMinutes(Math.max(vnPayConfig.getExpireMinutes(), 1)))
                    .message("Tạo yêu cầu thanh toán VNPay thành công.");
            case MOMO -> builder
                    .paymentUrl(momoService.createPaymentUrl(payment, order, request.getReturnUrl()))
                    .expiresAt(LocalDateTime.now().plusMinutes(Math.max(momoConfig.getExpireMinutes(), 1)))
                    .message("Tạo yêu cầu thanh toán MoMo thành công.");
            case BANK_TRANSFER -> {
                if (!bankTransferConfig.isConfigured()) {
                    throw new AppException(ErrorCode.INVALID_REQUEST,
                            "Chưa cấu hình tài khoản nhận chuyển khoản (payment.bank-transfer).");
                }
                builder.bankName(bankTransferConfig.getBankName())
                        .bankAccountNumber(bankTransferConfig.getAccountNumber())
                        .bankAccountName(bankTransferConfig.getAccountName())
                        .bankBranch(bankTransferConfig.getBranch())
                        .transferContent(bankTransferConfig.buildTransferContent(payment.getTransactionRef()))
                        .expiresAt(LocalDateTime.now().plusMinutes(Math.max(bankTransferConfig.getExpireMinutes(), 1)))
                        .message("Vui lòng chuyển khoản đúng nội dung để được xác nhận tự động.");
            }
            case COD -> builder.message("Đơn hàng sẽ được thanh toán khi giao hàng (COD).");
        }
        return builder.build();
    }

    @Transactional(readOnly = true)
    public PaymentIntentResponse getByOrderId(Long orderId) {
        User user = securityHelper.getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        boolean isOwner = order.getUser() != null && user.getUserId().equals(order.getUser().getUserId());
        if (!isOwner && user.getRole() != Role.ADMIN && user.getRole() != Role.STAFF) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        Payment payment = paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "Đơn hàng chưa có giao dịch thanh toán."));
        return toIntentResponse(payment, null);
    }

    /**
     * Xử lý redirect VNPay về FE (chỉ hiển thị kết quả; trạng thái chuẩn do IPN cập nhật).
     */
    @Transactional
    public PaymentIntentResponse handleVnpayReturn(Map<String, String> params) {
        if (!vnPayService.verifySignature(params)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Chữ ký VNPay không hợp lệ.");
        }
        Payment payment = paymentRepository.findByTransactionRef(params.get("vnp_TxnRef"))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "Không tìm thấy giao dịch thanh toán."));
        boolean success = vnPayService.isSuccessResponse(params);
        updatePaymentStatus(payment, success);
        return toIntentResponse(payment, success
                ? "Thanh toán VNPay thành công."
                : "Thanh toán VNPay không thành công.");
    }

    /**
     * Nhận IPN VNPay: xác thực chữ ký, đối soát số tiền, cập nhật trạng thái, trả RspCode chuẩn.
     */
    @Transactional
    public VnpayIpnResponse handleVnpayIpn(Map<String, String> params) {
        if (!vnPayService.verifySignature(params)) {
            return VnpayIpnResponse.of("97", "Invalid signature");
        }
        Payment payment = paymentRepository.findByTransactionRef(params.get("vnp_TxnRef")).orElse(null);
        if (payment == null) {
            return VnpayIpnResponse.of("01", "Order not found");
        }
        if (PaymentStatus.from(payment.getStatus()) == PaymentStatus.SUCCESS) {
            return VnpayIpnResponse.of("02", "Order already confirmed");
        }
        BigDecimal paidAmount = vnPayService.extractAmount(params);
        if (paidAmount == null || payment.getAmount() == null || paidAmount.compareTo(payment.getAmount()) != 0) {
            return VnpayIpnResponse.of("04", "Invalid amount");
        }
        updatePaymentStatus(payment, vnPayService.isSuccessResponse(params));
        return VnpayIpnResponse.of("00", "Confirm Success");
    }

    /**
     * Xử lý redirect MoMo về FE.
     */
    @Transactional
    public PaymentIntentResponse handleMomoReturn(Map<String, String> params) {
        if (!momoService.verifySignature(params)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Chữ ký MoMo không hợp lệ.");
        }
        Payment payment = paymentRepository.findByTransactionRef(params.get("orderId"))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "Không tìm thấy giao dịch thanh toán."));
        boolean success = momoService.isSuccessResult(params);
        updatePaymentStatus(payment, success);
        return toIntentResponse(payment, success
                ? "Thanh toán MoMo thành công."
                : "Thanh toán MoMo không thành công.");
    }

    /**
     * Nhận IPN MoMo (server-to-server).
     */
    @Transactional
    public PaymentIntentResponse handleMomoIpn(MomoIpnRequest request) {
        Map<String, String> params = request.toParams();
        if (!momoService.verifySignature(params)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Chữ ký MoMo không hợp lệ.");
        }
        Payment payment = paymentRepository.findByTransactionRef(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "Không tìm thấy giao dịch thanh toán."));
        boolean success = momoService.isSuccessResult(params);
        updatePaymentStatus(payment, success);
        return toIntentResponse(payment, success
                ? "Thanh toán MoMo thành công."
                : "Thanh toán MoMo không thành công.");
    }

    private void updatePaymentStatus(Payment payment, boolean success) {
        PaymentStatus current = PaymentStatus.from(payment.getStatus());
        if (current == PaymentStatus.SUCCESS || current == PaymentStatus.REFUNDED) {
            return;
        }
        if (success) {
            payment.setStatus(PaymentStatus.SUCCESS.name());
            payment.setPaidAt(LocalDateTime.now());
        } else {
            payment.setStatus(PaymentStatus.FAILED.name());
        }
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        if (order != null) {
            order.setPaymentStatus(success ? ORDER_PAID : PaymentStatus.FAILED.name());
            orderRepository.save(order);
        }
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

    private static String resolveProvider(PaymentMethod method) {
        return switch (method) {
            case VNPAY -> "VNPAY";
            case MOMO -> "MOMO";
            case BANK_TRANSFER -> "BANK";
            case COD -> "INTERNAL";
        };
    }

    private PaymentIntentResponse toIntentResponse(Payment payment, String message) {
        Order order = payment.getOrder();
        return PaymentIntentResponse.builder()
                .paymentId(payment.getPaymentId())
                .orderId(order != null ? order.getOrderId() : null)
                .orderCode(order != null ? order.getOrderCode() : null)
                .paymentMethod(payment.getPaymentMethod())
                .paymentProvider(payment.getPaymentProvider())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .transactionRef(payment.getTransactionRef())
                .message(message)
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
