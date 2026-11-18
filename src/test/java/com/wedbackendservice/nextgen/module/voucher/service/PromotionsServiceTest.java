package com.wedbackendservice.nextgen.module.voucher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.security.SecurityHelper;
import com.wedbackendservice.nextgen.module.notification.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.module.product.entity.Product;
import com.wedbackendservice.nextgen.module.product.entity.ProductVariant;
import com.wedbackendservice.nextgen.module.product.repository.ProductVariantRepository;
import com.wedbackendservice.nextgen.module.user.entity.User;
import com.wedbackendservice.nextgen.module.user.enums.Role;
import com.wedbackendservice.nextgen.module.voucher.dto.portal.request.CreatePromotionRequest;
import com.wedbackendservice.nextgen.module.voucher.dto.portal.request.PromotionVariantItemRequest;
import com.wedbackendservice.nextgen.module.voucher.dto.portal.response.CreatePromotionResponse;
import com.wedbackendservice.nextgen.module.voucher.entity.Promotion;
import com.wedbackendservice.nextgen.module.voucher.entity.PromotionVariant;
import com.wedbackendservice.nextgen.module.voucher.repository.PromotionRepository;
import com.wedbackendservice.nextgen.module.voucher.repository.PromotionVariantRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromotionsServiceTest {

  @Mock private PromotionRepository promotionRepository;
  @Mock private PromotionVariantRepository promotionVariantRepository;
  @Mock private ProductVariantRepository productVariantRepository;
  @Mock private AuditLogRepository auditLogRepository;
  @Mock private SecurityHelper securityHelper;

  @InjectMocks private PromotionsService promotionsService;

  private User adminUser;
  private Product product;
  private ProductVariant variant;

  @BeforeEach
  void setUp() {
    adminUser = User.builder().userId(1L).fullName("Admin").role(Role.ADMIN).build();
    product = Product.builder().productId(10L).productName("iPhone 16 Pro").build();
    variant =
        ProductVariant.builder()
            .variantId(100L)
            .product(product)
            .sku("IP16P-256")
            .price(BigDecimal.valueOf(30000000))
            .build();
  }

  @Test
  void createPromotion_throwsWhenProductAlreadyInOverlappingCampaign() {
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(adminUser);
    when(productVariantRepository.findAllById(List.of(100L))).thenReturn(List.of(variant));

    Promotion existingPromo =
        Promotion.builder()
            .promotionId(5L)
            .promotionName("Flash Sale Thang 11")
            .status("ACTIVE")
            .endDate(LocalDateTime.now().plusDays(5))
            .build();
    PromotionVariant conflict =
        PromotionVariant.builder().promotion(existingPromo).variant(variant).build();

    when(promotionVariantRepository.findConflictingPromotionVariants(
            any(), any(), isNull(), any(), any(), any()))
        .thenReturn(List.of(conflict));

    CreatePromotionRequest req =
        CreatePromotionRequest.builder()
            .name("Black Friday")
            .startAt(LocalDateTime.now().plusHours(1).toString())
            .endAt(LocalDateTime.now().plusDays(3).toString())
            .status("ACTIVE")
            .variants(
                List.of(
                    PromotionVariantItemRequest.builder()
                        .variantId(100L)
                        .promoPrice(BigDecimal.valueOf(25000000))
                        .build()))
            .build();

    assertThatThrownBy(() -> promotionsService.createPromotion(req))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("Flash Sale Thang 11");

    verify(promotionRepository, never()).save(any());
  }

  @Test
  void createPromotion_succeedsWhenNoOverlappingCampaign() {
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(adminUser);
    when(productVariantRepository.findAllById(List.of(100L))).thenReturn(List.of(variant));
    when(promotionVariantRepository.findConflictingPromotionVariants(
            any(), any(), isNull(), any(), any(), any()))
        .thenReturn(List.of());
    when(promotionRepository.save(any(Promotion.class)))
        .thenAnswer(
            inv -> {
              Promotion p = inv.getArgument(0);
              p.setPromotionId(9L);
              return p;
            });

    CreatePromotionRequest req =
        CreatePromotionRequest.builder()
            .name("Sale Sau Khi Het Han")
            .startAt(LocalDateTime.now().plusDays(10).toString())
            .endAt(LocalDateTime.now().plusDays(12).toString())
            .status("UPCOMING")
            .variants(
                List.of(
                    PromotionVariantItemRequest.builder()
                        .variantId(100L)
                        .promoPrice(BigDecimal.valueOf(25000000))
                        .build()))
            .build();

    CreatePromotionResponse res = promotionsService.createPromotion(req);
    assertThat(res.getPromotionId()).isEqualTo(9L);
    assertThat(res.getAppliedVariantsCount()).isEqualTo(1);
  }
}
