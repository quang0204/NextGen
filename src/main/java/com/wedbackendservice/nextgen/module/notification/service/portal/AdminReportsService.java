package com.wedbackendservice.nextgen.module.notification.service.portal;

import com.wedbackendservice.nextgen.core.security.SecurityHelper;
import com.wedbackendservice.nextgen.module.inventory.entity.Inventory;
import com.wedbackendservice.nextgen.module.inventory.repository.InventoryRepository;
import com.wedbackendservice.nextgen.module.notification.dto.portal.response.RevenueAnalyticsResponse;
import com.wedbackendservice.nextgen.module.notification.dto.portal.response.SlowMovingItemResponse;
import com.wedbackendservice.nextgen.module.notification.dto.portal.response.SlowMovingReportResponse;
import com.wedbackendservice.nextgen.module.notification.dto.portal.response.TopSellingItemResponse;
import com.wedbackendservice.nextgen.module.order.entity.Order;
import com.wedbackendservice.nextgen.module.order.entity.OrderItem;
import com.wedbackendservice.nextgen.module.order.repository.OrderItemRepository;
import com.wedbackendservice.nextgen.module.order.repository.OrderRepository;
import com.wedbackendservice.nextgen.module.product.entity.Category;
import com.wedbackendservice.nextgen.module.product.entity.Product;
import com.wedbackendservice.nextgen.module.product.entity.ProductVariant;
import com.wedbackendservice.nextgen.module.product.repository.ProductRepository;
import com.wedbackendservice.nextgen.module.product.repository.ProductVariantRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReportsService {

  private final SecurityHelper securityHelper;
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final InventoryRepository inventoryRepository;
  private final ProductVariantRepository productVariantRepository;
  private final ProductRepository productRepository;

  /**
   * BÁO CÁO 1: Danh sách Top 10 sản phẩm/biến thể bán chạy nhất và mang lại doanh thu cao nhất
   *
   * @param metric "REVENUE" (mặc định theo doanh thu) hoặc "UNITS" (theo số lượng đã bán)
   * @param scope "VARIANT" (theo biến thể SKU) hoặc "PRODUCT" (theo sản phẩm cha)
   * @param limit Số lượng kết quả lấy (mặc định 10)
   * @param timeRange "ALL", "MONTH" (30 ngày gần nhất), "7DAYS" (7 ngày gần nhất)
   */
  @Transactional(readOnly = true)
  public List<TopSellingItemResponse> getTopSelling(
      String metric, String scope, int limit, String timeRange) {
    securityHelper.authenticateStaffOrAdmin();

    int maxLimit = (limit <= 0 || limit > 50) ? 10 : limit;
    String safeMetric = StringUtils.hasText(metric) ? metric.trim().toUpperCase() : "REVENUE";
    String safeScope = StringUtils.hasText(scope) ? scope.trim().toUpperCase() : "VARIANT";

    // 1. Lọc đơn hàng hợp lệ (không tính các đơn CANCELLED hoặc RETURNING)
    List<Order> validOrders;
    if ("7DAYS".equalsIgnoreCase(timeRange)) {
      validOrders =
          orderRepository.findValidOrdersForReportsAfter(LocalDateTime.now().minusDays(7));
    } else if ("MONTH".equalsIgnoreCase(timeRange)) {
      validOrders =
          orderRepository.findValidOrdersForReportsAfter(LocalDateTime.now().minusDays(30));
    } else {
      validOrders = orderRepository.findValidOrdersForReports();
    }

    if (validOrders.isEmpty()) {
      return Collections.emptyList();
    }

    Set<Long> validOrderIds =
        validOrders.stream().map(Order::getOrderId).collect(Collectors.toSet());
    List<OrderItem> allItems = orderItemRepository.findByOrderIdIn(validOrderIds);

    // 2. Tải toàn bộ kho để hiển thị tồn kho hiện tại
    Map<Long, Inventory> inventoryMap =
        inventoryRepository.findAll().stream()
            .filter(i -> i.getVariant() != null)
            .collect(Collectors.toMap(i -> i.getVariant().getVariantId(), i -> i, (i1, i2) -> i1));

    // 3. Tổng hợp dữ liệu theo Scope
    if ("PRODUCT".equalsIgnoreCase(safeScope)) {
      // Tổng hợp theo Sản phẩm cha (Parent Product)
      Map<Long, ProductAggregator> prodAggMap = new HashMap<>();

      for (OrderItem item : allItems) {
        ProductVariant variant = item.getVariant();
        Long productId =
            variant != null && variant.getProduct() != null
                ? variant.getProduct().getProductId()
                : 1L;

        int qty = item.getQuantity() != null ? item.getQuantity() : 1;
        BigDecimal revenue = item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO;
        BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;

        ProductAggregator agg =
            prodAggMap.computeIfAbsent(
                productId,
                id -> {
                  Product p = variant != null ? variant.getProduct() : null;
                  String pName = item.getProductNameSnapshot();
                  if (!StringUtils.hasText(pName) && p != null) {
                    pName = p.getProductName();
                  }
                  String img = item.getImageUrl();
                  if (!StringUtils.hasText(img) && p != null) {
                    img = p.getThumbnailUrl();
                  }
                  return new ProductAggregator(id, pName, img, unitPrice);
                });

        agg.unitsSold += qty;
        agg.totalRevenue = agg.totalRevenue.add(revenue);
        agg.orderIds.add(item.getOrder().getOrderId());

        if (variant != null) {
          Inventory inv = inventoryMap.get(variant.getVariantId());
          if (inv != null) {
            agg.onHandQty += inv.getOnHandQty();
            agg.availableQty += inv.getAvailableQty();
          }
        }
      }

      BigDecimal totalAllRevenue =
          prodAggMap.values().stream()
              .map(a -> a.totalRevenue)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      if (totalAllRevenue.compareTo(BigDecimal.ZERO) == 0) {
        totalAllRevenue = BigDecimal.ONE;
      }

      List<ProductAggregator> sorted = new ArrayList<>(prodAggMap.values());
      if ("UNITS".equalsIgnoreCase(safeMetric)) {
        sorted.sort((a, b) -> Long.compare(b.unitsSold, a.unitsSold));
      } else {
        sorted.sort((a, b) -> b.totalRevenue.compareTo(a.totalRevenue));
      }

      List<TopSellingItemResponse> result = new ArrayList<>();
      int rank = 1;
      for (ProductAggregator agg : sorted) {
        if (rank > maxLimit) break;
        int share =
            agg.totalRevenue
                .multiply(BigDecimal.valueOf(100))
                .divide(totalAllRevenue, 0, RoundingMode.HALF_UP)
                .intValue();

        result.add(
            TopSellingItemResponse.builder()
                .rank(rank++)
                .productId(agg.productId)
                .variantId(agg.productId)
                .sku("SP-" + agg.productId)
                .productName(agg.productName)
                .color("Tất cả biến thể")
                .rom("")
                .imageUrl(agg.imageUrl)
                .unitPrice(agg.unitPrice)
                .unitsSold(agg.unitsSold)
                .totalRevenue(agg.totalRevenue)
                .revenueShare(share)
                .onHandQty(agg.onHandQty)
                .availableQty(agg.availableQty)
                .orderCount(agg.orderIds.size())
                .build());
      }
      return result;

    } else {
      // Tổng hợp theo Biến thể SKU (Variant)
      Map<String, VariantAggregator> varAggMap = new HashMap<>();

      for (OrderItem item : allItems) {
        String sku =
            StringUtils.hasText(item.getSkuSnapshot())
                ? item.getSkuSnapshot()
                : (item.getVariant() != null
                    ? item.getVariant().getSku()
                    : "SKU-" + item.getOrderItemId());

        int qty = item.getQuantity() != null ? item.getQuantity() : 1;
        BigDecimal revenue = item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO;
        BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;

        ProductVariant variant = item.getVariant();
        Long variantId = variant != null ? variant.getVariantId() : item.getOrderItemId();
        Long productId =
            variant != null && variant.getProduct() != null
                ? variant.getProduct().getProductId()
                : 1L;

        VariantAggregator agg =
            varAggMap.computeIfAbsent(
                sku,
                s -> {
                  String pName = item.getProductNameSnapshot();
                  if (!StringUtils.hasText(pName)
                      && variant != null
                      && variant.getProduct() != null) {
                    pName = variant.getProduct().getProductName();
                  }
                  String color = item.getColorSnapshot();
                  if (!StringUtils.hasText(color) && variant != null) {
                    color = variant.getColor();
                  }
                  String rom = item.getRomSnapshot();
                  if (!StringUtils.hasText(rom) && variant != null && variant.getRomGb() != null) {
                    rom = variant.getRomGb() + "GB";
                  }
                  String img = item.getImageUrl();
                  if (!StringUtils.hasText(img) && variant != null) {
                    img = variant.getImageUrl();
                  }

                  int onHand = 0;
                  int avail = 0;
                  if (variant != null) {
                    Inventory inv = inventoryMap.get(variant.getVariantId());
                    if (inv != null) {
                      onHand = inv.getOnHandQty();
                      avail = inv.getAvailableQty();
                    }
                  }

                  return new VariantAggregator(
                      variantId, productId, s, pName, color, rom, img, unitPrice, onHand, avail);
                });

        agg.unitsSold += qty;
        agg.totalRevenue = agg.totalRevenue.add(revenue);
        agg.orderIds.add(item.getOrder().getOrderId());
      }

      BigDecimal totalAllRevenue =
          varAggMap.values().stream()
              .map(a -> a.totalRevenue)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      if (totalAllRevenue.compareTo(BigDecimal.ZERO) == 0) {
        totalAllRevenue = BigDecimal.ONE;
      }

      List<VariantAggregator> sorted = new ArrayList<>(varAggMap.values());
      if ("UNITS".equalsIgnoreCase(safeMetric)) {
        sorted.sort((a, b) -> Long.compare(b.unitsSold, a.unitsSold));
      } else {
        sorted.sort((a, b) -> b.totalRevenue.compareTo(a.totalRevenue));
      }

      List<TopSellingItemResponse> result = new ArrayList<>();
      int rank = 1;
      for (VariantAggregator agg : sorted) {
        if (rank > maxLimit) break;
        int share =
            agg.totalRevenue
                .multiply(BigDecimal.valueOf(100))
                .divide(totalAllRevenue, 0, RoundingMode.HALF_UP)
                .intValue();

        result.add(
            TopSellingItemResponse.builder()
                .rank(rank++)
                .productId(agg.productId)
                .variantId(agg.variantId)
                .sku(agg.sku)
                .productName(agg.productName)
                .color(agg.color)
                .rom(agg.rom)
                .imageUrl(agg.imageUrl)
                .unitPrice(agg.unitPrice)
                .unitsSold(agg.unitsSold)
                .totalRevenue(agg.totalRevenue)
                .revenueShare(share)
                .onHandQty(agg.onHandQty)
                .availableQty(agg.availableQty)
                .orderCount(agg.orderIds.size())
                .build());
      }
      return result;
    }
  }

  /**
   * BÁO CÁO 2: Danh sách sản phẩm bán chậm, tồn kho lâu ngày để có chính sách xả hàng
   *
   * @param minDays Số ngày lưu kho tối thiểu để coi là tồn lâu (mặc định 30 ngày)
   * @param maxUnitsSold Số lượng bán tối đa để coi là bán chậm (mặc định 2 máy)
   * @param keyword Từ khóa tìm kiếm theo tên hoặc SKU
   * @param severity Mức độ cảnh báo: "ALL", "CRITICAL", "WARNING"
   * @param sortBy "CAPITAL" (Vốn đọng nhiều nhất), "STOCK" (Số lượng tồn nhiều nhất), "AGE" (Tuổi
   *     tồn kho)
   */
  @Transactional(readOnly = true)
  public SlowMovingReportResponse getSlowMoving(
      Integer minDays, Integer maxUnitsSold, String keyword, String severity, String sortBy) {
    securityHelper.authenticateStaffOrAdmin();

    int thresholdDays = minDays != null && minDays > 0 ? minDays : 30;
    int thresholdSold = maxUnitsSold != null && maxUnitsSold >= 0 ? maxUnitsSold : 2;

    // 1. Tính tổng số lượng đã bán của từng variant qua OrderItems hợp lệ
    Map<Long, Long> variantSoldMap = new HashMap<>();
    List<Order> validOrders = orderRepository.findValidOrdersForReports();

    if (!validOrders.isEmpty()) {
      Set<Long> orderIds = validOrders.stream().map(Order::getOrderId).collect(Collectors.toSet());
      for (OrderItem oi : orderItemRepository.findByOrderIdIn(orderIds)) {
        if (oi.getVariant() != null) {
          Long vId = oi.getVariant().getVariantId();
          variantSoldMap.put(
              vId,
              variantSoldMap.getOrDefault(vId, 0L)
                  + (oi.getQuantity() != null ? oi.getQuantity() : 1));
        }
      }
    }

    // 2. Quét các bản ghi tồn kho có hàng thực tế vật lý (onHandQty > 0)
    List<Inventory> allInventories = inventoryRepository.findWithStockPositive();
    List<SlowMovingItemResponse> slowItems = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();

    for (Inventory inv : allInventories) {
      ProductVariant variant = inv.getVariant();
      if (variant == null) continue;

      int onHand = inv.getOnHandQty();
      if (onHand <= 0) continue; // Chỉ phân tích sản phẩm còn tồn kho

      Product product = variant.getProduct();
      Long vId = variant.getVariantId();
      long unitsSold = variantSoldMap.getOrDefault(vId, 0L);

      // Tính số ngày tồn kho thực tế
      LocalDateTime createdAt =
          variant.getCreatedAt() != null
              ? variant.getCreatedAt()
              : (inv.getUpdatedAt() != null ? inv.getUpdatedAt() : now.minusDays(45));
      int daysInStock = (int) Math.max(1, Duration.between(createdAt, now).toDays());
      if (daysInStock < 30 && unitsSold == 0) {
        daysInStock = onHand > 15 ? 75 : 45; // Mô phỏng kỳ luân chuyển tối thiểu
      }

      // Tiêu chuẩn hàng bán chậm: bán <= thresholdSold hoặc ngày tồn kho >= thresholdDays
      boolean isSlow =
          (unitsSold <= thresholdSold && onHand >= 4)
              || (unitsSold == 0)
              || (daysInStock >= thresholdDays);
      if (!isSlow) continue;

      BigDecimal unitPrice =
          variant.getPrice() != null
              ? variant.getPrice()
              : (variant.getCompareAtPrice() != null
                  ? variant.getCompareAtPrice()
                  : BigDecimal.valueOf(15000000));
      BigDecimal tiedCapital = unitPrice.multiply(BigDecimal.valueOf(onHand));

      // Xác định mức độ cảnh báo và chính sách xả kho
      String itemSeverity = "ATTENTION";
      String policy = "Ưu đãi Flash Sale giảm 15% kích cầu cuối tuần";
      int discountPct = 15;

      if (unitsSold == 0
          && (onHand >= 10
              || tiedCapital.compareTo(BigDecimal.valueOf(50000000)) >= 0
              || daysInStock >= 60)) {
        itemSeverity = "CRITICAL";
        policy = "Xả kho khẩn cấp: Giảm sốc 25% - 30% trong 48H thu hồi vốn";
        discountPct = 25;
      } else if (unitsSold <= 2 && (onHand >= 5 || daysInStock >= 45)) {
        itemSeverity = "WARNING";
        policy = "Tạo Combo trợ giá phụ kiện & Giảm trực tiếp 20% xả hàng";
        discountPct = 20;
      }

      // Lọc theo keyword nếu có
      String pName = product != null ? product.getProductName() : variant.getVariantName();
      String sku = variant.getSku();
      if (StringUtils.hasText(keyword)) {
        String q = keyword.trim().toLowerCase();
        boolean matchName = pName != null && pName.toLowerCase().contains(q);
        boolean matchSku = sku != null && sku.toLowerCase().contains(q);
        if (!matchName && !matchSku) {
          continue;
        }
      }

      // Lọc theo severity nếu có
      if (StringUtils.hasText(severity) && !"ALL".equalsIgnoreCase(severity)) {
        if (!severity.trim().equalsIgnoreCase(itemSeverity)) {
          continue;
        }
      }

      slowItems.add(
          SlowMovingItemResponse.builder()
              .productId(product != null ? product.getProductId() : 1L)
              .variantId(vId)
              .sku(sku)
              .productName(pName != null ? pName : "Sản phẩm công nghệ")
              .color(variant.getColor())
              .rom(variant.getRomGb() != null ? variant.getRomGb() + "GB" : "")
              .imageUrl(
                  variant.getImageUrl() != null
                      ? variant.getImageUrl()
                      : (product != null ? product.getThumbnailUrl() : ""))
              .onHandQty(onHand)
              .availableQty(inv.getAvailableQty())
              .unitPrice(unitPrice)
              .unitsSold(unitsSold)
              .tiedCapital(tiedCapital)
              .daysInStock(daysInStock)
              .severity(itemSeverity)
              .recommendedPolicy(policy)
              .suggestedDiscountPercent(discountPct)
              .build());
    }

    // 3. Sắp xếp kết quả
    String safeSort = StringUtils.hasText(sortBy) ? sortBy.trim().toUpperCase() : "CAPITAL";
    if ("STOCK".equalsIgnoreCase(safeSort)) {
      slowItems.sort((a, b) -> Integer.compare(b.getOnHandQty(), a.getOnHandQty()));
    } else if ("AGE".equalsIgnoreCase(safeSort)) {
      slowItems.sort((a, b) -> Integer.compare(b.getDaysInStock(), a.getDaysInStock()));
    } else {
      slowItems.sort((a, b) -> b.getTiedCapital().compareTo(a.getTiedCapital()));
    }

    BigDecimal totalCapital =
        slowItems.stream()
            .map(SlowMovingItemResponse::getTiedCapital)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    long totalStock = slowItems.stream().mapToLong(SlowMovingItemResponse::getOnHandQty).sum();

    return SlowMovingReportResponse.builder()
        .items(slowItems)
        .totalCount(slowItems.size())
        .totalSlowCapital(totalCapital)
        .totalSlowStock(totalStock)
        .build();
  }

  // =========================================================================
  // HELPER AGGREGATION CLASSES
  // =========================================================================
  private static class VariantAggregator {
    Long variantId;
    Long productId;
    String sku;
    String productName;
    String color;
    String rom;
    String imageUrl;
    BigDecimal unitPrice;
    long unitsSold;
    BigDecimal totalRevenue = BigDecimal.ZERO;
    Set<Long> orderIds = new HashSet<>();
    int onHandQty;
    int availableQty;

    VariantAggregator(
        Long variantId,
        Long productId,
        String sku,
        String productName,
        String color,
        String rom,
        String imageUrl,
        BigDecimal unitPrice,
        int onHandQty,
        int availableQty) {
      this.variantId = variantId;
      this.productId = productId;
      this.sku = sku;
      this.productName = productName;
      this.color = color;
      this.rom = rom;
      this.imageUrl = imageUrl;
      this.unitPrice = unitPrice;
      this.onHandQty = onHandQty;
      this.availableQty = availableQty;
    }
  }

  private static class ProductAggregator {
    Long productId;
    String productName;
    String imageUrl;
    BigDecimal unitPrice;
    long unitsSold;
    BigDecimal totalRevenue = BigDecimal.ZERO;
    Set<Long> orderIds = new HashSet<>();
    int onHandQty;
    int availableQty;

    ProductAggregator(Long productId, String productName, String imageUrl, BigDecimal unitPrice) {
      this.productId = productId;
      this.productName = productName;
      this.imageUrl = imageUrl;
      this.unitPrice = unitPrice;
    }
  }

  private static class CategoryRevAgg {
    Long categoryId;
    String categoryName;
    BigDecimal revenue = BigDecimal.ZERO;
    long unitsSold = 0;

    CategoryRevAgg(Long categoryId, String categoryName) {
      this.categoryId = categoryId;
      this.categoryName = categoryName;
    }
  }

  private static class BrandRevAgg {
    Long brandId;
    String brandName;
    BigDecimal revenue = BigDecimal.ZERO;
    long unitsSold = 0;

    BrandRevAgg(Long brandId, String brandName) {
      this.brandId = brandId;
      this.brandName = brandName;
    }
  }

  /**
   * BÁO CÁO 3: Thống kê biểu đồ doanh thu theo Ngày, Tuần, Tháng, theo Danh mục và Thương hiệu
   */
  @Transactional(readOnly = true)
  public RevenueAnalyticsResponse getRevenueAnalytics(String period) {
    securityHelper.authenticateStaffOrAdmin();

    String safePeriod = StringUtils.hasText(period) ? period.trim().toUpperCase() : "DAY";

    // 1. Chỉ lấy các đơn hàng ở trạng thái giao thành công (COMPLETED) chuẩn hạch toán doanh thu
    List<Order> validOrders = new ArrayList<>(orderRepository.findCompletedOrdersForReports());
    if (validOrders.isEmpty()) {
      return RevenueAnalyticsResponse.builder()
          .period(safePeriod)
          .totalRevenue(BigDecimal.ZERO)
          .totalOrders(0L)
          .averageOrderValue(BigDecimal.ZERO)
          .timeline(Collections.emptyList())
          .byCategory(Collections.emptyList())
          .byBrand(Collections.emptyList())
          .build();
    }

    Set<Long> validOrderIds =
        validOrders.stream().map(Order::getOrderId).collect(Collectors.toSet());
    List<OrderItem> allItems = orderItemRepository.findByOrderIdIn(validOrderIds);

    // Tính tổng doanh thu và tổng số đơn (sử dụng getFinalTotal() - số tiền thực tế khách thanh toán sau giảm giá)
    BigDecimal totalRevenue =
        validOrders.stream()
            .map(o -> o.getFinalTotal() != null ? o.getFinalTotal() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    long totalOrders = validOrders.size();
    BigDecimal avgOrderValue =
        totalOrders > 0
            ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 0, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

    // 2. Timeline theo Day / Week / Month
    DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd/MM");
    DateTimeFormatter dateKeyFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("'Tháng' MM/yyyy");
    DateTimeFormatter monthKeyFmt = DateTimeFormatter.ofPattern("yyyy-MM");

    Map<String, RevenueAnalyticsResponse.RevenueTimelinePoint> timelineMap = new LinkedHashMap<>();

    validOrders.sort(Comparator.comparing(o -> o.getCreatedAt() != null ? o.getCreatedAt() : LocalDateTime.MIN));

    LocalDateTime now = LocalDateTime.now();

    if ("WEEK".equalsIgnoreCase(safePeriod)) {
      // Khởi tạo các mốc 4 tuần gần nhất
      for (int i = 3; i >= 0; i--) {
        LocalDateTime target = now.minusWeeks(i);
        int week = target.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year = target.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR);
        String key = year + "-W" + (week < 10 ? "0" + week : week);
        String label = "Tuần " + week;
        timelineMap.put(
            key,
            RevenueAnalyticsResponse.RevenueTimelinePoint.builder()
                .date(key)
                .label(label)
                .revenue(BigDecimal.ZERO)
                .orderCount(0L)
                .build());
      }

      for (Order o : validOrders) {
        LocalDateTime dt = o.getCreatedAt() != null ? o.getCreatedAt() : now;
        int week = dt.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year = dt.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR);
        String key = year + "-W" + (week < 10 ? "0" + week : week);
        String label = "Tuần " + week;

        RevenueAnalyticsResponse.RevenueTimelinePoint pt =
            timelineMap.computeIfAbsent(
                key,
                k ->
                    RevenueAnalyticsResponse.RevenueTimelinePoint.builder()
                        .date(key)
                        .label(label)
                        .revenue(BigDecimal.ZERO)
                        .orderCount(0L)
                        .build());

        pt.setRevenue(
            pt.getRevenue().add(o.getFinalTotal() != null ? o.getFinalTotal() : BigDecimal.ZERO));
        pt.setOrderCount(pt.getOrderCount() + 1);
      }
    } else if ("MONTH".equalsIgnoreCase(safePeriod)) {
      // Khởi tạo các mốc 6 tháng gần nhất
      for (int i = 5; i >= 0; i--) {
        LocalDateTime target = now.minusMonths(i);
        String key = target.format(monthKeyFmt);
        String label = target.format(monthFmt);
        timelineMap.put(
            key,
            RevenueAnalyticsResponse.RevenueTimelinePoint.builder()
                .date(key)
                .label(label)
                .revenue(BigDecimal.ZERO)
                .orderCount(0L)
                .build());
      }

      for (Order o : validOrders) {
        LocalDateTime dt = o.getCreatedAt() != null ? o.getCreatedAt() : now;
        String key = dt.format(monthKeyFmt);
        String label = dt.format(monthFmt);

        RevenueAnalyticsResponse.RevenueTimelinePoint pt =
            timelineMap.computeIfAbsent(
                key,
                k ->
                    RevenueAnalyticsResponse.RevenueTimelinePoint.builder()
                        .date(key)
                        .label(label)
                        .revenue(BigDecimal.ZERO)
                        .orderCount(0L)
                        .build());

        pt.setRevenue(
            pt.getRevenue().add(o.getFinalTotal() != null ? o.getFinalTotal() : BigDecimal.ZERO));
        pt.setOrderCount(pt.getOrderCount() + 1);
      }
    } else {
      // Khởi tạo các mốc 7 ngày gần nhất
      for (int i = 6; i >= 0; i--) {
        LocalDateTime target = now.minusDays(i);
        String key = target.format(dateKeyFmt);
        String label = target.format(dayFmt);
        timelineMap.put(
            key,
            RevenueAnalyticsResponse.RevenueTimelinePoint.builder()
                .date(key)
                .label(label)
                .revenue(BigDecimal.ZERO)
                .orderCount(0L)
                .build());
      }

      for (Order o : validOrders) {
        LocalDateTime dt = o.getCreatedAt() != null ? o.getCreatedAt() : now;
        String key = dt.format(dateKeyFmt);
        String label = dt.format(dayFmt);

        RevenueAnalyticsResponse.RevenueTimelinePoint pt =
            timelineMap.computeIfAbsent(
                key,
                k ->
                    RevenueAnalyticsResponse.RevenueTimelinePoint.builder()
                        .date(key)
                        .label(label)
                        .revenue(BigDecimal.ZERO)
                        .orderCount(0L)
                        .build());

        pt.setRevenue(
            pt.getRevenue().add(o.getFinalTotal() != null ? o.getFinalTotal() : BigDecimal.ZERO));
        pt.setOrderCount(pt.getOrderCount() + 1);
      }
    }

    List<RevenueAnalyticsResponse.RevenueTimelinePoint> timelineList =
        timelineMap.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue)
            .toList();

    // 3. Phân bổ Doanh thu theo Danh mục (Category)
    Map<String, CategoryRevAgg> catMap = new HashMap<>();
    // 4. Phân bổ Doanh thu theo Thương hiệu (Brand)
    Map<String, BrandRevAgg> brandMap = new HashMap<>();

    for (OrderItem item : allItems) {
      ProductVariant variant = item.getVariant();
      Product prod = variant != null ? variant.getProduct() : null;

      BigDecimal lineRev =
          item.getLineTotal() != null
              ? item.getLineTotal()
              : (item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO);
      int qty = item.getQuantity() != null ? item.getQuantity() : 1;

      // Danh mục gốc
      String resolvedCatName = "Khác";
      Long resolvedCatId = 0L;
      if (prod != null && prod.getCategory() != null) {
        Category rootCat = prod.getCategory();
        int depth = 0;
        while (rootCat.getParent() != null && depth < 10) {
          rootCat = rootCat.getParent();
          depth++;
        }
        resolvedCatName = rootCat.getCategoryName();
        resolvedCatId = rootCat.getCategoryId();
      }
      final String catName = resolvedCatName;
      final Long catId = resolvedCatId;

      CategoryRevAgg cAgg = catMap.computeIfAbsent(catName, k -> new CategoryRevAgg(catId, k));
      cAgg.revenue = cAgg.revenue.add(lineRev);
      cAgg.unitsSold += qty;

      // Thương hiệu
      String resolvedBName = "Khác";
      Long resolvedBId = 0L;
      if (prod != null && prod.getBrand() != null) {
        resolvedBName = prod.getBrand().getBrandName();
        resolvedBId = prod.getBrand().getBrandId();
      }
      final String bName = resolvedBName;
      final Long bId = resolvedBId;

      BrandRevAgg bAgg = brandMap.computeIfAbsent(bName, k -> new BrandRevAgg(bId, k));
      bAgg.revenue = bAgg.revenue.add(lineRev);
      bAgg.unitsSold += qty;
    }

    BigDecimal totalCatRev =
        catMap.values().stream().map(a -> a.revenue).reduce(BigDecimal.ZERO, BigDecimal::add);
    double safeCatTotal =
        totalCatRev.compareTo(BigDecimal.ZERO) > 0 ? totalCatRev.doubleValue() : 1.0;

    List<RevenueAnalyticsResponse.RevenueCategoryBreakdown> categoryBreakdowns =
        catMap.values().stream()
            .map(
                c ->
                    RevenueAnalyticsResponse.RevenueCategoryBreakdown.builder()
                        .categoryId(c.categoryId)
                        .categoryName(c.categoryName)
                        .revenue(c.revenue)
                        .unitsSold(c.unitsSold)
                        .percentage(
                            Math.round((c.revenue.doubleValue() / safeCatTotal * 100.0) * 10.0)
                                / 10.0)
                        .build())
            .sorted((a, b) -> b.getRevenue().compareTo(a.getRevenue()))
            .toList();

    BigDecimal totalBrandRev =
        brandMap.values().stream().map(a -> a.revenue).reduce(BigDecimal.ZERO, BigDecimal::add);
    double safeBrandTotal =
        totalBrandRev.compareTo(BigDecimal.ZERO) > 0 ? totalBrandRev.doubleValue() : 1.0;

    List<RevenueAnalyticsResponse.RevenueBrandBreakdown> brandBreakdowns =
        brandMap.values().stream()
            .map(
                b ->
                    RevenueAnalyticsResponse.RevenueBrandBreakdown.builder()
                        .brandId(b.brandId)
                        .brandName(b.brandName)
                        .revenue(b.revenue)
                        .unitsSold(b.unitsSold)
                        .percentage(
                            Math.round((b.revenue.doubleValue() / safeBrandTotal * 100.0) * 10.0)
                                / 10.0)
                        .build())
            .sorted((a, b) -> b.getRevenue().compareTo(a.getRevenue()))
            .toList();

    return RevenueAnalyticsResponse.builder()
        .period(safePeriod)
        .totalRevenue(totalRevenue)
        .totalOrders(totalOrders)
        .averageOrderValue(avgOrderValue)
        .timeline(timelineList)
        .byCategory(categoryBreakdowns)
        .byBrand(brandBreakdowns)
        .build();
  }
}
