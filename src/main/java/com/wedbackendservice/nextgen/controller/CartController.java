package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.request.cart.CartItemAddRequest;
import com.wedbackendservice.nextgen.dto.request.cart.CartItemUpdateRequest;
import com.wedbackendservice.nextgen.dto.request.cart.CartSyncRequest;
import com.wedbackendservice.nextgen.dto.response.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.cart.*;
import com.wedbackendservice.nextgen.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
        return ApiResponse.ok("Lấy thông tin giỏ hàng thành công", cartService.getCart());
    }

    /**
     * API 7.2: Thêm sản phẩm biến thể (SKU) vào giỏ hàng (Authenticated)
     */
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CartItemAddResponse> addItemToCart(@Valid @RequestBody CartItemAddRequest request) {
        return ApiResponse.created("Đã thêm sản phẩm vào giỏ hàng thành công!", cartService.addItemToCart(request));
    }

    /**
     * API 7.3: Thay đổi số lượng mua (+ / - / nhập số) (Authenticated)
     */
    @PutMapping("/items/{itemId}")
    public ApiResponse<CartItemUpdateResponse> updateItemQuantity(
            @PathVariable Long itemId,
            @Valid @RequestBody CartItemUpdateRequest request) {
        return ApiResponse.ok("Cập nhật số lượng thành công!", cartService.updateItemQuantity(itemId, request));
    }

    /**
     * API 7.4: Xóa 1 biến thể SKU khỏi giỏ hàng (Authenticated)
     */
    @DeleteMapping("/items/{itemId}")
    public ApiResponse<CartItemDeleteResponse> removeItemFromCart(@PathVariable Long itemId) {
        return ApiResponse.ok("Đã xóa sản phẩm khỏi giỏ hàng!", cartService.removeItemFromCart(itemId));
    }

    /**
     * API 7.5: Xóa sạch toàn bộ giỏ hàng (Authenticated)
     */
    @DeleteMapping("/clear")
    public ApiResponse<Void> clearCart() {
        cartService.clearCart();
        return ApiResponse.ok("Đã làm trống giỏ hàng thành công!", null);
    }

    /**
     * API 7.6: Đồng bộ giỏ hàng vãng lai (Guest) vào tài khoản khi Đăng nhập (Authenticated)
     */
    @PostMapping("/sync")
    public ApiResponse<CartSyncResponse> syncCart(@Valid @RequestBody CartSyncRequest request) {
        return ApiResponse.ok("Đồng bộ giỏ hàng thành công!", cartService.syncCart(request));
    }
}
