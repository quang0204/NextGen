package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.request.auth.ChangePasswordRequest;
import com.wedbackendservice.nextgen.dto.response.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.category.CategoryResponse;
import com.wedbackendservice.nextgen.dto.request.category.CategoryRequest;
import com.wedbackendservice.nextgen.service.CategoriesAndBrandsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
public class AdminCategoriesController {

    private final CategoriesAndBrandsService categoriesAndBrandsService;

    /**
     * API 3.3: Tạo mới danh mục sản phẩm (STAFF / ADMIN)
     */
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.<CategoryResponse>builder()
                .success(true)
                .statusCode(201)
                .message("Tạo danh mục mới thành công!")
                .data(categoriesAndBrandsService.createCategory(request))
                .build();
    }

    /**
     * API 3.4: Cập nhật danh mục (STAFF / ADMIN)
     */
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.<CategoryResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Cập nhật thông tin danh mục thành công!")
                .data(categoriesAndBrandsService.updateCategory(id, request))
                .build();
    }

    /**
     * API 3.5: Xóa danh mục (ADMIN)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCategory(
            @PathVariable Long id) {
        categoriesAndBrandsService.deleteCategory(id);
        return ApiResponse.<Void>builder()
                .success(true)
                .statusCode(200)
                .message("Xóa danh mục thành công!")
                .build();
    }
}
