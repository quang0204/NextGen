package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.response.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.brand.BrandResponse;
import com.wedbackendservice.nextgen.dto.request.brand.BrandRequest;
import com.wedbackendservice.nextgen.service.CategoriesAndBrandsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/brands")
@RequiredArgsConstructor
public class AdminBrandsController {

    private final CategoriesAndBrandsService categoriesAndBrandsService;

    /**
     * API 3.8: Thêm mới thương hiệu (STAFF / ADMIN)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BrandResponse> createBrand(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @Valid @RequestBody BrandRequest request) {
        return ApiResponse.<BrandResponse>builder()
                .success(true)
                .statusCode(201)
                .message("Thêm thương hiệu đối tác mới thành công!")
                .data(categoriesAndBrandsService.createBrand(authHeader, request))
                .build();
    }

    /**
     * API 3.9: Cập nhật thương hiệu (STAFF / ADMIN)
     */
    @PutMapping("/{id}")
    public ApiResponse<BrandResponse> updateBrand(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @PathVariable Long id,
            @Valid @RequestBody BrandRequest request) {
        return ApiResponse.<BrandResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Cập nhật thương hiệu thành công!")
                .data(categoriesAndBrandsService.updateBrand(authHeader, id, request))
                .build();
    }

    /**
     * API 3.10: Xóa thương hiệu (ADMIN)
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBrand(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @PathVariable Long id) {
        categoriesAndBrandsService.deleteBrand(authHeader, id);
        return ApiResponse.<Void>builder()
                .success(true)
                .statusCode(200)
                .message("Xóa thương hiệu thành công!")
                .build();
    }
}
