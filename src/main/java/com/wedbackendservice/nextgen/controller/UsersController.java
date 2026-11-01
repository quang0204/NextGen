package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.response.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.user.CreateAddressResponse;
import com.wedbackendservice.nextgen.dto.response.user.UserAddressItemResponse;
import com.wedbackendservice.nextgen.dto.response.user.UserProfileResponse;
import com.wedbackendservice.nextgen.dto.request.user.UpdateUserProfileRequest;
import com.wedbackendservice.nextgen.dto.request.user.UserAddressRequest;
import com.wedbackendservice.nextgen.service.UsersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UsersController {

    private final UsersService usersService;

    /**
     * API 1: Lấy thông tin hồ sơ cá nhân
     */
    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> getUserProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return ApiResponse.<UserProfileResponse>builder()
                .success(true)
                .statusCode(200)
                .data(usersService.getUserProfile(authHeader))
                .build();
    }

    /**
     * API 2: Cập nhật thông tin cá nhân
     */
    @PutMapping("/profile")
    public ApiResponse<Void> updateUserProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        usersService.updateUserProfile(authHeader, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .statusCode(200)
                .message("Cập nhật thông tin cá nhân thành công!")
                .build();
    }

    /**
     * API 3: Lấy danh sách sổ địa chỉ nhận hàng
     */
    @GetMapping("/addresses")
    public ApiResponse<List<UserAddressItemResponse>> getUserAddresses(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return ApiResponse.<List<UserAddressItemResponse>>builder()
                .success(true)
                .statusCode(200)
                .data(usersService.getUserAddresses(authHeader))
                .build();
    }

    /**
     * API 4: Thêm mới 1 địa chỉ giao hàng
     */
    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateAddressResponse> createUserAddress(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody UserAddressRequest request) {
        CreateAddressResponse data = usersService.createUserAddress(authHeader, request);
        return ApiResponse.<CreateAddressResponse>builder()
                .success(true)
                .statusCode(201)
                .message("Thêm địa chỉ nhận hàng thành công!")
                .data(data)
                .build();
    }

    /**
     * API 5: Sửa địa chỉ giao hàng
     */
    @PutMapping("/addresses/{addressId}")
    public ApiResponse<Void> updateUserAddress(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long addressId,
            @Valid @RequestBody UserAddressRequest request) {
        usersService.updateUserAddress(authHeader, addressId, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .statusCode(200)
                .message("Cập nhật địa chỉ thành công!")
                .build();
    }

    /**
     * API 6: Đặt địa chỉ làm mặc định
     */
    @PatchMapping("/addresses/{addressId}/default")
    public ApiResponse<Void> setDefaultAddress(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long addressId) {
        usersService.setDefaultAddress(authHeader, addressId);
        return ApiResponse.<Void>builder()
                .success(true)
                .statusCode(200)
                .message("Đã đổi địa chỉ mặc định thành công!")
                .build();
    }

    /**
     * API 7: Xóa địa chỉ khỏi sổ địa chỉ
     */
    @DeleteMapping("/addresses/{addressId}")
    public ApiResponse<Void> deleteUserAddress(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long addressId) {
        usersService.deleteUserAddress(authHeader, addressId);
        return ApiResponse.<Void>builder()
                .success(true)
                .statusCode(200)
                .message("Xóa địa chỉ thành công!")
                .build();
    }
}
