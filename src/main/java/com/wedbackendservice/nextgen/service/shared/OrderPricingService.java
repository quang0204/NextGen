package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.dto.client.request.order.OrderItemRequest;
import com.wedbackendservice.nextgen.entity.OrderItem;
import com.wedbackendservice.nextgen.entity.Product;
import com.wedbackendservice.nextgen.entity.ProductVariant;
import com.wedbackendservice.nextgen.entity.PromotionVariant;
import com.wedbackendservice.nextgen.entity.Voucher;
import com.wedbackendservice.nextgen.event.saga.OrderCreatedEvent;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.ProductVariantRepository;
import com.wedbackendservice.nextgen.repository.PromotionVariantRepository;
import com.wedbackendservice.nextgen.repository.VoucherRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPricingService {

    private final ProductVariantRepository productVariantRepository;
    private final PromotionVariantRepository promotionVariantRepository;
    private final VoucherRepository voucherRepository;
    private final ShippingFeeCalculator shippingFeeCalculator;

    private static final DecimalFormat CURRENCY_FORMATTER = new DecimalFormat("#,###");

    @Getter
    @Builder
    public static class PricingResult {
        private final Map<Long, Integer> consolidatedQty;
        private final Map<Long, ProductVariant> variantMap;
        private final List<OrderItem> orderItemsToSave;
        private final List<OrderCreatedEvent.OrderItemDto> eventItems;
        private final BigDecimal subtotal;
        private final Voucher appliedVoucher;
        private final BigDecimal voucherDiscount;
        private final BigDecimal shippingFee;
        private final BigDecimal totalAmount;
    }

    public PricingResult calculatePricing(List<OrderItemRequest> itemRequests,
                                          String voucherCode,
                                          String shippingMethod,
                                          BigDecimal expectedTotalAmount,
                                          LocalDateTime now) {
        // 1. Validate & consolidate items
        Map<Long, Integer> consolidatedQty = validateAndConsolidateItems(itemRequests);

        // 2. Batch load & validate variants
        List<Long> variantIds = new ArrayList<>(consolidatedQty.keySet());
        List<ProductVariant> variants = productVariantRepository.findAllById(variantIds);
        if (variants == null || variants.size() != variantIds.size()) {
            throw new AppException(ErrorCode.ERR_VARIANT_NOT_FOUND, "Có biến thể sản phẩm không tồn tại trong hệ thống!");
        }

        Map<Long, ProductVariant> variantMap = new HashMap<>();
        for (ProductVariant v : variants) {
            variantMap.put(v.getVariantId(), v);
            if (v.getStatus() != null && !"ACTIVE".equalsIgnoreCase(v.getStatus())) {
                String prodName = v.getProduct() != null ? v.getProduct().getProductName() : v.getVariantName();
                throw new AppException(ErrorCode.PRODUCT_INACTIVE,
                        String.format("Biến thể '%s' (SKU: %s) hiện đã ngừng kinhdong!", prodName, v.getSku()));
            }
            if (v.getProduct() != null && v.getProduct().getStatus() != null && !"ACTIVE".equalsIgnoreCase(v.getProduct().getStatus())) {
                throw new AppException(ErrorCode.PRODUCT_INACTIVE,
                        String.format("Sản phẩm '%s' hiện đã ngừng kinh doanh!", v.getProduct().getProductName()));
            }
        }

        // 3. Batch load Promotions (Lấy khuyến mãi thấp nhất, tránh N+1)
        List<PromotionVariant> activePromos = promotionVariantRepository.findActivePromotionsForVariantIds(variantIds, now);
        Map<Long, BigDecimal> bestPromoPriceMap = new HashMap<>();
        if (activePromos != null) {
            for (PromotionVariant pv : activePromos) {
                if (pv != null && pv.getVariant() != null && pv.getVariant().getVariantId() != null && pv.getPromotionalPrice() != null) {
                    bestPromoPriceMap.merge(pv.getVariant().getVariantId(), pv.getPromotionalPrice(), BigDecimal::min);
                }
            }
        }

        // 4. Calculate subtotal & items
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItemsToSave = new ArrayList<>();
        List<OrderCreatedEvent.OrderItemDto> eventItems = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : consolidatedQty.entrySet()) {
            Long vId = entry.getKey();
            int qty = entry.getValue();
            ProductVariant variant = variantMap.get(vId);

            BigDecimal unitPrice = variant.getPrice() != null ? variant.getPrice() : BigDecimal.ZERO;
            BigDecimal promoPrice = bestPromoPriceMap.get(vId);
            if (promoPrice != null && promoPrice.compareTo(unitPrice) < 0) {
                unitPrice = promoPrice;
            }

            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
            subtotal = subtotal.add(lineTotal);

            Product prod = variant.getProduct();
            String prodName = prod != null
                    ? prod.getProductName() + (variant.getVariantName() != null && !variant.getVariantName().isBlank() ? " (" + variant.getVariantName() + ")" : "")
                    : variant.getVariantName();
            String rom = variant.getRomGb() != null ? variant.getRomGb() + "GB" : null;

            orderItemsToSave.add(OrderItem.builder()
                    .variant(variant)
                    .productNameSnapshot(prodName)
                    .skuSnapshot(variant.getSku())
                    .colorSnapshot(variant.getColor())
                    .romSnapshot(rom)
                    .imageUrl(variant.getImageUrl())
                    .price(unitPrice)
                    .quantity(qty)
                    .totalPrice(lineTotal)
                    .build());

            eventItems.add(OrderCreatedEvent.OrderItemDto.builder()
                    .variantId(vId)
                    .quantity(qty)
                    .unitPrice(unitPrice)
                    .build());
        }

        // 5. Áp dụng Voucher (nếu có)
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        Voucher appliedVoucher = null;
        if (StringUtils.hasText(voucherCode)) {
            appliedVoucher = applyVoucherAtomic(voucherCode.trim(), subtotal, now);
            voucherDiscount = calculateVoucherDiscount(appliedVoucher, subtotal);
        }

        // 6. Tính phí ship độc lập từ Server
        BigDecimal shippingFee = shippingFeeCalculator.calculateFee(shippingMethod);
        BigDecimal totalAmount = subtotal.subtract(voucherDiscount).add(shippingFee).max(BigDecimal.ZERO);

        // 7. So khớp chính xác expectedTotalAmount nếu client gửi lên
        if (expectedTotalAmount != null && totalAmount.compareTo(expectedTotalAmount) != 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    String.format("Giá sản phẩm hoặc khuyến mãi vừa thay đổi (Tổng tiền mới: %sđ). Vui lòng xác nhận lại đơn hàng!",
                            CURRENCY_FORMATTER.format(totalAmount)));
        }

        return PricingResult.builder()
                .consolidatedQty(consolidatedQty)
                .variantMap(variantMap)
                .orderItemsToSave(orderItemsToSave)
                .eventItems(eventItems)
                .subtotal(subtotal)
                .appliedVoucher(appliedVoucher)
                .voucherDiscount(voucherDiscount)
                .shippingFee(shippingFee)
                .totalAmount(totalAmount)
                .build();
    }

    public Map<Long, Integer> validateAndConsolidateItems(List<OrderItemRequest> items) {
        Map<Long, Integer> consolidated = new LinkedHashMap<>();
        if (items != null) {
            for (OrderItemRequest req : items) {
                if (req.getVariantId() != null && req.getQuantity() != null && req.getQuantity() > 0) {
                    consolidated.merge(req.getVariantId(), req.getQuantity(), Integer::sum);
                }
            }
        }
        if (consolidated.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Danh sách sản phẩm không hợp lệ!");
        }
        return consolidated;
    }

    public Voucher applyVoucherAtomic(String vCode, BigDecimal subtotal, LocalDateTime now) {
        Voucher appliedVoucher = voucherRepository.findByVoucherCodeIgnoreCase(vCode)
                .orElseThrow(() -> new AppException(ErrorCode.ERR_VOUCHER_NOT_FOUND, "Mã giảm giá '" + vCode + "' không tồn tại!"));

        if (!Boolean.TRUE.equals(appliedVoucher.getIsActive())) {
            throw new AppException(ErrorCode.ERR_VOUCHER_EXPIRED, "Mã giảm giá '" + vCode + "' đã ngừng hoạt động!");
        }
        if (appliedVoucher.getStartDate() != null && now.isBefore(appliedVoucher.getStartDate())) {
            throw new AppException(ErrorCode.ERR_VOUCHER_EXPIRED, "Mã giảm giá '" + vCode + "' chưa đến thời gian có hiệu lực!");
        }
        if (appliedVoucher.getEndDate() != null && now.isAfter(appliedVoucher.getEndDate())) {
            throw new AppException(ErrorCode.ERR_VOUCHER_EXPIRED, "Mã giảm giá '" + vCode + "' đã hết hạn sử dụng!");
        }
        if (appliedVoucher.getMinOrderValue() != null && subtotal.compareTo(appliedVoucher.getMinOrderValue()) < 0) {
            throw new AppException(ErrorCode.ERR_VOUCHER_MIN_ORDER,
                    String.format("Đơn hàng chưa đạt giá trị tối thiểu %sđ để áp dụng mã %s!",
                            CURRENCY_FORMATTER.format(appliedVoucher.getMinOrderValue()), appliedVoucher.getCode()));
        }
        if (appliedVoucher.getUsageLimit() != null && appliedVoucher.getUsedCount() != null && appliedVoucher.getUsedCount() >= appliedVoucher.getUsageLimit()) {
            throw new AppException(ErrorCode.ERR_VOUCHER_LIMIT_REACHED, "Mã giảm giá '" + vCode + "' đã hết lượt sử dụng trên hệ thống!");
        }

        int voucherUpdated = voucherRepository.incrementUsedCountAtomic(appliedVoucher.getVoucherId());
        if (voucherUpdated == 0) {
            throw new AppException(ErrorCode.ERR_VOUCHER_LIMIT_REACHED, "Mã giảm giá '" + vCode + "' vừa hết lượt sử dụng!");
        }
        return appliedVoucher;
    }

    public BigDecimal calculateVoucherDiscount(Voucher appliedVoucher, BigDecimal subtotal) {
        if (appliedVoucher == null) return BigDecimal.ZERO;
        String discType = appliedVoucher.getDiscountType() != null ? appliedVoucher.getDiscountType().trim().toUpperCase() : "FIXED";
        BigDecimal voucherDiscount;
        if ("PERCENT".equals(discType) || "PERCENTAGE".equals(discType)) {
            BigDecimal pct = appliedVoucher.getDiscountValue() != null ? appliedVoucher.getDiscountValue() : BigDecimal.ZERO;
            voucherDiscount = subtotal.multiply(pct).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (appliedVoucher.getMaxDiscount() != null && appliedVoucher.getMaxDiscount().compareTo(BigDecimal.ZERO) > 0) {
                voucherDiscount = voucherDiscount.min(appliedVoucher.getMaxDiscount());
            }
        } else {
            BigDecimal fixedVal = appliedVoucher.getDiscountValue() != null ? appliedVoucher.getDiscountValue() : BigDecimal.ZERO;
            voucherDiscount = fixedVal.min(subtotal);
        }
        return voucherDiscount.min(subtotal);
    }
}
