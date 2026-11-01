package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.security.SecurityHelper;

import com.wedbackendservice.nextgen.dto.response.admin.PaginationResponse;
import com.wedbackendservice.nextgen.dto.response.shipment.*;
import com.wedbackendservice.nextgen.dto.request.shipment.ShipmentFailDeliveryRequest;
import com.wedbackendservice.nextgen.dto.request.shipment.ShipmentStatusUpdateRequest;
import com.wedbackendservice.nextgen.entity.*;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    /**
     * API 9.1: Khách hàng tra cứu hành trình vận chuyển (Public)
     */
    public ShipmentTrackResponse trackShipment(String trackingCode) {
        if (trackingCode == null || trackingCode.trim().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã tra cứu không được để trống");
        }

        Shipment shipment = shipmentRepository.findByTrackingCodeOrOrderCode(trackingCode.trim())
                .orElseThrow(() -> new AppException(ErrorCode.SHIPMENT_NOT_FOUND));

        Order order = shipment.getOrder();

        List<ShipmentTrackResponse.ShipmentTrackItem> items = new ArrayList<>();
        if (order != null) {
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getOrderId());
            for (OrderItem oi : orderItems) {
                ProductVariant variant = oi.getVariant();
                String productName = variant != null && variant.getProduct() != null
                        ? variant.getProduct().getProductName()
                        : "Sản phẩm";
                String color = variant != null ? variant.getColor() : null;

                items.add(ShipmentTrackResponse.ShipmentTrackItem.builder()
                        .productName(productName)
                        .color(color)
                        .quantity(oi.getQuantity())
                        .build());
            }
        }

        List<ShipmentTrackResponse.ShipmentTimelineItem> timeline = buildTimeline(shipment, order);

        return ShipmentTrackResponse.builder()
                .shipmentId(shipment.getShipmentId())
                .trackingCode(shipment.getTrackingNumber())
                .orderCode(order != null ? order.getOrderCode() : null)
                .carrier(shipment.getCarrierName())
                .status(shipment.getShippingStatus())
                .statusLabel(getStatusLabel(shipment.getShippingStatus()))
                .recipientName(order != null ? order.getRecipientName() : null)
                .recipientPhone(maskPhone(order != null ? order.getRecipientPhone() : null))
                .shippingAddress(order != null ? order.getShippingAddress() : null)
                .estimatedDeliveryDate(shipment.getEstimatedDelivery())
                .items(items)
                .timeline(timeline)
                .build();
    }

    /**
     * API 9.2: Quản trị danh sách vận đơn (Admin Portal) (STAFF / ADMIN)
     */
    public AdminShipmentListResponse getAdminShipments(
            String authHeader, int page, int size, String carrier, String status, String search) {
        securityHelper.authenticateStaffOrAdmin(authHeader);

        int pageIndex = Math.max(0, page - 1);
        int pageSize = size > 0 ? size : 10;

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

        Page<Shipment> shipmentPage = shipmentRepository.findAll(
                spec, PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<AdminShipmentItemResponse> items = shipmentPage.getContent().stream()
                .map(this::mapToAdminShipmentItem)
                .toList();

        PaginationResponse pagination = PaginationResponse.builder()
                .page(page)
                .size(pageSize)
                .totalItems((int) shipmentPage.getTotalElements())
                .totalPages(shipmentPage.getTotalPages())
                .build();

        return AdminShipmentListResponse.builder()
                .items(items)
                .pagination(pagination)
                .build();
    }

    /**
     * API 9.3: Cập nhật trạng thái vận đơn & Tự động đồng bộ 2 chiều (STAFF / ADMIN)
     */
    @Transactional
    public ShipmentStatusSyncResponse updateShipmentStatus(
            String authHeader, Long id, ShipmentStatusUpdateRequest request) {
        User operator = securityHelper.authenticateStaffOrAdmin(authHeader);

        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHIPMENT_NOT_FOUND));

        String newStatus = request.getStatus().trim();
        shipment.setShippingStatus(newStatus);
        shipment.setUpdatedBy(operator);
        shipment.setUpdatedAt(LocalDateTime.now());

        if ("PICKED".equalsIgnoreCase(newStatus) || "IN_TRANSIT".equalsIgnoreCase(newStatus)) {
            if (shipment.getShippedAt() == null) {
                shipment.setShippedAt(LocalDateTime.now());
            }
        } else if ("DELIVERED".equalsIgnoreCase(newStatus)) {
            shipment.setDeliveredAt(LocalDateTime.now());
        }

        shipmentRepository.save(shipment);

        Order order = shipment.getOrder();
        String synchronizedOrderStatus = null;
        boolean isPaymentCompleted = false;

        if (order != null) {
            String fromStatus = order.getStatus();

            if ("PICKED".equalsIgnoreCase(newStatus) || "IN_TRANSIT".equalsIgnoreCase(newStatus)) {
                synchronizedOrderStatus = "SHIPPING";
                order.setStatus("SHIPPING");
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
            } else if ("DELIVERED".equalsIgnoreCase(newStatus)) {
                synchronizedOrderStatus = "COMPLETED";
                order.setStatus("COMPLETED");
                order.setCompletedAt(LocalDateTime.now());
                order.setUpdatedAt(LocalDateTime.now());

                Optional<Payment> paymentOpt = paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(order.getOrderId());
                if (paymentOpt.isPresent()) {
                    Payment payment = paymentOpt.get();
                    payment.setStatus("PAID");
                    payment.setPaidAt(LocalDateTime.now());
                    payment.setUpdatedAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                }
                order.setPaymentStatus("PAID");
                orderRepository.save(order);
                isPaymentCompleted = true;
            }

            orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                    .order(order)
                    .changedBy(operator)
                    .previousStatus(fromStatus)
                    .newStatus(synchronizedOrderStatus != null ? synchronizedOrderStatus : fromStatus)
                    .note(request.getNote() != null && !request.getNote().isBlank()
                            ? request.getNote()
                            : "Đồng bộ tự động từ phân hệ Quản lý Vận đơn")
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        return ShipmentStatusSyncResponse.builder()
                .shipmentId(shipment.getShipmentId())
                .trackingCode(shipment.getTrackingNumber())
                .shipmentStatus(shipment.getShippingStatus())
                .synchronizedOrderId(order != null ? order.getOrderId() : null)
                .synchronizedOrderStatus(synchronizedOrderStatus)
                .isPaymentCompleted(isPaymentCompleted)
                .updatedAt(shipment.getUpdatedAt())
                .build();
    }

    /**
     * API 9.4: Xử lý giao hàng thất bại, tự động hủy đơn và hoàn kho (STAFF / ADMIN)
     */
    @Transactional
    public ShipmentFailDeliveryResponse failDelivery(
            String authHeader, Long id, ShipmentFailDeliveryRequest request) {
        User operator = securityHelper.authenticateStaffOrAdmin(authHeader);

        if (request.getFailReason() == null || request.getFailReason().trim().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Thiếu failReason");
        }

        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHIPMENT_NOT_FOUND));

        if ("DELIVERED".equalsIgnoreCase(shipment.getShippingStatus())) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Vận đơn đã ở trạng thái DELIVERED, không thể chuyển sang giao thất bại.");
        }

        String reason = request.getFailReason().trim();

        shipment.setShippingStatus("FAILED_DELIVERY");
        shipment.setFailReason(reason);
        shipment.setUpdatedBy(operator);
        shipment.setUpdatedAt(LocalDateTime.now());
        shipmentRepository.save(shipment);

        Order order = shipment.getOrder();
        int restockedCount = 0;

        if (order != null) {
            String fromStatus = order.getStatus();
            order.setStatus("CANCELLED");
            order.setCancelReason("Giao hàng thất bại: " + reason);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                    .order(order)
                    .changedBy(operator)
                    .previousStatus(fromStatus)
                    .newStatus("CANCELLED")
                    .note("Giao hàng thất bại: " + reason)
                    .createdAt(LocalDateTime.now())
                    .build());

            List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getOrderId());
            for (OrderItem item : orderItems) {
                ProductVariant variant = item.getVariant();
                if (variant != null) {
                    Optional<Inventory> invOpt = inventoryRepository.findByVariant_VariantId(variant.getVariantId());
                    if (invOpt.isPresent()) {
                        Inventory inv = invOpt.get();
                        inv.setQuantity(inv.getQuantity() + item.getQuantity());
                        inv.setUpdatedAt(LocalDateTime.now());
                        inventoryRepository.save(inv);
                    }

                    inventoryTransactionRepository.save(InventoryTransaction.builder()
                            .variant(variant)
                            .order(order)
                            .performedBy(operator)
                            .transactionType("IMPORT_RETURN")
                            .formCode("MB04")
                            .quantity(item.getQuantity())
                            .reason("Nhập hoàn kho do bưu tá giao hàng thất bại")
                            .createdAt(LocalDateTime.now())
                            .build());

                    restockedCount++;
                }
            }

            if (order.getUser() != null) {
                notificationRepository.save(Notification.builder()
                        .user(order.getUser())
                        .order(order)
                        .title("Đơn hàng đã bị hủy")
                        .content("Đơn hàng " + order.getOrderCode() + " đã bị hủy do giao hàng không thành công: " + reason)
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build());
            }
        }

        return ShipmentFailDeliveryResponse.builder()
                .shipmentId(shipment.getShipmentId())
                .trackingCode(shipment.getTrackingNumber())
                .shipmentStatus(shipment.getShippingStatus())
                .failReason(reason)
                .orderStatus("CANCELLED")
                .inventoryRestocked(true)
                .restockedItemsCount(restockedCount)
                .build();
    }

    // =========================================================================
    // HELPERS & MAPPERS
    // =========================================================================

    private AdminShipmentItemResponse mapToAdminShipmentItem(Shipment shipment) {
        Order order = shipment.getOrder();
        String paymentMethod = null;
        if (order != null) {
            Optional<Payment> payment = paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(order.getOrderId());
            paymentMethod = payment.map(Payment::getPaymentMethod).orElse("COD");
        }

        String updatedByName = shipment.getUpdatedBy() != null
                ? shipment.getUpdatedBy().getFullName() + " (" + shipment.getUpdatedBy().getRole() + ")"
                : null;

        return AdminShipmentItemResponse.builder()
                .shipmentId(shipment.getShipmentId())
                .orderId(order != null ? order.getOrderId() : null)
                .orderCode(order != null ? order.getOrderCode() : null)
                .carrier(shipment.getCarrierName())
                .trackingCode(shipment.getTrackingNumber())
                .status(shipment.getShippingStatus())
                .failReason(shipment.getFailReason())
                .recipientName(order != null ? order.getRecipientName() : null)
                .recipientPhone(order != null ? order.getRecipientPhone() : null)
                .shippingAddress(order != null ? order.getShippingAddress() : null)
                .orderTotalAmount(order != null ? order.getFinalAmount() : null)
                .paymentMethod(paymentMethod)
                .updatedBy(updatedByName)
                .updatedAt(shipment.getUpdatedAt())
                .build();
    }

    private List<ShipmentTrackResponse.ShipmentTimelineItem> buildTimeline(Shipment shipment, Order order) {
        List<ShipmentTrackResponse.ShipmentTimelineItem> list = new ArrayList<>();

        if (order != null) {
            List<OrderStatusHistory> history = orderStatusHistoryRepository.findByOrder_OrderIdOrderByCreatedAtDesc(order.getOrderId());
            if (!history.isEmpty()) {
                for (OrderStatusHistory h : history) {
                    list.add(ShipmentTrackResponse.ShipmentTimelineItem.builder()
                            .time(h.getCreatedAt())
                            .status(h.getNewStatus())
                            .description(h.getNote() != null ? h.getNote() : ("Chuyển trạng thái sang " + h.getNewStatus()))
                            .build());
                }
                return list;
            }
        }

        if (shipment.getCreatedAt() != null) {
            list.add(ShipmentTrackResponse.ShipmentTimelineItem.builder()
                    .time(shipment.getCreatedAt())
                    .status("READY_TO_PICK")
                    .description("Nhân viên vận hành đã xác nhận đơn hàng (MB02). Đang chờ đối tác vận chuyển lấy hàng.")
                    .build());
        }
        if (shipment.getShippedAt() != null) {
            list.add(ShipmentTrackResponse.ShipmentTimelineItem.builder()
                    .time(shipment.getShippedAt())
                    .status("PICKED")
                    .description("Bộ phận kho đã xuất kho vật lý (MB03). Tài xế " + (shipment.getCarrierName() != null ? shipment.getCarrierName() : "đối tác") + " đã lấy kiện hàng.")
                    .build());
            list.add(ShipmentTrackResponse.ShipmentTimelineItem.builder()
                    .time(shipment.getShippedAt().plusHours(4))
                    .status("IN_TRANSIT")
                    .description("Kiện hàng đang được luân chuyển qua Bưu cục trung chuyển.")
                    .build());
        }
        if (shipment.getDeliveredAt() != null) {
            list.add(ShipmentTrackResponse.ShipmentTimelineItem.builder()
                    .time(shipment.getDeliveredAt())
                    .status("DELIVERED")
                    .description("Kiện hàng đã được giao thành công tới người nhận.")
                    .build());
        }
        if (shipment.getFailReason() != null) {
            list.add(ShipmentTrackResponse.ShipmentTimelineItem.builder()
                    .time(shipment.getUpdatedAt())
                    .status("FAILED_DELIVERY")
                    .description("Giao hàng không thành công: " + shipment.getFailReason())
                    .build());
        }

        return list;
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "***" + phone.substring(phone.length() - 4);
    }

    public static String getStatusLabel(String status) {
        if (status == null) return "Không xác định";
        return switch (status.toUpperCase()) {
            case "READY_TO_PICK" -> "Chờ lấy hàng";
            case "PICKED" -> "Đã lấy hàng";
            case "IN_TRANSIT" -> "Đang vận chuyển";
            case "DELIVERED" -> "Giao hàng thành công";
            case "FAILED_DELIVERY" -> "Giao hàng thất bại";
            default -> status;
        };
    }
}
