package com.wedbackendservice.nextgen.controller.client;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.client.response.brand.BrandResponse;
import com.wedbackendservice.nextgen.service.shared.CategoriesAndBrandsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandsController {

    private final CategoriesAndBrandsService categoriesAndBrandsService;

    /**
     * API 3.6: Danh sách thương hiệu đối tác (Public, hỗ trợ includeHidden cho Staff)
     */
    @GetMapping
    public ApiResponse<List<BrandResponse>> getBrands(@RequestParam(defaultValue = "false") boolean includeHidden) {
        return ApiResponse.ok("Lấy danh sách thương hiệu thành công", categoriesAndBrandsService.getBrands(includeHidden));
    }

    /**
     * API 3.7: Chi tiết thương hiệu (Public)
     */
    @GetMapping("/{id}")
    public ApiResponse<BrandResponse> getBrandById(@PathVariable Long id) {
        return ApiResponse.ok(categoriesAndBrandsService.getBrandById(id));
    }
}
