package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.dto.client.request.order.OrderItemRequest;
import com.wedbackendservice.nextgen.entity.OrderItem;
import com.wedbackendservice.nextgen.entity.ProductVariant;
import com.wedbackendservice.nextgen.entity.Voucher;
import com.wedbackendservice.nextgen.event.saga.OrderCreatedEvent;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.port.out.product.ProductCatalogPort;
import com.wedbackendservice.nextgen.port.out.product.ProductValidationDto;
import com.wedbackendservice.nextgen.port.out.voucher.VoucherApplyResult;
import com.wedbackendservice.nextgen.port.out.voucher.VoucherPort;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPricingService {

  private final ProductCatalogPort productCatalogPort;
  private final VoucherPort voucherPort;
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

  public PricingResult calculatePricing(
      List<OrderItemRequest> itemRequests,
      String voucherCode,
      String shippingMethod,
      BigDecimal expectedTotalAmount,
      LocalDateTime now) {
    // 1. Validate & consolidate items (TreeMap for deterministic deadlock-free locking)
    Map<Long, Integer> consolidatedQty = validateAndConsolidateItems(itemRequests);

    // 2. Batch load & validate variants via ProductCatalogPort
    Map<Long, ProductValidationDto> validationMap =
        productCatalogPort.validateAndFetchPricing(consolidatedQty.keySet(), now);

    // 3. Calculate subtotal & items
    BigDecimal subtotal = BigDecimal.ZERO;
    List<OrderItem> orderItemsToSave = new ArrayList<>();
    List<OrderCreatedEvent.OrderItemDto> eventItems = new ArrayList<>();
    Map<Long, ProductVariant> variantMap = new HashMap<>();

    for (Map.Entry<Long, Integer> entry : consolidatedQty.entrySet()) {
      Long vId = entry.getKey();
      int qty = entry.getValue();
      ProductValidationDto productDto = validationMap.get(vId);

      BigDecimal unitPrice = productDto.getEffectivePrice();
      BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
      subtotal = subtotal.add(lineTotal);
      variantMap.put(vId, productDto.getVariantEntity());

      String rom = productDto.getRomGb() != null ? productDto.getRomGb() + "GB" : null;

      orderItemsToSave.add(
          OrderItem.builder()
              .variant(productDto.getVariantEntity())
              .productNameSnapshot(productDto.getProductName())
              .skuSnapshot(productDto.getSku())
              .colorSnapshot(
                  productDto.getVariantEntity() != null
                      ? productDto.getVariantEntity().getColor()
                      : null)
              .romSnapshot(rom)
              .imageUrl(
                  productDto.getVariantEntity() != null
                      ? productDto.getVariantEntity().getImageUrl()
                      : null)
              .price(unitPrice)
              .quantity(qty)
              .totalPrice(lineTotal)
              .build());

      eventItems.add(
          OrderCreatedEvent.OrderItemDto.builder()
              .variantId(vId)
              .quantity(qty)
              .unitPrice(unitPrice)
              .build());
    }

    // 4. Áp dụng Voucher (nếu có) via VoucherPort
    BigDecimal voucherDiscount = BigDecimal.ZERO;
    Voucher appliedVoucher = null;
    if (StringUtils.hasText(voucherCode)) {
      VoucherApplyResult voucherResult =
          voucherPort.applyVoucherAtomic(voucherCode.trim(), subtotal, now);
      if (voucherResult != null) {
        appliedVoucher = voucherResult.getVoucherEntity();
        voucherDiscount = voucherResult.getDiscountAmount();
      }
    }

    // 5. Tính phí ship độc lập từ Server
    BigDecimal shippingFee = shippingFeeCalculator.calculateFee(shippingMethod);
    BigDecimal totalAmount =
        subtotal.subtract(voucherDiscount).add(shippingFee).max(BigDecimal.ZERO);

    // 6. So khớp chính xác expectedTotalAmount nếu client gửi lên
    if (expectedTotalAmount != null && totalAmount.compareTo(expectedTotalAmount) != 0) {
      throw new AppException(
          ErrorCode.INVALID_REQUEST,
          String.format(
              "Giá sản phẩm hoặc khuyến mãi vừa thay đổi (Tổng tiền mới: %sđ). Vui lòng xác nhận lại"
                  + " đơn hàng!",
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
    Map<Long, Integer> consolidated = new TreeMap<>();
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
    VoucherApplyResult res = voucherPort.applyVoucherAtomic(vCode, subtotal, now);
    return res != null ? res.getVoucherEntity() : null;
  }
}
