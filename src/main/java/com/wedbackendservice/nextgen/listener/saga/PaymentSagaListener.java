package com.wedbackendservice.nextgen.listener.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedbackendservice.nextgen.entity.Order;
import com.wedbackendservice.nextgen.entity.Payment;
import com.wedbackendservice.nextgen.enums.PaymentStatus;
import com.wedbackendservice.nextgen.event.saga.KafkaTopics;
import com.wedbackendservice.nextgen.event.saga.PaymentResultEvent;
import com.wedbackendservice.nextgen.event.saga.StockReservedEvent;
import com.wedbackendservice.nextgen.repository.OrderRepository;
import com.wedbackendservice.nextgen.repository.PaymentRepository;
import com.wedbackendservice.nextgen.service.shared.InboxService;
import com.wedbackendservice.nextgen.service.shared.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSagaListener {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OutboxService outboxService;
    private final InboxService inboxService;
    private final ObjectMapper objectMapper;

    /**
     * Bước 7-9: Nhận StockReserved -> Khởi tạo Payment theo phương thức -> Bắn sự kiện tiếp theo
     */
    @KafkaListener(topics = KafkaTopics.STOCK_RESERVED, groupId = "nextgen-payment-saga")
    @Transactional
    public void handleStockReserved(String payload) {
        try {
            StockReservedEvent event = objectMapper.readValue(payload, StockReservedEvent.class);
            String consumerGroup = "nextgen-payment-saga";

            if (inboxService.isAlreadyProcessed(event.getEventId(), consumerGroup)) {
                log.info("[PaymentSaga] Event {} already processed, skipping", event.getEventId());
                return;
            }

            log.info("[PaymentSaga] Received StockReserved for order #{}, stock success: {}", event.getOrderCode(), event.isSuccess());

            if (!event.isSuccess()) {
                log.warn("[PaymentSaga] Stock reservation failed, skipping payment for order #{}", event.getOrderCode());
                PaymentResultEvent failedEvent = PaymentResultEvent.builder()
                        .correlationId(event.getCorrelationId())
                        .orderId(event.getOrderId())
                        .orderCode(event.getOrderCode())
                        .success(false)
                        .paymentMethod(event.getPaymentMethod())
                        .amount(event.getTotalAmount())
                        .message("Không thể thanh toán do giữ tồn kho thất bại: " + event.getMessage())
                        .items(event.getItems())
                        .build();

                outboxService.saveEvent("PAYMENT", event.getOrderCode(), "PaymentFailed",
                        KafkaTopics.PAYMENT_EVENTS, failedEvent);

                inboxService.markAsProcessed(event.getEventId(), consumerGroup, "StockReservationFailed", event.getOrderCode());
                return;
            }

            String payMethod = event.getPaymentMethod() != null ? event.getPaymentMethod().toUpperCase() : "COD";
            boolean isBankTransfer = "BANK_TRANSFER".equals(payMethod) || "SIMULATED_TRANSFER".equals(payMethod);
            String txnRef = "TXN-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);

            Optional<Order> orderOpt = orderRepository.findById(event.getOrderId());
            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();

                if (isBankTransfer) {
                    // BANK_TRANSFER: Tạo Payment PENDING để hiển thị QR, khách giả lập thanh toán
                    Payment payment = Payment.builder()
                            .order(order)
                            .paymentMethod("BANK_TRANSFER")
                            .paymentProvider("SIMULATED_QR")
                            .amount(event.getTotalAmount())
                            .transactionRef(txnRef)
                            .status(PaymentStatus.PENDING.name())
                            .paidAt(null)
                            .build();
                    paymentRepository.save(payment);

                    order.setStatus("PENDING_PAYMENT");
                    order.setPaymentStatus(PaymentStatus.UNPAID.name());
                    order.setUpdatedAt(LocalDateTime.now());
                    orderRepository.save(order);

                    log.info("[PaymentSaga] Created PENDING payment with QR demo for order #{}", order.getOrderCode());
                } else {
                    // COD: Tạo Payment UNPAID theo dõi công nợ, không tạo QR, order WAITING_CONFIRMATION
                    Payment payment = Payment.builder()
                            .order(order)
                            .paymentMethod("COD")
                            .paymentProvider("COD_SHIPPER")
                            .amount(event.getTotalAmount())
                            .transactionRef("COD-" + order.getOrderCode())
                            .status(PaymentStatus.UNPAID.name())
                            .paidAt(null)
                            .build();
                    paymentRepository.save(payment);

                    order.setStatus("WAITING_CONFIRMATION");
                    order.setPaymentStatus(PaymentStatus.UNPAID.name());
                    order.setUpdatedAt(LocalDateTime.now());
                    orderRepository.save(order);

                    // Phát sự kiện COD đã sẵn sàng chờ duyệt
                    PaymentResultEvent resultEvent = PaymentResultEvent.builder()
                            .correlationId(event.getCorrelationId())
                            .orderId(event.getOrderId())
                            .orderCode(event.getOrderCode())
                            .success(true)
                            .paymentMethod("COD")
                            .amount(event.getTotalAmount())
                            .transactionRef(payment.getTransactionRef())
                            .message("Khởi tạo đơn hàng COD thành công, chờ nhân viên xác nhận")
                            .items(event.getItems())
                            .build();

                    outboxService.saveEvent("PAYMENT", event.getOrderCode(), "PaymentSucceeded",
                            KafkaTopics.PAYMENT_EVENTS, resultEvent);

                    log.info("[PaymentSaga] Created UNPAID COD payment for order #{}", order.getOrderCode());
                }
            }

            inboxService.markAsProcessed(event.getEventId(), consumerGroup, "StockReserved", event.getOrderCode());

        } catch (Exception e) {
            log.error("[PaymentSaga] Error processing StockReserved: {}", e.getMessage(), e);
        }
    }
}
