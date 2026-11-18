package com.wedbackendservice.nextgen.module.product.controller.portal;

import com.wedbackendservice.nextgen.core.dto.ApiResponse;
import com.wedbackendservice.nextgen.module.product.dto.client.response.ProductListResponse;
import com.wedbackendservice.nextgen.module.product.dto.portal.request.CreateProductRequest;
import com.wedbackendservice.nextgen.module.product.dto.portal.request.CreateVariantRequest;
import com.wedbackendservice.nextgen.module.product.dto.portal.request.UpdateProductRequest;
import com.wedbackendservice.nextgen.module.product.dto.portal.request.UpdateProductStatusRequest;
import com.wedbackendservice.nextgen.module.product.dto.portal.request.UpdateVariantRequest;
import com.wedbackendservice.nextgen.module.product.dto.portal.response.AdminProductDetailResponse;
import com.wedbackendservice.nextgen.module.product.dto.portal.response.CreateProductResponse;
import com.wedbackendservice.nextgen.module.product.dto.portal.response.UpdateProductResponse;
import com.wedbackendservice.nextgen.module.product.dto.portal.response.UpdateProductStatusResponse;
import com.wedbackendservice.nextgen.module.product.dto.portal.response.UpdateVariantResponse;
import com.wedbackendservice.nextgen.module.product.dto.portal.response.VariantCreatedResponse;
import com.wedbackendservice.nextgen.module.product.service.ProductsService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class AdminProductsController {

  private final ProductsService productsService;

  /**
   * API 4.10: Quản trị danh sách sản phẩm (STAFF / ADMIN) Lấy toàn bộ sản phẩm (kể cả ACTIVE và
   * INACTIVE) cùng đầy đủ ma trận biến thể
   */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @GetMapping
  public ApiResponse<ProductListResponse> getAdminProducts(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "50") int limit,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Long brandId,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) BigDecimal minPrice,
      @RequestParam(required = false) BigDecimal maxPrice,
      @RequestParam(required = false) Integer ram,
      @RequestParam(required = false) Integer rom,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "newest") String sortBy) {
    return ApiResponse.ok(
        "Lấy danh sách sản phẩm quản trị thành công!",
        productsService.getAdminProducts(
            page,
            limit,
            search,
            brandId,
            categoryId,
            minPrice,
            maxPrice,
            ram,
            rom,
            status,
            sortBy));
  }

  /**
   * API 4.4: Lấy chi tiết sản phẩm đầy đủ cho Admin (STAFF / ADMIN) bất kể trạng thái ACTIVE hay
   * INACTIVE
   */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @GetMapping("/{id}")
  public ApiResponse<AdminProductDetailResponse> getAdminProductDetail(@PathVariable Long id) {
    return ApiResponse.ok(
        "Lấy chi tiết sản phẩm quản trị thành công!", productsService.getAdminProductDetail(id));
  }

  /** API 4.5: Khai báo sản phẩm mới hoàn chỉnh theo luồng Master-Detail (STAFF / ADMIN) */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<CreateProductResponse> createProduct(
      @Valid @RequestBody CreateProductRequest request) {
    return ApiResponse.created(
        "Khai báo sản phẩm mới và khởi tạo tồn kho thành công!",
        productsService.createProduct(request));
  }

  /** API 4.6: Cập nhật thông tin chung của sản phẩm & thông số kỹ thuật (STAFF / ADMIN) */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @PutMapping("/{id}")
  public ApiResponse<UpdateProductResponse> updateProduct(
      @PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
    return ApiResponse.ok(
        "Cập nhật thông tin sản phẩm và thông số kỹ thuật thành công!",
        productsService.updateProduct(id, request));
  }

  /** API 4.7: Bổ sung thêm biến thể SKU mới cho sản phẩm đã có (STAFF / ADMIN) */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @PostMapping("/{id}/variants")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<VariantCreatedResponse> createVariant(
      @PathVariable Long id, @Valid @RequestBody CreateVariantRequest request) {
    return ApiResponse.created(
        "Thêm biến thể SKU mới thành công!", productsService.createVariant(id, request));
  }

  /** API 4.8: Điều chỉnh giá niêm yết bán lẻ hoặc bật/tắt SKU (STAFF / ADMIN) */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @PutMapping("/variants/{varId}")
  public ApiResponse<UpdateVariantResponse> updateVariant(
      @PathVariable Long varId, @Valid @RequestBody UpdateVariantRequest request) {
    return ApiResponse.ok(
        "Cập nhật thông tin biến thể SKU thành công!",
        productsService.updateVariant(varId, request));
  }

  /** API 4.9: Bật/Tắt trạng thái kinh doanh của sản phẩm (STAFF / ADMIN) */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @PatchMapping("/{id}/status")
  public ApiResponse<UpdateProductStatusResponse> updateProductStatus(
      @PathVariable Long id, @Valid @RequestBody UpdateProductStatusRequest request) {
    return ApiResponse.ok(
        "Cập nhật trạng thái kinh doanh của sản phẩm thành công!",
        productsService.updateProductStatus(id, request));
  }

  /** Xóa ảnh sản phẩm (STAFF / ADMIN) và tự động xóa object trên MinIO sau khi DB commit */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @DeleteMapping("/{productId}/images/{imageId}")
  public ApiResponse<Void> deleteProductImage(
      @PathVariable Long productId, @PathVariable Long imageId) {
    productsService.deleteProductImage(productId, imageId);
    return ApiResponse.ok("Xóa ảnh sản phẩm thành công!", null);
  }
}
