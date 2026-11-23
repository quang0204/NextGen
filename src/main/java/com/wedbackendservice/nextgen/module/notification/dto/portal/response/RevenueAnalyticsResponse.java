package com.wedbackendservice.nextgen.module.notification.dto.portal.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RevenueAnalyticsResponse {
  String period; // "DAY", "WEEK", "MONTH"
  BigDecimal totalRevenue;
  Long totalOrders;
  BigDecimal averageOrderValue;
  List<RevenueTimelinePoint> timeline;
  List<RevenueCategoryBreakdown> byCategory;
  List<RevenueBrandBreakdown> byBrand;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class RevenueTimelinePoint {
    String label;
    String date;
    BigDecimal revenue;
    Long orderCount;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class RevenueCategoryBreakdown {
    Long categoryId;
    String categoryName;
    BigDecimal revenue;
    Long unitsSold;
    Double percentage;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class RevenueBrandBreakdown {
    Long brandId;
    String brandName;
    BigDecimal revenue;
    Long unitsSold;
    Double percentage;
  }
}
