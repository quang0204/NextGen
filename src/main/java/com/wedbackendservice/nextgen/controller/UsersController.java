package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.response.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.user.*;
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
     * API 2.1: Lấy thông tin hồ sơ cá nhân
     */
    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> getUserProfile() {
        return ApiResponse.<UserProfileResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Lấy thông tin tài khoản thành công!")
                .data(usersService.getUserProfile())
                .build();
    }

//    /**
//     * API gửi mã OTP xác nhận cập nhật thông tin email/sđt
//     */
//    @PostMapping("/profile/otp")
//    public ApiResponse<Void> sendProfileUpdateOtp(
//            @RequestHeader(value = "Authorization", required = false) String authHeader) {
//        usersService.sendProfileUpdateOtp();
//        return ApiResponse.<Void>builder()
//                .success(true)
//                .statusCode(200)
//                .message("Mã OTP xác thực đã được gửi về email của bạn.")
//                .build();
//    }

    /**
     * API 2.2: Cập nhật thông tin cá nhân
     */
    @PutMapping("/profile")
    public ApiResponse<UserProfileResponse> updateUserProfile(
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return ApiResponse.<UserProfileResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Cập nhật hồ sơ cá nhân thành công!")
                .data(usersService.updateUserProfile( request))
                .build();
    }

    /**
     * API 2.3: Danh sách địa chỉ nhận hàng
     */
    @GetMapping("/addresses")
    public ApiResponse<List<UserAddressItemResponse>> getUserAddresses() {
        return ApiResponse.<List<UserAddressItemResponse>>builder()
                .success(true)
                .statusCode(200)
                .message("Lấy sổ địa chỉ thành công!")
                .data(usersService.getUserAddresses())
                .build();
    }

    /**
     * API 2.4: Thêm mới 1 địa chỉ giao hàng
     */
    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateAddressResponse> createUserAddress(
            @Valid @RequestBody UserAddressRequest request) {
        return ApiResponse.<CreateAddressResponse>builder()
                .success(true)
                .statusCode(201)
                .message("Thêm địa chỉ giao hàng mới thành công!")
                .data(usersService.createUserAddress(request))
                .build();
    }

    /**
     * API 2.5: Sửa địa chỉ giao hàng
     */
    @PutMapping("/addresses/{addressId}")
    public ApiResponse<UserAddressItemResponse> updateUserAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody UserAddressRequest request) {
        return ApiResponse.<UserAddressItemResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Cập nhật địa chỉ nhận hàng thành công!")
                .data(usersService.updateUserAddress(addressId, request))
                .build();
    }

    /**
     * API 2.6: Đặt địa chỉ làm mặc định
     */
    @PatchMapping("/addresses/{addressId}/default")
    public ApiResponse<SetDefaultAddressResponse> setDefaultAddress(
            @PathVariable Long addressId) {
        return ApiResponse.<SetDefaultAddressResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Đã đặt địa chỉ ID " + addressId + " làm mặc định thành công!")
                .data(usersService.setDefaultAddress(addressId))
                .build();
    }

    /**
     * API 2.7: Xóa địa chỉ khỏi sổ địa chỉ
     */
    @DeleteMapping("/addresses/{addressId}")
    public ApiResponse<DeleteAddressResponse> deleteUserAddress(
            @PathVariable Long addressId) {
        return ApiResponse.<DeleteAddressResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Đã xóa địa chỉ nhận hàng thành công!")
                .data(usersService.deleteUserAddress(addressId))
                .build();
    }
}
