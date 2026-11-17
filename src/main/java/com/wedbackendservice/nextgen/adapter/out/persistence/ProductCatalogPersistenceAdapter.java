package com.wedbackendservice.nextgen.adapter.out.persistence;

import com.wedbackendservice.nextgen.entity.Product;
import com.wedbackendservice.nextgen.entity.ProductVariant;
import com.wedbackendservice.nextgen.entity.PromotionVariant;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.port.out.product.ProductCatalogPort;
import com.wedbackendservice.nextgen.port.out.product.ProductValidationDto;
import com.wedbackendservice.nextgen.repository.ProductVariantRepository;
import com.wedbackendservice.nextgen.repository.PromotionVariantRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductCatalogPersistenceAdapter implements ProductCatalogPort {

  private final ProductVariantRepository productVariantRepository;
  private final PromotionVariantRepository promotionVariantRepository;

  @Override
  public Map<Long, ProductValidationDto> validateAndFetchPricing(
      Collection<Long> variantIds, LocalDateTime atTime) {
    if (variantIds == null || variantIds.isEmpty()) {
      return Collections.emptyMap();
    }

    List<Long> idsList = new ArrayList<>(variantIds);
    List<ProductVariant> variants = productVariantRepository.findAllById(idsList);
    if (variants == null || variants.size() != idsList.size()) {
      throw new AppException(
          ErrorCode.ERR_VARIANT_NOT_FOUND, "Có biến thể sản phẩm không tồn tại trong hệ thống!");
    }

    List<PromotionVariant> activePromos =
        promotionVariantRepository.findActivePromotionsForVariantIds(idsList, atTime);
    Map<Long, BigDecimal> bestPromoPriceMap = new HashMap<>();
    if (activePromos != null) {
      for (PromotionVariant pv : activePromos) {
        if (pv != null
            && pv.getVariant() != null
            && pv.getVariant().getVariantId() != null
            && pv.getPromotionalPrice() != null) {
          bestPromoPriceMap.merge(
              pv.getVariant().getVariantId(), pv.getPromotionalPrice(), BigDecimal::min);
        }
      }
    }

    Map<Long, ProductValidationDto> result = new LinkedHashMap<>();
    for (ProductVariant v : variants) {
      if (v.getStatus() != null && !"ACTIVE".equalsIgnoreCase(v.getStatus())) {
        String prodName =
            v.getProduct() != null ? v.getProduct().getProductName() : v.getVariantName();
        throw new AppException(
            ErrorCode.PRODUCT_INACTIVE,
            String.format(
                "Biến thể '%s' (SKU: %s) hiện đã ngừng kinh doanh!", prodName, v.getSku()));
      }
      if (v.getProduct() != null
          && v.getProduct().getStatus() != null
          && !"ACTIVE".equalsIgnoreCase(v.getProduct().getStatus())) {
        throw new AppException(
            ErrorCode.PRODUCT_INACTIVE,
            String.format(
                "Sản phẩm '%s' hiện đã ngừng kinh doanh!", v.getProduct().getProductName()));
      }

      BigDecimal unitPrice = v.getPrice();
      if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
        throw new AppException(ErrorCode.INVALID_REQUEST, "Biến thể sản phẩm chưa có giá hợp lệ!");
      }

      BigDecimal promoPrice = bestPromoPriceMap.get(v.getVariantId());
      BigDecimal effectivePrice =
          (promoPrice != null && promoPrice.compareTo(unitPrice) < 0) ? promoPrice : unitPrice;

      Product prod = v.getProduct();
      String prodName =
          prod != null
              ? prod.getProductName()
                  + (v.getVariantName() != null && !v.getVariantName().isBlank()
                      ? " (" + v.getVariantName() + ")"
                      : "")
              : v.getVariantName();

      result.put(
          v.getVariantId(),
          ProductValidationDto.builder()
              .variantId(v.getVariantId())
              .sku(v.getSku())
              .variantName(v.getVariantName())
              .productName(prodName)
              .romGb(v.getRomGb())
              .originalPrice(unitPrice)
              .promotionalPrice(promoPrice)
              .effectivePrice(effectivePrice)
              .variantEntity(v)
              .build());
    }

    return result;
  }
}
