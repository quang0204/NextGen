package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.response.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.brand.BrandResponse;
import com.wedbackendservice.nextgen.service.CategoriesAndBrandsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandsController {

    private final CategoriesAndBrandsService categoriesAndBrandsService;

    /**
     * API 3.6: Danh sách thương hiệu đối tác (Public)
     */
    @GetMapping
    public ApiResponse<List<BrandResponse>> getBrands() {
        return ApiResponse.<List<BrandResponse>>builder()
                .success(true)
                .statusCode(200)
                .message("Lấy danh sách thương hiệu thành công")
                .data(categoriesAndBrandsService.getBrands())
                .build();
    }

    /**
     * API 3.7: Chi tiết thương hiệu (Public)
     */
    @GetMapping("/{id}")
    public ApiResponse<BrandResponse> getBrandById(@PathVariable Long id) {
        return ApiResponse.<BrandResponse>builder()
                .success(true)
                .statusCode(200)
                .data(categoriesAndBrandsService.getBrandById(id))
                .build();
    }
}
