package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.dto.common.PageResponse;
import com.wedbackendservice.nextgen.dto.client.request.order.CancelOrderRequest;
import com.wedbackendservice.nextgen.dto.client.request.order.CreateOrderRequest;
import com.wedbackendservice.nextgen.dto.client.response.order.CancelOrderResponse;
import com.wedbackendservice.nextgen.dto.client.response.order.CreateOrderResponse;
import com.wedbackendservice.nextgen.dto.client.response.order.MyOrderItemResponse;
import com.wedbackendservice.nextgen.dto.client.response.order.OrderDetailResponse;
import com.wedbackendservice.nextgen.dto.portal.request.order.ConfirmOrderRequest;
import com.wedbackendservice.nextgen.dto.portal.request.order.PackExportOrderRequest;
import com.wedbackendservice.nextgen.dto.portal.response.order.AdminOrderListResponse;
import com.wedbackendservice.nextgen.dto.portal.response.order.ConfirmOrderResponse;
import com.wedbackendservice.nextgen.dto.portal.response.order.PackExportOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdersService {

    private final OrderCreationService orderCreationService;
    private final OrdersQueryService ordersQueryService;
    private final OrderFulfillmentService orderFulfillmentService;
    private final OrderCancellationService orderCancellationService;
    private final OrderReturnService orderReturnService;

    /**
     * API 8.1: [POST] /api/v1/orders — Đặt hàng Checkout (Khởi tạo MB01 & Giữ chỗ tồn kho đồng bộ)
     */
    public CreateOrderResponse createOrder(CreateOrderRequest request, String idempotencyKey) {
        return orderCreationService.createOrder(request, idempotencyKey);
    }

    /**
     * API 8.2: [GET] /api/v1/orders/my-orders — Khách hàng tra cứu lịch sử mua hàng
     */
    public PageResponse<MyOrderItemResponse> getMyOrders(int page, int size, String status) {
        return ordersQueryService.getMyOrders(page, size, status);
    }

    /**
     * API 8.3: [GET] /api/v1/orders/{orderCode} — Chi tiết đơn hàng theo mã #DH-XXXX
     */
    public OrderDetailResponse getOrderDetail(String orderCode) {
        return ordersQueryService.getOrderDetail(orderCode);
    }

    /**
     * API 8.4: [POST] /api/v1/orders/{orderCode}/cancel — Khách hàng hủy đơn & Giải phóng kho
     */
    public CancelOrderResponse cancelOrder(String orderCode, CancelOrderRequest request) {
        return orderCancellationService.cancelOrder(orderCode, request);
    }

    /**
     * API 8.5: [GET] /api/v1/admin/orders — Quản trị toàn bộ danh sách đơn hàng (STAFF / ADMIN)
     */
    public AdminOrderListResponse getAdminOrders(int page, int size, String status, String paymentStatus) {
        return ordersQueryService.getAdminOrders(page, size, status, paymentStatus);
    }

    /**
     * API 8.6: [POST] /api/v1/admin/orders/{id}/confirm — Xác nhận đơn MB02 & Tự động tạo Vận đơn
     */
    public ConfirmOrderResponse confirmOrder(Long id, ConfirmOrderRequest request) {
        return orderFulfillmentService.confirmOrder(id, request);
    }

    /**
     * Chuyển trạng thái đơn hàng sang Đang chuẩn bị hàng (PREPARING) & Đẩy sang Vận đơn
     */
    public OrderDetailResponse prepareOrder(Long id) {
        return orderFulfillmentService.prepareOrder(id);
    }

    /**
     * API 8.7: [POST] /api/v1/admin/orders/{id}/pack-export — Đóng gói & Xuất kho vật lý MB03
     */
    public PackExportOrderResponse packAndExportOrder(Long id, PackExportOrderRequest request) {
        return orderFulfillmentService.packAndExportOrder(id, request);
    }

    /**
     * API Xác nhận đã nhận tiền chuyển khoản đơn hàng (STAFF / ADMIN)
     */
    public OrderDetailResponse confirmOrderPayment(Long orderId, String note) {
        return orderFulfillmentService.confirmOrderPayment(orderId, note);
    }

    /**
     * API Khách hàng thông báo đã chuyển khoản thành công
     */
    public OrderDetailResponse customerNotifyPayment(Long orderId, String note) {
        return orderFulfillmentService.customerNotifyPayment(orderId, note);
    }

    /**
     * API 8.8: Tiếp nhận hàng hoàn từ đơn vị vận chuyển về kho (MB04)
     */
    public OrderDetailResponse receiveReturnOrder(Long orderId, String note) {
        return orderReturnService.receiveReturnOrder(orderId, note);
    }

    /**
     * Tự động quét và hủy các đơn hàng chờ thanh toán QR đã hết hạn
     */
    public int cancelExpiredOrders() {
        return orderCancellationService.cancelExpiredOrders();
    }
}
