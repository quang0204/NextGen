package com.wedbackendservice.nextgen.controller.client;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.client.response.category.CategoryResponse;
import com.wedbackendservice.nextgen.service.shared.CategoriesAndBrandsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public ApiResponse<List<CategoryResponse>> getCategoriesTree(@RequestParam(defaultValue = "false") boolean includeHidden) {
        return ApiResponse.ok("Lấy danh sách danh mục thành công", categoriesAndBrandsService.getCategoriesTree(includeHidden));
    }

    /**
     * API 3.2: Chi tiết 1 danh mục sản phẩm (Public)
     */
    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return ApiResponse.ok(categoriesAndBrandsService.getCategoryById(id));
    }
}
