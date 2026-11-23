package com.wedbackendservice.nextgen.module.notification.controller.portal;

import com.wedbackendservice.nextgen.core.dto.ApiResponse;
import com.wedbackendservice.nextgen.module.notification.dto.portal.response.RevenueAnalyticsResponse;
import com.wedbackendservice.nextgen.module.notification.dto.portal.response.SlowMovingReportResponse;
import com.wedbackendservice.nextgen.module.notification.dto.portal.response.TopSellingItemResponse;
import com.wedbackendservice.nextgen.module.notification.service.portal.AdminReportsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
public class AdminReportsController {

  private final AdminReportsService reportsService;

  /**
   * BÁO CÁO 1: [GET] /api/v1/admin/reports/top-selling Danh sách Top 10 sản phẩm/biến thể bán chạy
   * nhất và mang lại doanh thu cao nhất
   *
   * @param metric "REVENUE" (Doanh thu) hoặc "UNITS" (Số lượng)
   * @param scope "VARIANT" (Biến thể SKU) hoặc "PRODUCT" (Sản phẩm cha)
   * @param limit Số lượng (mặc định 10)
   * @param timeRange "ALL", "MONTH", "7DAYS"
   */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @GetMapping("/top-selling")
  public ApiResponse<List<TopSellingItemResponse>> getTopSelling(
      @RequestParam(defaultValue = "REVENUE") String metric,
      @RequestParam(defaultValue = "VARIANT") String scope,
      @RequestParam(defaultValue = "10") int limit,
      @RequestParam(defaultValue = "ALL") String timeRange) {
    List<TopSellingItemResponse> result =
        reportsService.getTopSelling(metric, scope, limit, timeRange);
    return ApiResponse.ok("Lấy báo cáo Top sản phẩm bán chạy thành công", result);
  }

  /**
   * BÁO CÁO 2: [GET] /api/v1/admin/reports/slow-moving Danh sách sản phẩm bán chậm, tồn kho lâu
   * ngày để có chính sách xả hàng
   *
   * @param minDays Số ngày tồn kho (mặc định 30)
   * @param maxUnitsSold Số lượng bán tối đa (mặc định 2)
   * @param keyword Từ khóa tìm kiếm (SKU hoặc tên máy)
   * @param severity Mức độ cảnh báo ("ALL", "CRITICAL", "WARNING")
   * @param sortBy Sắp xếp ("CAPITAL", "STOCK", "AGE")
   */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @GetMapping("/slow-moving")
  public ApiResponse<SlowMovingReportResponse> getSlowMoving(
      @RequestParam(defaultValue = "30") Integer minDays,
      @RequestParam(defaultValue = "2") Integer maxUnitsSold,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "ALL") String severity,
      @RequestParam(defaultValue = "CAPITAL") String sortBy) {
    SlowMovingReportResponse result =
        reportsService.getSlowMoving(minDays, maxUnitsSold, keyword, severity, sortBy);
    return ApiResponse.ok(
        "Lấy danh sách sản phẩm bán chậm và đề xuất chính sách xả kho thành công", result);
  }

  /**
   * BÁO CÁO 3: [GET] /api/v1/admin/reports/revenue-analytics
   * Thống kê biểu đồ doanh thu theo Ngày, Tuần, Tháng, theo Danh mục và Thương hiệu
   *
   * @param period "DAY", "WEEK", "MONTH" (mặc định "DAY")
   */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @GetMapping("/revenue-analytics")
  public ApiResponse<RevenueAnalyticsResponse> getRevenueAnalytics(
      @RequestParam(defaultValue = "DAY") String period) {
    RevenueAnalyticsResponse result = reportsService.getRevenueAnalytics(period);
    return ApiResponse.ok("Lấy phân tích biểu đồ doanh thu thành công", result);
  }
}
