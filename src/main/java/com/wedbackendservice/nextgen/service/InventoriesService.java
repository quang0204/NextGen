package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.util.DateTimeUtil;

import com.wedbackendservice.nextgen.dto.request.inventory.InventoryAdjustmentRequest;
import com.wedbackendservice.nextgen.dto.response.PageResponse;
import com.wedbackendservice.nextgen.dto.response.inventory.*;
import com.wedbackendservice.nextgen.entity.*;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.repository.InventoryRepository;
import com.wedbackendservice.nextgen.repository.InventoryTransactionRepository;
import com.wedbackendservice.nextgen.repository.ProductVariantRepository;
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
import org.springframework.util.StringUtils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoriesService {

    private final SecurityHelper securityHelper;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ProductVariantRepository productVariantRepository;
    private final AuditLogRepository auditLogRepository;

    private String determineStatus(int availableQty, int minThreshold) {
        if (availableQty <= 0) {
            return "OUT_OF_STOCK";
        }
        if (availableQty <= minThreshold) {
            return "LOW_STOCK";
        }
        return "IN_STOCK";
    }

    /**
     * API 5.1: [GET] /api/v1/admin/inventories — Bảng theo dõi tồn kho biến thể SKU (STAFF / ADMIN)
     */
    @Transactional(readOnly = true)
    public AdminInventoryListResponse getAdminInventories(int page, int size, String keyword, String status, Boolean lowStockOnly) {
        securityHelper.authenticateStaffOrAdmin();

        int safePage = Math.max(page, 1) - 1;
        int safeSize = (size <= 0 || size > 100) ? 10 : size;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "updatedAt").and(Sort.by(Sort.Direction.DESC, "inventoryId")));

        Specification<Inventory> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Inventory, ProductVariant> variantJoin = root.join("variant", JoinType.LEFT);
            Join<ProductVariant, Product> productJoin = variantJoin.join("product", JoinType.LEFT);

            if (StringUtils.hasText(keyword)) {
                String kw = "%" + keyword.trim().toLowerCase() + "%";
                Predicate skuMatch = cb.like(cb.lower(variantJoin.get("sku")), kw);
                Predicate nameMatch = cb.like(cb.lower(productJoin.get("productName")), kw);
                predicates.add(cb.or(skuMatch, nameMatch));
            }

            var availDiff = cb.diff(
                    cb.coalesce(root.get("quantity"), 0),
                    cb.coalesce(root.get("reservedQuantity"), 0)
            );
            var thresholdVal = cb.coalesce(root.get("minThreshold"), 5);

            if (Boolean.TRUE.equals(lowStockOnly)) {
                predicates.add(cb.lessThanOrEqualTo(availDiff, thresholdVal));
            }

            if (StringUtils.hasText(status) && !status.equalsIgnoreCase("ALL")) {
                String st = status.trim().toUpperCase();
                switch (st) {
                    case "IN_STOCK" -> predicates.add(cb.greaterThan(availDiff, thresholdVal));
                    case "LOW_STOCK" -> {
                        predicates.add(cb.lessThanOrEqualTo(availDiff, thresholdVal));
                        predicates.add(cb.greaterThan(availDiff, 0));
                    }
                    case "OUT_OF_STOCK" -> predicates.add(cb.lessThanOrEqualTo(availDiff, 0));
                    default -> {}
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Inventory> pageResult = inventoryRepository.findAll(spec, pageable);

        List<InventoryItemResponse> items = pageResult.getContent().stream().map(inv -> {
            ProductVariant v = inv.getVariant();
            Product p = v != null ? v.getProduct() : null;

            int onHand = inv.getOnHandQty();
            int reserved = inv.getReservedQty();
            int available = inv.getAvailableQty();
            int threshold = inv.getMinThreshold();
            String itemStatus = determineStatus(available, threshold);

            return InventoryItemResponse.builder()
                    .inventoryId(inv.getInventoryId())
                    .variantId(v != null ? v.getVariantId() : null)
                    .productId(p != null ? p.getProductId() : null)
                    .productName(p != null ? p.getProductName() : null)
                    .sku(v != null ? v.getSku() : null)
                    .color(v != null ? v.getColor() : null)
                    .romGb(v != null ? v.getRomGb() : null)
                    .onHandQty(onHand)
                    .reservedQty(reserved)
                    .availableQty(available)
                    .minThreshold(threshold)
                    .status(itemStatus)
                    .updatedAt(DateTimeUtil.formatIsoInstant(inv.getUpdatedAt()))
                    .build();
        }).toList();

        // Summary tính toán trên toàn bộ kho
        List<Object[]> statsList = inventoryRepository.getInventorySummaryStats();
        int totalOnHand = 0;
        int totalReserved = 0;
        int totalAvailable = 0;
        int lowStockCount = 0;

        if (statsList != null && !statsList.isEmpty() && statsList.get(0) != null) {
            Object[] row = statsList.get(0);
            totalOnHand = ((Number) row[0]).intValue();
            totalReserved = ((Number) row[1]).intValue();
            totalAvailable = ((Number) row[2]).intValue();
            lowStockCount = ((Number) row[3]).intValue();
        }

        InventorySummaryResponse summary = InventorySummaryResponse.builder()
                .totalOnHand(totalOnHand)
                .totalReserved(totalReserved)
                .totalAvailable(totalAvailable)
                .lowStockCount(lowStockCount)
                .build();

        return AdminInventoryListResponse.builder()
                .content(items)
                .page(pageResult.getNumber() + 1)
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .hasNext(pageResult.hasNext())
                .summary(summary)
                .build();
    }

    /**
     * API 5.2: [GET] /api/v1/admin/inventories/{varId} — Chi tiết tồn kho biến thể SKU (STAFF / ADMIN)
     */
    @Transactional(readOnly = true)
    public AdminInventoryDetailResponse getInventoryDetail(Long varId) {
        securityHelper.authenticateStaffOrAdmin();

        ProductVariant variant = productVariantRepository.findById(varId)
                .orElseThrow(() -> new AppException(ErrorCode.ERR_VARIANT_NOT_FOUND, "Không tìm thấy dữ liệu tồn kho cho biến thể ID " + varId + "!"));

        Inventory inventory = inventoryRepository.findByVariant_VariantId(varId)
                .orElseGet(() -> inventoryRepository.save(Inventory.builder()
                        .variant(variant)
                        .quantity(0)
                        .reservedQuantity(0)
                        .minThreshold(5)
                        .build()));

        Product product = variant.getProduct();
        int onHand = inventory.getOnHandQty();
        int reserved = inventory.getReservedQty();
        int available = inventory.getAvailableQty();
        int threshold = inventory.getMinThreshold();
        String itemStatus = determineStatus(available, threshold);

        List<InventoryTransaction> transactions = inventoryTransactionRepository.findTop5ByVariant_VariantIdOrderByTxnIdDesc(varId);
        List<InventoryTransactionItemResponse> recentTxns = transactions.stream().map(txn -> {
            String form = txn.getFormCode();
            if (!StringUtils.hasText(form)) {
                form = "MB03";
            }
            String performedBy = "Hệ thống";
            if (txn.getPerformedBy() != null) {
                performedBy = txn.getPerformedBy().getFullName() != null
                        ? txn.getPerformedBy().getFullName() + " (" + (txn.getPerformedBy().getRole() != null ? txn.getPerformedBy().getRole().name() : "Nhân viên") + ")"
                        : "Nhân viên";
            }

            return InventoryTransactionItemResponse.builder()
                    .txnId(txn.getTxnId())
                    .formCode(form)
                    .sku(variant.getSku())
                    .productName(product != null ? product.getProductName() : null)
                    .txnType(txn.getTransactionType() != null ? txn.getTransactionType() : "EXPORT")
                    .quantityChange(txn.getQuantity())
                    .qtyBefore(txn.getQtyBefore())
                    .qtyAfter(txn.getQtyAfter())
                    .orderId(txn.getOrder() != null ? txn.getOrder().getOrderId() : null)
                    .reason(txn.getReason())
                    .performedBy(performedBy)
                    .performedAt(DateTimeUtil.formatIsoInstant(txn.getCreatedAt()))
                    .build();
        }).toList();

        return AdminInventoryDetailResponse.builder()
                .inventoryId(inventory.getInventoryId())
                .variantId(variant.getVariantId())
                .productId(product != null ? product.getProductId() : null)
                .productName(product != null ? product.getProductName() : null)
                .sku(variant.getSku())
                .color(variant.getColor())
                .ramGb(variant.getRamGb())
                .romGb(variant.getRomGb())
                .connectionType(variant.getConnectionType() != null ? variant.getConnectionType() : "5G")
                .listPrice(variant.getPrice())
                .onHandQty(onHand)
                .reservedQty(reserved)
                .availableQty(available)
                .minThreshold(threshold)
                .status(itemStatus)
                .recentTransactions(recentTxns)
                .updatedAt(DateTimeUtil.formatIsoInstant(inventory.getUpdatedAt()))
                .build();
    }

    /**
     * API 5.3: [POST] /api/v1/admin/inventories/adjustments — Lập phiếu kiểm kê điều chỉnh tồn kho MB05 (STAFF / ADMIN)
     */
    @Transactional
    public InventoryAdjustmentResponse adjustInventory(InventoryAdjustmentRequest request) {
        User currentUser = securityHelper.authenticateStaffOrAdmin();

        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new AppException(ErrorCode.ERR_VARIANT_NOT_FOUND, "Không tìm thấy dữ liệu tồn kho cho biến thể ID " + request.getVariantId() + "!"));

        Inventory inventory = inventoryRepository.findByVariant_VariantId(variant.getVariantId())
                .orElseGet(() -> inventoryRepository.save(Inventory.builder()
                        .variant(variant)
                        .quantity(0)
                        .reservedQuantity(0)
                        .minThreshold(5)
                        .build()));

        int reservedQty = inventory.getReservedQty();
        if (request.getActualCount() < reservedQty) {
            throw new AppException(
                    ErrorCode.ERR_STOCK_ADJUST_INVALID,
                    "Lỗi toàn vẹn dữ liệu: Số lượng thực tế kiểm kê (" + request.getActualCount() +
                            ") không được nhỏ hơn số lượng đang giữ chỗ cho khách (reserved_qty = " + reservedQty + ")!"
            );
        }

        int qtyBefore = inventory.getOnHandQty();
        int qtyActual = request.getActualCount();
        int quantityChange = qtyActual - qtyBefore;

        inventory.setQuantity(qtyActual);
        inventory.setUpdatedAt(LocalDateTime.now());
        inventoryRepository.save(inventory);

        // Sinh mã phiếu kiểm kê MB05-YYYYMMDD-XXXX
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = String.format("%04d", (int) (Math.random() * 9000) + 1000);
        String formCode = "MB05-" + datePart + "-" + randomSuffix;

        // Ghi sổ cái biến động kho bất biến
        InventoryTransaction txn = InventoryTransaction.builder()
                .variant(variant)
                .performedBy(currentUser)
                .transactionType("ADJUSTMENT")
                .formCode(formCode)
                .quantity(quantityChange)
                .qtyBefore(qtyBefore)
                .qtyAfter(qtyActual)
                .reason(request.getReason())
                .build();
        txn = inventoryTransactionRepository.save(txn);

        // Ghi Audit Log (D18)
        auditLogRepository.log(currentUser, "INVENTORY_ADJUSTMENT", "Inventories", inventory.getInventoryId(),
                "on_hand_qty: " + qtyBefore, "on_hand_qty: " + qtyActual + ", reason: " + request.getReason());

        String performedByName = currentUser.getFullName() != null
                ? currentUser.getFullName() + " (" + (currentUser.getRole() != null ? currentUser.getRole().name() : "Thủ kho") + ")"
                : "Thủ kho";

        Product product = variant.getProduct();
        int availableQty = Math.max(0, qtyActual - reservedQty);

        return InventoryAdjustmentResponse.builder()
                .adjustmentId(txn.getTxnId())
                .formCode(formCode)
                .variantId(variant.getVariantId())
                .sku(variant.getSku())
                .productName(product != null ? product.getProductName() : null)
                .qtyBefore(qtyBefore)
                .qtyActual(qtyActual)
                .quantityChange(quantityChange)
                .reservedQty(reservedQty)
                .availableQty(availableQty)
                .reason(request.getReason())
                .performedBy(performedByName)
                .performedAt(DateTimeUtil.formatIsoInstant(txn.getCreatedAt()))
                .build();
    }

    /**
     * API 5.4: [GET] /api/v1/admin/inventories/transactions — Sổ cái biến động kho bất biến (STAFF / ADMIN)
     */
    @Transactional(readOnly = true)
    public PageResponse<InventoryTransactionItemResponse> getInventoryTransactions(
            int page, int size, String formCode, String txnType, String startDate, String endDate) {
        securityHelper.authenticateStaffOrAdmin();

        int safePage = Math.max(page, 1) - 1;
        int safeSize = (size <= 0 || size > 100) ? 10 : size;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "txnId")));

        LocalDate parsedStartDate = null;
        if (StringUtils.hasText(startDate)) {
            try {
                parsedStartDate = LocalDate.parse(startDate.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException ex) {
                throw new AppException(ErrorCode.ERR_INVALID_DATE_FORMAT, "Định dạng ngày lọc startDate không hợp lệ (yêu cầu chuẩn YYYY-MM-DD)!");
            }
        }

        LocalDate parsedEndDate = null;
        if (StringUtils.hasText(endDate)) {
            try {
                parsedEndDate = LocalDate.parse(endDate.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException ex) {
                throw new AppException(ErrorCode.ERR_INVALID_DATE_FORMAT, "Định dạng ngày lọc endDate không hợp lệ (yêu cầu chuẩn YYYY-MM-DD)!");
            }
        }

        final LocalDate finalStartDate = parsedStartDate;
        final LocalDate finalEndDate = parsedEndDate;

        Specification<InventoryTransaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(formCode)) {
                predicates.add(cb.like(cb.lower(root.get("formCode")), "%" + formCode.trim().toLowerCase() + "%"));
            }

            if (StringUtils.hasText(txnType)) {
                predicates.add(cb.equal(cb.upper(root.get("transactionType")), txnType.trim().toUpperCase()));
            }

            if (finalStartDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), finalStartDate.atStartOfDay()));
            }

            if (finalEndDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), finalEndDate.atTime(LocalTime.MAX)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<InventoryTransaction> pageResult = inventoryTransactionRepository.findAll(spec, pageable);

        List<InventoryTransactionItemResponse> items = pageResult.getContent().stream().map(txn -> {
            ProductVariant v = txn.getVariant();
            Product p = v != null ? v.getProduct() : null;

            String performedBy = "Hệ thống";
            if (txn.getPerformedBy() != null) {
                performedBy = txn.getPerformedBy().getFullName() != null
                        ? txn.getPerformedBy().getFullName() + " (" + (txn.getPerformedBy().getRole() != null ? txn.getPerformedBy().getRole().name() : "Thủ kho") + ")"
                        : "Thủ kho";
            }

            return InventoryTransactionItemResponse.builder()
                    .txnId(txn.getTxnId())
                    .formCode(txn.getFormCode() != null ? txn.getFormCode() : "MB03")
                    .sku(v != null ? v.getSku() : null)
                    .productName(p != null ? p.getProductName() : null)
                    .txnType(txn.getTransactionType() != null ? txn.getTransactionType() : "EXPORT")
                    .quantityChange(txn.getQuantity())
                    .qtyBefore(txn.getQtyBefore())
                    .qtyAfter(txn.getQtyAfter())
                    .orderId(txn.getOrder() != null ? txn.getOrder().getOrderId() : null)
                    .reason(txn.getReason())
                    .performedBy(performedBy)
                    .performedAt(DateTimeUtil.formatIsoInstant(txn.getCreatedAt()))
                    .build();
        }).toList();

        return PageResponse.<InventoryTransactionItemResponse>builder()
                .content(items)
                .page(pageResult.getNumber() + 1)
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .hasNext(pageResult.hasNext())
                .build();
    }

    /**
     * API 5.5: [GET] /api/v1/admin/inventories/low-stock — Cảnh báo biến thể chạm ngưỡng an toàn (STAFF / ADMIN)
     */
    @Transactional(readOnly = true)
    public PageResponse<LowStockItemResponse> getLowStockAlerts(int page, int size) {
        securityHelper.authenticateStaffOrAdmin();

        int safePage = Math.max(page, 1) - 1;
        int safeSize = (size <= 0 || size > 100) ? 10 : size;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "quantity").and(Sort.by(Sort.Direction.DESC, "updatedAt")));

        Specification<Inventory> spec = (root, query, cb) -> {
            var availDiff = cb.diff(
                    cb.coalesce(root.get("quantity"), 0),
                    cb.coalesce(root.get("reservedQuantity"), 0)
            );
            var thresholdVal = cb.coalesce(root.get("minThreshold"), 5);
            return cb.lessThanOrEqualTo(availDiff, thresholdVal);
        };

        Page<Inventory> pageResult = inventoryRepository.findAll(spec, pageable);

        List<LowStockItemResponse> items = pageResult.getContent().stream().map(inv -> {
            ProductVariant v = inv.getVariant();
            Product p = v != null ? v.getProduct() : null;

            int onHand = inv.getOnHandQty();
            int reserved = inv.getReservedQty();
            int available = inv.getAvailableQty();
            int threshold = inv.getMinThreshold();
            int deficit = Math.max(0, threshold - available);
            String itemStatus = available <= 0 ? "OUT_OF_STOCK" : "LOW_STOCK";
            int recommendImport = Math.max(10, deficit + 15);

            return LowStockItemResponse.builder()
                    .variantId(v != null ? v.getVariantId() : null)
                    .productId(p != null ? p.getProductId() : null)
                    .productName(p != null ? p.getProductName() : null)
                    .sku(v != null ? v.getSku() : null)
                    .color(v != null ? v.getColor() : null)
                    .onHandQty(onHand)
                    .reservedQty(reserved)
                    .availableQty(available)
                    .minThreshold(threshold)
                    .deficit(deficit)
                    .status(itemStatus)
                    .recommendImportQty(recommendImport)
                    .build();
        }).toList();

        return PageResponse.<LowStockItemResponse>builder()
                .content(items)
                .page(pageResult.getNumber() + 1)
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .hasNext(pageResult.hasNext())
                .build();
    }
}
