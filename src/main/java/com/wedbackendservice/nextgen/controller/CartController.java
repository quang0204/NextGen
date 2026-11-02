package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.response.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.cart.*;
import com.wedbackendservice.nextgen.dto.request.cart.CartItemAddRequest;
import com.wedbackendservice.nextgen.dto.request.cart.CartItemUpdateRequest;
import com.wedbackendservice.nextgen.dto.request.cart.CartSyncRequest;
import com.wedbackendservice.nextgen.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * API 7.1: Lấy thông tin giỏ hàng của người dùng (Authenticated)
     */
    @GetMapping
    public ApiResponse<CartResponse> getCart() {
        return ApiResponse.<CartResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Lấy thông tin giỏ hàng thành công")
                .data(cartService.getCart())
                .build();
    }

    /**
     * API 7.2: Thêm sản phẩm biến thể (SKU) vào giỏ hàng (Authenticated)
     */
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CartItemAddResponse> addItemToCart(
            @Valid @RequestBody CartItemAddRequest request) {
        return ApiResponse.<CartItemAddResponse>builder()
                .success(true)
                .statusCode(201)
                .message("Đã thêm sản phẩm vào giỏ hàng thành công!")
                .data(cartService.addItemToCart(request))
                .build();
    }

    /**
     * API 7.3: Thay đổi số lượng mua (+ / - / nhập số) (Authenticated)
     */
    @PutMapping("/items/{itemId}")
    public ApiResponse<CartItemUpdateResponse> updateItemQuantity(
            @PathVariable Long itemId,
            @Valid @RequestBody CartItemUpdateRequest request) {
        return ApiResponse.<CartItemUpdateResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Cập nhật số lượng thành công!")
                .data(cartService.updateItemQuantity(itemId, request))
                .build();
    }

    /**
     * API 7.4: Xóa 1 biến thể SKU khỏi giỏ hàng (Authenticated)
     */
    @DeleteMapping("/items/{itemId}")
    public ApiResponse<CartItemDeleteResponse> removeItemFromCart(
            @PathVariable Long itemId) {
        return ApiResponse.<CartItemDeleteResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Đã xóa sản phẩm khỏi giỏ hàng!")
                .data(cartService.removeItemFromCart(itemId))
                .build();
    }

    /**
     * API 7.5: Xóa sạch toàn bộ giỏ hàng (Authenticated)
     */
    @DeleteMapping("/clear")
    public ApiResponse<Void> clearCart() {
        cartService.clearCart();
        return ApiResponse.<Void>builder()
                .success(true)
                .statusCode(200)
                .message("Đã làm trống giỏ hàng thành công!")
                .build();
    }

    /**
     * API 7.6: Đồng bộ giỏ hàng vãng lai (Guest) vào tài khoản khi Đăng nhập (Authenticated)
     */
    @PostMapping("/sync")
    public ApiResponse<CartSyncResponse> syncCart(
            @Valid @RequestBody CartSyncRequest request) {
        return ApiResponse.<CartSyncResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Đồng bộ giỏ hàng thành công!")
                .data(cartService.syncCart(request))
                .build();
    }
}
