package com.wedbackendservice.nextgen.controller.client;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.client.request.wishlist.WishlistToggleRequest;
import com.wedbackendservice.nextgen.dto.client.response.wishlist.WishlistListResponse;
import com.wedbackendservice.nextgen.dto.client.response.wishlist.WishlistToggleResponse;
import com.wedbackendservice.nextgen.service.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wishlists")
@RequiredArgsConstructor
public class WishlistsController {

    private final WishlistService wishlistService;

    /**
     * API 13.1: Danh sách sản phẩm yêu thích.
     */
    @GetMapping
    public ApiResponse<WishlistListResponse> getWishlist(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(wishlistService.getMyWishlist(page, size));
    }

    /**
     * API 13.2: Thêm hoặc bỏ yêu thích.
     */
    @PostMapping("/toggle")
    public ApiResponse<WishlistToggleResponse> toggle(
            @Valid @RequestBody WishlistToggleRequest request) {
        return ApiResponse.ok(wishlistService.toggle(request));
    }
}
