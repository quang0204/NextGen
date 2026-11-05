package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.util.DateTimeUtil;

import com.wedbackendservice.nextgen.dto.request.voucher.ApplyVoucherRequest;
import com.wedbackendservice.nextgen.dto.request.voucher.CreateVoucherRequest;
import com.wedbackendservice.nextgen.dto.response.PageResponse;
import com.wedbackendservice.nextgen.dto.response.voucher.*;
import com.wedbackendservice.nextgen.entity.AuditLog;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.entity.Voucher;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.repository.VoucherRepository;
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
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VouchersService {

    private final SecurityHelper securityHelper;
    private final VoucherRepository voucherRepository;
    private final AuditLogRepository auditLogRepository;
    private static final DecimalFormat CURRENCY_FORMATTER = new DecimalFormat("#,###");

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
                throw new AppException(ErrorCode.INVALID_REQUEST, "Định dạng thời gian " + fieldName + " không hợp lệ (chuẩn ISO-8601)!");
            }
        }
    }

    /**
     * API 6.5: [GET] /api/v1/vouchers/valid — Danh sách mã giảm giá còn hiệu lực (Public / Auth)
     */
    @Transactional(readOnly = true)
    public List<ValidVoucherResponse> getValidVouchers(BigDecimal minOrderValue) {
        LocalDateTime now = LocalDateTime.now();
        List<Voucher> vouchers = voucherRepository.findValidVouchers(now);

        List<ValidVoucherResponse> result = new ArrayList<>();
        for (Voucher v : vouchers) {
            if (minOrderValue != null && v.getMinOrderValue() != null) {
                if (minOrderValue.compareTo(v.getMinOrderValue()) < 0) {
                    continue;
                }
            }

            int used = v.getUsedCount() != null ? v.getUsedCount() : 0;
            Integer remaining = v.getUsageLimit() != null ? Math.max(0, v.getUsageLimit() - used) : null;

            result.add(ValidVoucherResponse.builder()
                    .voucherId(v.getVoucherId())
                    .code(v.getCode())
                    .discountType(v.getDiscountType() != null ? v.getDiscountType().toUpperCase() : "FIXED")
                    .discountValue(v.getDiscountValue())
                    .maxDiscount(v.getMaxDiscount())
                    .minOrderValue(v.getMinOrderValue())
                    .usageLimit(v.getUsageLimit())
                    .usedCount(used)
                    .remainingCount(remaining)
                    .expiredAt(DateTimeUtil.formatIsoInstant(v.getEndDate()))
                    .build());
        }

        return result;
    }

    /**
     * API 6.6: [POST] /api/v1/vouchers/apply — Kiểm tra hợp lệ và tính mức giảm voucher (Authenticated)
     */
    public ApplyVoucherResponse applyVoucher(ApplyVoucherRequest request) {
        securityHelper.getCurrentUser();

        String code = request.getCode().trim();
        Voucher voucher = voucherRepository.findByVoucherCodeIgnoreCase(code)
                .orElseThrow(() -> new AppException(ErrorCode.ERR_VOUCHER_NOT_FOUND, "Mã giảm giá '" + code + "' không tồn tại trên hệ thống!"));

        if (!Boolean.TRUE.equals(voucher.getIsActive())) {
            throw new AppException(ErrorCode.ERR_VOUCHER_EXPIRED, "Mã giảm giá '" + code + "' đang tạm thời bị khóa hoặc ngừng kích hoạt!");
        }

        LocalDateTime now = LocalDateTime.now();
        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
            throw new AppException(ErrorCode.ERR_VOUCHER_EXPIRED, "Mã giảm giá '" + code + "' chưa đến thời gian có hiệu lực!");
        }

        if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) {
            throw new AppException(ErrorCode.ERR_VOUCHER_EXPIRED, "Mã giảm giá '" + code + "' đã hết hạn sử dụng!");
        }

        int used = voucher.getUsedCount() != null ? voucher.getUsedCount() : 0;
        if (voucher.getUsageLimit() != null && used >= voucher.getUsageLimit()) {
            throw new AppException(ErrorCode.ERR_VOUCHER_LIMIT_REACHED, "Mã giảm giá '" + code + "' đã hết lượt sử dụng trên toàn hệ thống!");
        }

        BigDecimal subtotal = request.getSubtotal();
        if (voucher.getMinOrderValue() != null && subtotal.compareTo(voucher.getMinOrderValue()) < 0) {
            String minOrderFormatted = CURRENCY_FORMATTER.format(voucher.getMinOrderValue()) + "đ";
            throw new AppException(ErrorCode.ERR_VOUCHER_MIN_ORDER,
                    String.format("Đơn hàng chưa đạt giá trị tối thiểu %s để áp dụng mã %s!", minOrderFormatted, voucher.getCode()));
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        String type = voucher.getDiscountType() != null ? voucher.getDiscountType().trim().toUpperCase() : "FIXED";

        if ("PERCENT".equals(type)) {
            BigDecimal percent = voucher.getDiscountValue() != null ? voucher.getDiscountValue() : BigDecimal.ZERO;
            discountAmount = subtotal.multiply(percent).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (voucher.getMaxDiscount() != null && voucher.getMaxDiscount().compareTo(BigDecimal.ZERO) > 0) {
                discountAmount = discountAmount.min(voucher.getMaxDiscount());
            }
        } else {
            // FIXED
            BigDecimal fixedVal = voucher.getDiscountValue() != null ? voucher.getDiscountValue() : BigDecimal.ZERO;
            discountAmount = fixedVal.min(subtotal);
        }

        BigDecimal finalSubtotal = subtotal.subtract(discountAmount);
        if (finalSubtotal.compareTo(BigDecimal.ZERO) < 0) {
            finalSubtotal = BigDecimal.ZERO;
        }

        return ApplyVoucherResponse.builder()
                .voucherId(voucher.getVoucherId())
                .code(voucher.getCode())
                .discountType(type)
                .discountValue(voucher.getDiscountValue())
                .discountAmount(discountAmount)
                .originalSubtotal(subtotal)
                .finalSubtotal(finalSubtotal)
                .build();
    }

    /**
     * API 6.7: [GET] /api/v1/admin/vouchers — Quản trị danh sách mã giảm giá (STAFF / ADMIN)
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminVoucherItemResponse> getAdminVouchers(int page, int size, String keyword, String status) {
        securityHelper.authenticateStaffOrAdmin();

        int safePage = Math.max(page, 1) - 1;
        int safeSize = (size <= 0 || size > 100) ? 10 : size;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "voucherId")));

        Specification<Voucher> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String kw = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("voucherCode")), kw),
                        cb.like(cb.lower(root.get("voucherName")), kw)
                ));
            }

            if (StringUtils.hasText(status) && !status.equalsIgnoreCase("ALL")) {
                if ("ACTIVE".equalsIgnoreCase(status)) {
                    predicates.add(cb.isTrue(root.get("isActive")));
                } else if ("INACTIVE".equalsIgnoreCase(status) || "PAUSED".equalsIgnoreCase(status)) {
                    predicates.add(cb.isFalse(root.get("isActive")));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Voucher> pageResult = voucherRepository.findAll(spec, pageable);

        List<AdminVoucherItemResponse> items = pageResult.getContent().stream().map(v -> AdminVoucherItemResponse.builder()
                .voucherId(v.getVoucherId())
                .code(v.getCode())
                .discountType(v.getDiscountType() != null ? v.getDiscountType().toUpperCase() : "FIXED")
                .discountValue(v.getDiscountValue())
                .maxDiscount(v.getMaxDiscount())
                .minOrderValue(v.getMinOrderValue())
                .usageLimit(v.getUsageLimit())
                .usedCount(v.getUsedCount() != null ? v.getUsedCount() : 0)
                .startAt(DateTimeUtil.formatIsoInstant(v.getStartDate()))
                .expiredAt(DateTimeUtil.formatIsoInstant(v.getEndDate()))
                .isActive(v.getIsActive())
                .build()).toList();

        return PageResponse.<AdminVoucherItemResponse>builder()
                .content(items)
                .page(pageResult.getNumber() + 1)
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .hasNext(pageResult.hasNext())
                .build();
    }

    /**
     * API 6.8: [POST] /api/v1/admin/vouchers — Khởi tạo mã voucher mới (STAFF / ADMIN)
     */
    @Transactional
    public CreateVoucherResponse createVoucher(CreateVoucherRequest request) {
        User currentUser = securityHelper.authenticateStaffOrAdmin();

        String code = request.getCode().trim().toUpperCase();
        if (voucherRepository.existsByVoucherCodeIgnoreCase(code)) {
            throw new AppException(ErrorCode.ERR_VOUCHER_DUPLICATE_CODE,
                    String.format("Mã voucher '%s' đã tồn tại trên hệ thống!", code));
        }

        LocalDateTime startAt = parseIsoDateTime(request.getStartAt(), "startAt");
        LocalDateTime expiredAt = parseIsoDateTime(request.getExpiredAt(), "expiredAt");

        if (startAt != null && expiredAt != null && !startAt.isBefore(expiredAt)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Thời gian bắt đầu (startAt) phải trước thời gian hết hạn (expiredAt)!");
        }

        String type = request.getDiscountType().trim().toUpperCase();
        if (!"FIXED".equals(type) && !"PERCENT".equals(type)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Loại giảm giá không hợp lệ (chỉ chấp nhận FIXED hoặc PERCENT)!");
        }

        Voucher voucher = Voucher.builder()
                .voucherCode(code)
                .voucherName("Voucher " + code)
                .discountType(type)
                .discountValue(request.getDiscountValue())
                .maxDiscountAmount(request.getMaxDiscount())
                .minOrderValue(request.getMinOrderValue())
                .usageLimit(request.getUsageLimit())
                .usedCount(0)
                .startDate(startAt)
                .endDate(expiredAt)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        Voucher savedVoucher = voucherRepository.save(voucher);

        // Ghi Audit Log (D18)
        auditLogRepository.log(currentUser, "CREATE_VOUCHER", "Vouchers", savedVoucher.getVoucherId(),
                null, "Khởi tạo mã voucher mới: " + savedVoucher.getCode());

        return CreateVoucherResponse.builder()
                .voucherId(savedVoucher.getVoucherId())
                .code(savedVoucher.getCode())
                .discountType(savedVoucher.getDiscountType())
                .discountValue(savedVoucher.getDiscountValue())
                .usageLimit(savedVoucher.getUsageLimit())
                .usedCount(savedVoucher.getUsedCount())
                .isActive(savedVoucher.getIsActive())
                .createdAt(DateTimeUtil.formatIsoInstant(savedVoucher.getCreatedAt() != null ? savedVoucher.getCreatedAt() : LocalDateTime.now()))
                .build();
    }

    /**
     * API 6.9: [PATCH] /api/v1/admin/vouchers/{id}/toggle — Bật / Tắt kích hoạt nhanh voucher (STAFF / ADMIN)
     */
    @Transactional
    public ToggleVoucherResponse toggleVoucher(Long id) {
        User currentUser = securityHelper.authenticateStaffOrAdmin();

        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ERR_VOUCHER_NOT_FOUND, "Không tìm thấy voucher có ID " + id + "!"));

        boolean newActive = !Boolean.TRUE.equals(voucher.getIsActive());
        voucher.setIsActive(newActive);
        voucher.setUpdatedAt(LocalDateTime.now());
        voucherRepository.save(voucher);

        // Ghi Audit Log (D18)
        auditLogRepository.log(currentUser, "TOGGLE_VOUCHER", "Vouchers", voucher.getVoucherId(),
                "isActive: " + !newActive, "isActive: " + newActive);

        return ToggleVoucherResponse.builder()
                .voucherId(voucher.getVoucherId())
                .code(voucher.getCode())
                .isActive(voucher.getIsActive())
                .updatedAt(DateTimeUtil.formatIsoInstant(voucher.getUpdatedAt()))
                .build();
    }
}
