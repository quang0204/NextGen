package com.wedbackendservice.nextgen.controller.client;

import com.wedbackendservice.nextgen.dto.client.response.product.*;
import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.service.shared.ProductsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductsController {

    private final ProductsService productsService;

    /**
     * API 4.1: Danh sách sản phẩm dành cho khách hàng với bộ lọc đa tiêu chí (Public)
     */
    @GetMapping
    public ApiResponse<ProductListResponse> getProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer ram,
            @RequestParam(required = false) Integer rom,
            @RequestParam(defaultValue = "newest") String sortBy) {
        return ApiResponse.ok("Lấy danh sách sản phẩm thành công!",
                productsService.getProducts(page, limit, search, brandId, categoryId, minPrice, maxPrice, ram, rom, sortBy));
    }

    /**
     * API 4.2: Danh sách sản phẩm nổi bật (Flash Sale hoặc Bán chạy nhất) (Public)
     */
    @GetMapping("/featured")
    public ApiResponse<List<ProductFeaturedResponse>> getFeaturedProducts(
            @RequestParam(defaultValue = "FLASH_SALE") String type,
            @RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.ok("Lấy danh sách sản phẩm nổi bật thành công!",
                productsService.getFeaturedProducts(type, limit));
    }

    /**
     * API 4.3: Xem chi tiết sản phẩm kèm specs, images và variants (Public)
     */
    @GetMapping("/{id}")
    public ApiResponse<ProductDetailResponse> getProductDetail(@PathVariable Long id) {
        return ApiResponse.ok("Lấy chi tiết sản phẩm thành công!",
                productsService.getProductDetail(id));
    }

    /**
     * API 4.4: Gợi ý các sản phẩm liên quan (Public)
     */
    @GetMapping("/{id}/related")
    public ApiResponse<List<ProductRelatedResponse>> getRelatedProducts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "4") int limit) {
        return ApiResponse.ok("Lấy sản phẩm liên quan thành công!",
                productsService.getRelatedProducts(id, limit));
    }
}
