package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.util.DateTimeUtil;

import com.wedbackendservice.nextgen.dto.request.promotion.CreatePromotionRequest;
import com.wedbackendservice.nextgen.dto.request.promotion.PromotionVariantItemRequest;
import com.wedbackendservice.nextgen.dto.request.promotion.UpdatePromotionStatusRequest;
import com.wedbackendservice.nextgen.dto.response.PageResponse;
import com.wedbackendservice.nextgen.dto.response.promotion.*;
import com.wedbackendservice.nextgen.entity.*;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.*;
import com.wedbackendservice.nextgen.security.SecurityHelper;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionsService {

    private final SecurityHelper securityHelper;
    private final PromotionRepository promotionRepository;
    private final PromotionVariantRepository promotionVariantRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryRepository inventoryRepository;
    private final AuditLogRepository auditLogRepository;

    private LocalDateTime parseIsoDateTime(String text, String fieldName) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(text.trim()), ZoneId.systemDefault());
        } catch (DateTimeParseException ex) {
            try {
                return LocalDateTime.parse(text.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Định dạng thời gian " + fieldName + " không hợp lệ (chuẩn ISO-8601 YYYY-MM-DDTHH:mm:ssZ)!");
            }
        }
    }

    /**
     * API 6.1: [GET] /api/v1/promotions/active — Danh sách Flash Sale đang diễn ra (Public)
     */
    @Transactional(readOnly = true)
    public List<ActivePromotionResponse> getActivePromotions() {
        LocalDateTime now = LocalDateTime.now();
        List<Promotion> promotions = promotionRepository.findActivePromotions(now);

        List<ActivePromotionResponse> result = new ArrayList<>();
        for (Promotion p : promotions) {
            List<PromotionVariant> pvs = promotionVariantRepository.findByPromotion_PromotionId(p.getPromotionId());
            List<ActivePromotionVariantResponse> variantResponses = new ArrayList<>();

            for (PromotionVariant pv : pvs) {
                ProductVariant v = pv.getVariant();
                if (v == null) continue;
                Product prod = v.getProduct();

                BigDecimal listPrice = v.getPrice() != null ? v.getPrice() : BigDecimal.ZERO;
                BigDecimal promoPrice = pv.getPromoPrice() != null ? pv.getPromoPrice() : listPrice;

                int discountPercent = 0;
                if (listPrice.compareTo(BigDecimal.ZERO) > 0 && promoPrice.compareTo(listPrice) < 0) {
                    discountPercent = listPrice.subtract(promoPrice)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(listPrice, 0, RoundingMode.HALF_UP)
                            .intValue();
                }

                int availableQty = inventoryRepository.findByVariant_VariantId(v.getVariantId())
                        .map(Inventory::getAvailableQty)
                        .orElse(0);

                variantResponses.add(ActivePromotionVariantResponse.builder()
                        .promoVariantId(pv.getPromoVariantId())
                        .variantId(v.getVariantId())
                        .productId(prod != null ? prod.getProductId() : null)
                        .productName(prod != null ? prod.getProductName() : null)
                        .sku(v.getSku())
                        .color(v.getColor())
                        .romGb(v.getRomGb())
                        .listPrice(listPrice)
                        .promoPrice(promoPrice)
                        .discountPercent(discountPercent)
                        .availableQty(availableQty)
                        .build());
            }

            result.add(ActivePromotionResponse.builder()
                    .promotionId(p.getPromotionId())
                    .name(p.getName())
                    .description(p.getDescription())
                    .startAt(DateTimeUtil.formatIsoInstant(p.getStartDate()))
                    .endAt(DateTimeUtil.formatIsoInstant(p.getEndDate()))
                    .status(p.resolveStatus(now))
                    .variants(variantResponses)
                    .build());
        }

        return result;
    }

    /**
     * API 6.2: [GET] /api/v1/admin/promotions — Quản lý chiến dịch khuyến mãi (STAFF / ADMIN)
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminPromotionItemResponse> getAdminPromotions(int page, int size, String status) {
        securityHelper.authenticateStaffOrAdmin();

        int safePage = Math.max(page, 1) - 1;
        int safeSize = (size <= 0 || size > 100) ? 10 : size;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "promotionId")));

        Specification<Promotion> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(status) && !status.equalsIgnoreCase("ALL")) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Promotion> pageResult = promotionRepository.findAll(spec, pageable);
        List<Promotion> promotions = pageResult.getContent();
        List<Long> promoIds = promotions.stream().map(Promotion::getPromotionId).toList();
        Map<Long, Integer> variantsCountMap = promoIds.isEmpty() ? Map.of() :
                promotionVariantRepository.countByPromotionIds(promoIds).stream()
                        .filter(r -> r != null && r[0] != null && r[1] != null)
                        .collect(Collectors.toMap(r -> (Long) r[0], r -> ((Number) r[1]).intValue(), (a, b) -> a));

        LocalDateTime now = LocalDateTime.now();

        List<AdminPromotionItemResponse> items = promotions.stream().map(p -> {
            int appliedVariantsCount = variantsCountMap.getOrDefault(p.getPromotionId(), 0);
            String createdByName = "Admin";
            if (p.getCreatedBy() != null) {
                createdByName = p.getCreatedBy().getFullName() != null
                        ? p.getCreatedBy().getFullName() + " (" + (p.getCreatedBy().getRole() != null ? p.getCreatedBy().getRole().name() : "Admin") + ")"
                        : "Admin";
            }

            return AdminPromotionItemResponse.builder()
                    .promotionId(p.getPromotionId())
                    .name(p.getName())
                    .description(p.getDescription())
                    .startAt(DateTimeUtil.formatIsoInstant(p.getStartDate()))
                    .endAt(DateTimeUtil.formatIsoInstant(p.getEndDate()))
                    .status(p.resolveStatus(now))
                    .appliedVariantsCount(appliedVariantsCount)
                    .createdBy(createdByName)
                    .createdAt(DateTimeUtil.formatIsoInstant(p.getCreatedAt()))
                    .build();
        }).toList();

        return PageResponse.<AdminPromotionItemResponse>builder()
                .content(items)
                .page(pageResult.getNumber() + 1)
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .hasNext(pageResult.hasNext())
                .build();
    }

    /**
     * API 6.3: [POST] /api/v1/admin/promotions — Tạo mới chiến dịch Flash Sale (STAFF / ADMIN)
     */
    @Transactional
    public CreatePromotionResponse createPromotion(CreatePromotionRequest request) {
        User currentUser = securityHelper.authenticateStaffOrAdmin();

        LocalDateTime startAt = parseIsoDateTime(request.getStartAt(), "startAt");
        LocalDateTime endAt = parseIsoDateTime(request.getEndAt(), "endAt");

        if (startAt != null && endAt != null && !startAt.isBefore(endAt)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Thời gian bắt đầu (startAt) phải trước thời gian kết thúc (endAt)!");
        }

        // Validate variants & prices
        List<ProductVariant> variants = new ArrayList<>();
        List<BigDecimal> promoPrices = new ArrayList<>();

        for (PromotionVariantItemRequest item : request.getVariants()) {
            ProductVariant pv = productVariantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new AppException(ErrorCode.ERR_VARIANT_NOT_FOUND, "Không tìm thấy biến thể sản phẩm ID " + item.getVariantId() + "!"));

            BigDecimal listPrice = pv.getPrice() != null ? pv.getPrice() : BigDecimal.ZERO;
            if (item.getPromoPrice().compareTo(listPrice) >= 0) {
                throw new AppException(ErrorCode.ERR_PROMO_PRICE_INVALID,
                        String.format("Giá khuyến mãi (%sđ) không được lớn hơn hoặc bằng giá niêm yết (%sđ) của biến thể ID %d!",
                                item.getPromoPrice().toPlainString(), listPrice.toPlainString(), pv.getVariantId()));
            }

            variants.add(pv);
            promoPrices.add(item.getPromoPrice());
        }

        String initialStatus = StringUtils.hasText(request.getStatus()) ? request.getStatus().trim().toUpperCase() : "UPCOMING";

        Promotion promotion = Promotion.builder()
                .promotionName(request.getName().trim())
                .description(request.getDescription())
                .startDate(startAt)
                .endDate(endAt)
                .status(initialStatus)
                .isActive(!"PAUSED".equalsIgnoreCase(initialStatus) && !"ENDED".equalsIgnoreCase(initialStatus))
                .createdBy(currentUser)
                .build();

        Promotion savedPromotion = promotionRepository.save(promotion);

        for (int i = 0; i < variants.size(); i++) {
            PromotionVariant pv = PromotionVariant.builder()
                    .promotion(savedPromotion)
                    .variant(variants.get(i))
                    .promotionalPrice(promoPrices.get(i))
                    .build();
            promotionVariantRepository.save(pv);
        }

        // Ghi Audit Log (D18)
        auditLogRepository.log(currentUser, "CREATE_PROMOTION", "Promotions", savedPromotion.getPromotionId(),
                null, "Tạo mới chiến dịch: " + savedPromotion.getName() + " kèm " + variants.size() + " biến thể");

        return CreatePromotionResponse.builder()
                .promotionId(savedPromotion.getPromotionId())
                .name(savedPromotion.getName())
                .appliedVariantsCount(variants.size())
                .status(savedPromotion.getStatus())
                .startAt(DateTimeUtil.formatIsoInstant(savedPromotion.getStartDate()))
                .endAt(DateTimeUtil.formatIsoInstant(savedPromotion.getEndDate()))
                .createdAt(DateTimeUtil.formatIsoInstant(savedPromotion.getCreatedAt() != null ? savedPromotion.getCreatedAt() : LocalDateTime.now()))
                .build();
    }

    /**
     * API 6.4: [PUT] /api/v1/admin/promotions/{id}/status — Cập nhật trạng thái chiến dịch khuyến mãi (STAFF / ADMIN)
     */
    @Transactional
    public UpdatePromotionStatusResponse updatePromotionStatus(Long id, UpdatePromotionStatusRequest request) {
        User currentUser = securityHelper.authenticateStaffOrAdmin();

        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ERR_PROMOTION_NOT_FOUND, "Không tìm thấy chiến dịch khuyến mãi có ID " + id + "!"));

        String oldStatus = promotion.getStatus();
        String newStatus = request.getStatus().trim().toUpperCase();

        promotion.setStatus(newStatus);
        if ("PAUSED".equalsIgnoreCase(newStatus) || "ENDED".equalsIgnoreCase(newStatus)) {
            promotion.setIsActive(false);
        } else if ("ACTIVE".equalsIgnoreCase(newStatus)) {
            promotion.setIsActive(true);
        }
        promotion.setUpdatedAt(LocalDateTime.now());
        promotionRepository.save(promotion);

        // Ghi Audit Log (D18)
        auditLogRepository.log(currentUser, "UPDATE_PROMOTION_STATUS", "Promotions", promotion.getPromotionId(),
                oldStatus, newStatus + (StringUtils.hasText(request.getReason()) ? " | Lý do: " + request.getReason() : ""));

        return UpdatePromotionStatusResponse.builder()
                .promotionId(promotion.getPromotionId())
                .status(promotion.getStatus())
                .updatedAt(DateTimeUtil.formatIsoInstant(promotion.getUpdatedAt()))
                .build();
    }
}
