package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.dto.response.PageInfoResponse;
import com.wedbackendservice.nextgen.dto.response.admin.AdminPaymentItemResponse;
import com.wedbackendservice.nextgen.dto.response.admin.AdminPaymentListResponse;
import com.wedbackendservice.nextgen.dto.response.admin.PaymentLedgerSummaryResponse;
import com.wedbackendservice.nextgen.entity.Order;
import com.wedbackendservice.nextgen.entity.Payment;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.PaymentMethod;
import com.wedbackendservice.nextgen.enums.PaymentStatus;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.PaymentRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPaymentsService {

    private final PaymentRepository paymentRepository;
    private final SecurityHelper securityHelper;

    /**
     * API 10.3: sổ cái thu chi và đối soát thanh toán.
     */
    @Transactional(readOnly = true)
    public AdminPaymentListResponse getPayments(int page, int size, String status, String method,
                                                String search, LocalDate fromDate, LocalDate toDate) {
        securityHelper.authenticateStaffOrAdmin();

        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Payment> result = paymentRepository.findAll(
                buildSpecification(status, method, search, fromDate, toDate), pageable);

        return AdminPaymentListResponse.builder()
                .items(result.getContent().stream().map(this::toItem).toList())
                .pageInfo(PageInfoResponse.of(safePage, safeSize, result))
                .build();
    }

    @Transactional(readOnly = true)
    public PaymentLedgerSummaryResponse getLedgerSummary(LocalDate fromDate, LocalDate toDate) {
        securityHelper.authenticateStaffOrAdmin();

        List<Payment> payments = paymentRepository.findAll(
                buildSpecification(null, null, null, fromDate, toDate));

        long total = 0;
        long success = 0;
        long pending = 0;
        long failed = 0;
        long refunded = 0;
        BigDecimal totalCollected = BigDecimal.ZERO;
        BigDecimal totalPending = BigDecimal.ZERO;
        BigDecimal totalRefunded = BigDecimal.ZERO;
        Map<String, BigDecimal> collectedByMethod = new TreeMap<>();

        for (Payment payment : payments) {
            total++;
            BigDecimal amount = payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount();
            PaymentStatus paymentStatus = PaymentStatus.from(payment.getStatus());

            if (paymentStatus == PaymentStatus.SUCCESS) {
                success++;
                totalCollected = totalCollected.add(amount);
                String method = payment.getPaymentMethod() == null ? "UNKNOWN" : payment.getPaymentMethod();
                collectedByMethod.merge(method, amount, BigDecimal::add);
            } else if (paymentStatus == PaymentStatus.PENDING) {
                pending++;
                totalPending = totalPending.add(amount);
            } else if (paymentStatus == PaymentStatus.REFUNDED) {
                refunded++;
                totalRefunded = totalRefunded.add(amount);
            } else {
                failed++;
            }
        }

        return PaymentLedgerSummaryResponse.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .totalTransactions(total)
                .successTransactions(success)
                .pendingTransactions(pending)
                .failedTransactions(failed)
                .refundedTransactions(refunded)
                .totalCollected(totalCollected)
                .totalPending(totalPending)
                .totalRefunded(totalRefunded)
                .collectedByMethod(collectedByMethod)
                .build();
    }

    private Specification<Payment> buildSpecification(String status, String method, String search,
                                                      LocalDate fromDate, LocalDate toDate) {
        validateStatus(status);
        validateMethod(method);

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }
            if (method != null && !method.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("paymentMethod")), method.trim().toUpperCase()));
            }
            if (search != null && !search.isBlank()) {
                query.distinct(true);
                Join<Payment, Order> orderJoin = root.join("order", JoinType.LEFT);
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(orderJoin.get("orderCode")), like),
                        cb.like(cb.lower(root.get("transactionRef")), like)));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("createdAt"), fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<LocalDateTime>get("createdAt"), toDate.atTime(LocalTime.MAX)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AdminPaymentItemResponse toItem(Payment payment) {
        Order order = payment.getOrder();
        User customer = order != null ? order.getUser() : null;
        return AdminPaymentItemResponse.builder()
                .paymentId(payment.getPaymentId())
                .orderId(order != null ? order.getOrderId() : null)
                .orderCode(order != null ? order.getOrderCode() : null)
                .customerName(customer != null ? customer.getFullName() : null)
                .customerPhone(customer != null ? customer.getPhone() : null)
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .transactionRef(payment.getTransactionRef())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private static void validateStatus(String status) {
        if (status != null && !status.isBlank() && PaymentStatus.from(status) == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Trạng thái thanh toán không hợp lệ.");
        }
    }

    private static void validateMethod(String method) {
        if (method == null || method.isBlank()) {
            return;
        }
        for (PaymentMethod value : PaymentMethod.values()) {
            if (value.name().equalsIgnoreCase(method.trim())) {
                return;
            }
        }
        throw new AppException(ErrorCode.INVALID_REQUEST, "Phương thức thanh toán không hợp lệ.");
    }
}
