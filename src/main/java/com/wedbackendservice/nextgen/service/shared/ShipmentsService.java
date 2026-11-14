package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.dto.portal.request.shipment.ShipmentFailDeliveryRequest;
import com.wedbackendservice.nextgen.dto.portal.request.shipment.ShipmentStatusUpdateRequest;
import com.wedbackendservice.nextgen.dto.common.PageResponse;
import com.wedbackendservice.nextgen.dto.portal.response.shipment.*;
import com.wedbackendservice.nextgen.dto.client.response.shipment.*;
import com.wedbackendservice.nextgen.entity.*;
import com.wedbackendservice.nextgen.enums.ShipmentStatus;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.mapper.ShipmentMapper;
import com.wedbackendservice.nextgen.repository.*;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import com.wedbackendservice.nextgen.util.PaginationUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentsService {

    private final SecurityHelper securityHelper;
    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final NotificationRepository notificationRepository;
    private final ShipmentMapper shipmentMapper;

    /**
     * API 9.1: Khách hàng tra cứu hành trình vận chuyển (Public)
     * Bảo vệ PII: Che tên người nhận, che số điện thoại (chỉ để lại 3 số cuối), che địa chỉ chi tiết.
     * Làm sạch timeline: không để lộ mã kho nội bộ MB02/MB03 và ghi chú nội bộ.
     */
    public ShipmentTrackResponse trackShipment(String trackingCode) {
        if (trackingCode == null || trackingCode.trim().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã tra cứu không được để trống");
        }

        Shipment shipment = shipmentRepository.findByTrackingCodeOrOrderCode(trackingCode.trim())
                .orElseThrow(() -> new AppException(ErrorCode.SHIPMENT_NOT_FOUND));

        Order order = shipment.getOrder();

        List<ShipmentTrackItem> items = new ArrayList<>();
        if (order != null) {
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getOrderId());
            for (OrderItem oi : orderItems) {
                ProductVariant variant = oi.getVariant();
                String productName = variant != null && variant.getProduct() != null
                        ? variant.getProduct().getProductName()
                        : "Sản phẩm";
                String color = variant != null ? variant.getColor() : null;

                items.add(ShipmentTrackItem.builder()
                        .productName(productName)
                        .color(color)
                        .quantity(oi.getQuantity())
                        .build());
            }
        }

        List<ShipmentTimelineItem> timeline = buildTimeline(shipment, order);
        timeline.sort(Comparator.comparing(ShipmentTimelineItem::getTime));

        return ShipmentTrackResponse.builder()
                .shipmentId(shipment.getShipmentId())
                .trackingCode(shipment.getTrackingNumber())
                .orderCode(order != null ? order.getOrderCode() : null)
                .carrier(shipment.getCarrierName())
                .status(shipment.getShippingStatus())
                .statusLabel(getStatusLabel(shipment.getShippingStatus()))
                .recipientName(order != null ? maskRecipientName(order.getRecipientName()) : null)
                .recipientPhone(order != null ? maskPhone(order.getRecipientPhone()) : null)
                .shippingAddress(order != null ? maskShippingAddress(order.getShippingAddress()) : null)
                .estimatedDeliveryDate(shipment.getEstimatedDelivery())
                .items(items)
                .timeline(timeline)
                .build();
    }

    /**
     * API 9.2: Quản trị danh sách vận đơn (Admin Portal) (STAFF / ADMIN)
     */
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public PageResponse<AdminShipmentItemResponse> getAdminShipments(int page, int size, String carrier, String status, String search) {
        securityHelper.authenticateStaffOrAdmin();

        log.debug("Get admin shipments: page={}, size={}, carrier={}, status={}, search={}", page, size, carrier, status, search);

        Pageable pageable = PaginationUtil.createPageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Shipment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (carrier != null && !carrier.isBlank() && !"ALL".equalsIgnoreCase(carrier.trim())) {
                predicates.add(cb.like(cb.upper(root.get("carrierName")), "%" + carrier.trim().toUpperCase() + "%"));
            }

            if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
                predicates.add(cb.equal(root.get("shippingStatus"), status.trim()));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                var orderJoin = root.join("order", jakarta.persistence.criteria.JoinType.LEFT);

                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("trackingNumber")), pattern),
                        cb.like(cb.lower(orderJoin.get("orderCode")), pattern),
                        cb.like(cb.lower(orderJoin.get("recipientName")), pattern),
                        cb.like(orderJoin.get("recipientPhone"), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Shipment> shipmentPage = shipmentRepository.findAll(spec, pageable);

        if (shipmentPage.isEmpty()) {
            return PageResponse.from(shipmentPage, s -> null);
        }

        List<Long> orderIds = shipmentPage.getContent().stream()
                .map(Shipment::getOrder)
                .filter(Objects::nonNull)
                .map(Order::getOrderId)
                .distinct()
                .toList();

        Map<Long, String> paymentMethodMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            List<Payment> payments = paymentRepository.findByOrderIdsOrderByCreatedAtDesc(orderIds);
            for (Payment p : payments) {
                if (p.getOrder() != null && p.getOrder().getOrderId() != null) {
                    paymentMethodMap.putIfAbsent(p.getOrder().getOrderId(), p.getPaymentMethod());
                }
            }
        }

        return PageResponse.from(shipmentPage, s -> shipmentMapper.toAdminShipmentItem(s,
                s.getOrder() != null ? paymentMethodMap.getOrDefault(s.getOrder().getOrderId(), "COD") : "COD"));
    }

    /**
     * API 9.3: Cập nhật trạng thái vận đơn & Tự động đồng bộ 2 chiều (STAFF / ADMIN)
     * Kiểm tra State Machine nghiêm ngặt: không nhảy lùi trạng thái, không ghi đè khi gọi DELIVERED lặp lại.
     */
    @Transactional
    public ShipmentStatusSyncResponse updateShipmentStatus(Long id, ShipmentStatusUpdateRequest request) {
        User operator = securityHelper.authenticateStaffOrAdmin();

        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHIPMENT_NOT_FOUND));

        String currentStatusStr = shipment.getShippingStatus();
        String newStatusStr = request.getStatus().trim();

        ShipmentStatus targetStatus;
        try {
            targetStatus = ShipmentStatus.valueOf(newStatusStr.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Trạng thái vận chuyển không hợp lệ: " + newStatusStr);
        }

        ShipmentStatus currentStatus = null;
        if (currentStatusStr != null && !currentStatusStr.isBlank()) {
            try {
                currentStatus = ShipmentStatus.valueOf(currentStatusStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (currentStatus != null) {
            if (currentStatus == targetStatus) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Vận đơn đã ở trạng thái " + newStatusStr + " trước đó.");
            }
            if (!currentStatus.canTransitionTo(targetStatus)) {
                log.warn("Không thể chuyển đổi trạng thái từ {} sang {}", currentStatus, targetStatus);
                throw new AppException(ErrorCode.INVALID_SHIPMENT_STATUS_TRANSITION);
            }
        }

        shipment.setShippingStatus(targetStatus.name());
        shipment.setUpdatedBy(operator);

        if (targetStatus == ShipmentStatus.PICKED || targetStatus == ShipmentStatus.IN_TRANSIT) {
            if (shipment.getShippedAt() == null) {
                shipment.setShippedAt(LocalDateTime.now());
            }
        } else if (targetStatus == ShipmentStatus.DELIVERED) {
            if (shipment.getDeliveredAt() == null) {
                shipment.setDeliveredAt(LocalDateTime.now());
            }
        }

        shipmentRepository.save(shipment);

        Order order = shipment.getOrder();
        String synchronizedOrderStatus = null;
        boolean isPaymentCompleted = false;

        if (order != null) {
            String fromStatus = order.getStatus();

            if (targetStatus == ShipmentStatus.PICKED || targetStatus == ShipmentStatus.IN_TRANSIT) {
                synchronizedOrderStatus = "SHIPPING";
                order.setStatus("SHIPPING");
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
            } else if (targetStatus == ShipmentStatus.DELIVERED) {
                synchronizedOrderStatus = "COMPLETED";
                order.setStatus("COMPLETED");
                if (order.getCompletedAt() == null) {
                    order.setCompletedAt(LocalDateTime.now());
                }
                order.setUpdatedAt(LocalDateTime.now());

                Optional<Payment> paymentOpt = paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(order.getOrderId());
                if (paymentOpt.isPresent()) {
                    Payment payment = paymentOpt.get();
                    payment.setStatus("PAID");
                    payment.setPaidAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                }
                order.setPaymentStatus("PAID");
                isPaymentCompleted = true;
                orderRepository.save(order);
            }

            if (synchronizedOrderStatus != null && !synchronizedOrderStatus.equals(fromStatus)) {
                orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                        .order(order)
                        .changedBy(operator)
                        .previousStatus(fromStatus)
                        .newStatus(synchronizedOrderStatus)
                        .note("Đồng bộ trạng thái từ Vận chuyển: " + getStatusLabel(targetStatus.name()) +
                                (request.getNote() != null ? " (" + request.getNote() + ")" : ""))
                        .build());
            }
        }

        return ShipmentStatusSyncResponse.builder()
                .shipmentId(shipment.getShipmentId())
                .trackingCode(shipment.getTrackingNumber())
                .shipmentStatus(shipment.getShippingStatus())
                .synchronizedOrderId(order != null ? order.getOrderId() : null)
                .synchronizedOrderStatus(synchronizedOrderStatus)
                .isPaymentCompleted(isPaymentCompleted)
                .build();
    }

    /**
     * API 9.4: Xử lý giao hàng thất bại (Admin)
     * Ngăn chặn bug double restock: kiểm tra xem đơn đã bị hủy/giao thất bại trước đó chưa.
     * Cập nhật kho bằng hàm UPDATE atomic.
     * Xử lý hoàn tiền cho đơn thanh toán online (REFUND_PENDING).
     */
    @Transactional
    public ShipmentFailDeliveryResponse failDelivery(Long id, ShipmentFailDeliveryRequest request) {
        User operator = securityHelper.authenticateStaffOrAdmin();

        if (request.getFailReason() == null || request.getFailReason().trim().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Thiếu failReason");
        }

        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHIPMENT_NOT_FOUND));

        if ("DELIVERED".equalsIgnoreCase(shipment.getShippingStatus())) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Vận đơn đã ở trạng thái DELIVERED, không thể chuyển sang giao thất bại.");
        }

        // Chặn lỗi hoàn kho 2 lần (Double Restock)
        if ("FAILED_DELIVERY".equalsIgnoreCase(shipment.getShippingStatus())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Vận đơn này đã được xử lý giao hàng thất bại trước đó.");
        }

        Order order = shipment.getOrder();
        if (order != null && "CANCELLED".equalsIgnoreCase(order.getStatus())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Đơn hàng liên kết đã bị hủy trước đó.");
        }

        String reason = request.getFailReason().trim();

        shipment.setShippingStatus("FAILED_DELIVERY");
        shipment.setFailReason(reason);
        shipment.setUpdatedBy(operator);
        shipmentRepository.save(shipment);

        int restockedCount = 0;

        if (order != null) {
            String fromStatus = order.getStatus();
            order.setStatus("CANCELLED");
            order.setCancelReason("Giao hàng thất bại: " + reason);

            // Kiểm tra xử lý hoàn tiền nếu đơn hàng đã thanh toán online
            boolean needRefund = "PAID".equalsIgnoreCase(order.getPaymentStatus());
            if (needRefund) {
                order.setPaymentStatus("REFUND_PENDING");
            }
            orderRepository.save(order);

            orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                    .order(order)
                    .changedBy(operator)
                    .previousStatus(fromStatus)
                    .newStatus("CANCELLED")
                    .note("Giao hàng thất bại: " + reason + (needRefund ? " (Đã đánh dấu chờ hoàn tiền)" : ""))
                    .build());

            List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getOrderId());
            for (OrderItem item : orderItems) {
                ProductVariant variant = item.getVariant();
                if (variant != null) {
                    // Cập nhật kho bằng câu atomic update tránh race conditions
                    int updatedRows = inventoryRepository.increaseQuantityAtomic(
                            variant.getVariantId(), item.getQuantity(), LocalDateTime.now());

                    if (updatedRows > 0) {
                        restockedCount++;
                    }

                    inventoryTransactionRepository.save(InventoryTransaction.builder()
                            .variant(variant)
                            .order(order)
                            .performedBy(operator)
                            .transactionType("IMPORT_RETURN")
                            .formCode("MB04")
                            .quantity(item.getQuantity())
                            .reason("Nhập hoàn kho do bưu tá giao hàng thất bại")
                            .build());
                }
            }

            if (order.getUser() != null) {
                notificationRepository.save(Notification.builder()
                        .user(order.getUser())
                        .order(order)
                        .title("Đơn hàng đã bị hủy")
                        .content("Đơn hàng " + order.getOrderCode() + " đã bị hủy do giao hàng không thành công: " + reason)
                        .isRead(false)
                        .build());
            }
        }

        return ShipmentFailDeliveryResponse.builder()
                .shipmentId(id)
                .trackingCode(shipment.getTrackingNumber())
                .shipmentStatus("FAILED_DELIVERY")
                .failReason(reason)
                .orderStatus("CANCELLED")
                .inventoryRestocked(restockedCount > 0)
                .restockedItemsCount(restockedCount)
                .build();
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private List<ShipmentTimelineItem> buildTimeline(Shipment shipment, Order order) {
        List<ShipmentTimelineItem> list = new ArrayList<>();

        if (order != null && order.getCreatedAt() != null) {
            list.add(ShipmentTimelineItem.builder()
                    .time(order.getCreatedAt())
                    .status("ORDER_PLACED")
                    .description("Đơn hàng đã được tạo thành công.")
                    .build());
        }

        if (shipment.getCreatedAt() != null) {
            list.add(ShipmentTimelineItem.builder()
                    .time(shipment.getCreatedAt())
                    .status("READY_TO_PICK")
                    .description("Đơn hàng đã được xác nhận. Đang chuẩn bị đóng gói và chờ đối tác lấy hàng.")
                    .build());
        }

        if (shipment.getShippedAt() != null) {
            list.add(ShipmentTimelineItem.builder()
                    .time(shipment.getShippedAt())
                    .status("PICKED")
                    .description("Kiện hàng đã xuất kho. Đơn vị vận chuyển " +
                            (shipment.getCarrierName() != null ? shipment.getCarrierName() : "") + " đã nhận kiện hàng.")
                    .build());
        }

        if ("IN_TRANSIT".equalsIgnoreCase(shipment.getShippingStatus()) || "DELIVERED".equalsIgnoreCase(shipment.getShippingStatus())) {
            LocalDateTime transitTime = shipment.getShippedAt() != null ? shipment.getShippedAt() : shipment.getCreatedAt();
            list.add(ShipmentTimelineItem.builder()
                    .time(transitTime)
                    .status("IN_TRANSIT")
                    .description("Kiện hàng đang trong quá trình luân chuyển tới địa chỉ nhận.")
                    .build());
        }

        if (shipment.getDeliveredAt() != null) {
            list.add(ShipmentTimelineItem.builder()
                    .time(shipment.getDeliveredAt())
                    .status("DELIVERED")
                    .description("Kiện hàng đã được giao thành công tới người nhận.")
                    .build());
        }

        if (shipment.getFailReason() != null) {
            list.add(ShipmentTimelineItem.builder()
                    .time(shipment.getUpdatedAt())
                    .status("FAILED_DELIVERY")
                    .description("Giao hàng không thành công: " + shipment.getFailReason())
                    .build());
        }

        return list;
    }

    public static String maskRecipientName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "***";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].charAt(0) + "***";
        }
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            sb.append(" ").append(parts[i].charAt(0)).append("***");
        }
        return sb.toString();
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        // Giữ lại 3 số đầu và 2 số cuối, che 5 số ở giữa (e.g. 098*****21)
        int len = phone.length();
        return phone.substring(0, 3) + "*****" + phone.substring(len - 2);
    }

    public static String maskShippingAddress(String address) {
        if (address == null || address.isBlank()) {
            return "***";
        }
        int commaIndex = address.indexOf(",");
        if (commaIndex > 0 && commaIndex < address.length() - 1) {
            return "***," + address.substring(commaIndex + 1);
        }
        return "*** " + (address.length() > 10 ? address.substring(address.length() - 10) : "");
    }

    public static String getStatusLabel(String status) {
        if (status == null) return "Không xác định";
        return switch (status.toUpperCase()) {
            case "READY_TO_PICK" -> "Chờ lấy hàng";
            case "PICKED" -> "Đã lấy hàng";
            case "IN_TRANSIT" -> "Đang vận chuyển";
            case "DELIVERED" -> "Giao hàng thành công";
            case "FAILED_DELIVERY" -> "Giao hàng thất bại";
            case "CANCELLED" -> "Đã hủy";
            default -> status;
        };
    }
}
