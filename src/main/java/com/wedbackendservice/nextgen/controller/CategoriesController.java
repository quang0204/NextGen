package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.response.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.category.CategoryResponse;
import com.wedbackendservice.nextgen.service.CategoriesAndBrandsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoriesController {

    private final CategoriesAndBrandsService categoriesAndBrandsService;

    /**
     * API 3.1: Lấy cây danh mục phân cấp cha/con (Public)
     */
    @GetMapping
    public ApiResponse<List<CategoryResponse>> getCategoriesTree(
            @RequestParam(defaultValue = "false") boolean includeHidden) {
        return ApiResponse.<List<CategoryResponse>>builder()
                .success(true)
                .statusCode(200)
                .message("Lấy danh sách danh mục thành công")
                .data(categoriesAndBrandsService.getCategoriesTree(includeHidden))
                .timestamp(Instant.now().toString())
                .build();
    }

    /**
     * API 3.2: Chi tiết 1 danh mục sản phẩm (Public)
     */
    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> getCategoryById(
            @PathVariable Long id) {
        return ApiResponse.<CategoryResponse>builder()
                .success(true)
                .statusCode(200)
                .data(categoriesAndBrandsService.getCategoryById(id))
                .build();
    }
}
