package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.request.brand.BrandRequest;
import com.wedbackendservice.nextgen.dto.response.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.brand.BrandResponse;
import com.wedbackendservice.nextgen.service.CategoriesAndBrandsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/brands")
@RequiredArgsConstructor
public class AdminBrandsController {

    private final CategoriesAndBrandsService categoriesAndBrandsService;

    /**
     * API 3.8: Thêm mới thương hiệu (STAFF / ADMIN)
     */
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BrandResponse> createBrand(@Valid @RequestBody BrandRequest request) {
        return ApiResponse.created("Thêm thương hiệu đối tác mới thành công!", categoriesAndBrandsService.createBrand(request));
    }

    /**
     * API 3.9: Cập nhật thương hiệu (STAFF / ADMIN)
     */
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<BrandResponse> updateBrand(@PathVariable Long id, @Valid @RequestBody BrandRequest request) {
        return ApiResponse.ok("Cập nhật thương hiệu thành công!", categoriesAndBrandsService.updateBrand(id, request));
    }

    /**
     * API 3.10: Xóa thương hiệu (ADMIN)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBrand(@PathVariable Long id) {
        categoriesAndBrandsService.deleteBrand(id);
        return ApiResponse.ok("Xóa thương hiệu thành công!", null);
    }
}
