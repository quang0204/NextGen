package com.wedbackendservice.nextgen.controller.client;

import com.wedbackendservice.nextgen.dto.client.request.user.UpdateUserProfileRequest;
import com.wedbackendservice.nextgen.dto.client.request.user.UserAddressRequest;
import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.client.response.user.*;
import com.wedbackendservice.nextgen.service.client.UsersService;
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
        return ApiResponse.ok("Lấy thông tin tài khoản thành công!", usersService.getUserProfile());
    }

    /**
     * API 2.2: Cập nhật thông tin cá nhân
     */
    @PutMapping("/profile")
    public ApiResponse<UserProfileResponse> updateUserProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
        return ApiResponse.ok("Cập nhật hồ sơ cá nhân thành công!", usersService.updateUserProfile(request));
    }

    /**
     * API 2.3: Danh sách địa chỉ nhận hàng
     */
    @GetMapping("/addresses")
    public ApiResponse<List<UserAddressItemResponse>> getUserAddresses() {
        return ApiResponse.ok("Lấy sổ địa chỉ thành công!", usersService.getUserAddresses());
    }

    /**
     * API 2.4: Thêm mới 1 địa chỉ giao hàng
     */
    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateAddressResponse> createUserAddress(@Valid @RequestBody UserAddressRequest request) {
        return ApiResponse.created("Thêm địa chỉ giao hàng mới thành công!", usersService.createUserAddress(request));
    }

    /**
     * API 2.5: Sửa địa chỉ giao hàng
     */
    @PutMapping("/addresses/{addressId}")
    public ApiResponse<UserAddressItemResponse> updateUserAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody UserAddressRequest request) {
        return ApiResponse.ok("Cập nhật địa chỉ nhận hàng thành công!", usersService.updateUserAddress(addressId, request));
    }

    /**
     * API 2.6: Đặt địa chỉ làm mặc định
     */
    @PatchMapping("/addresses/{addressId}/default")
    public ApiResponse<SetDefaultAddressResponse> setDefaultAddress(@PathVariable Long addressId) {
        return ApiResponse.ok("Đã đặt địa chỉ ID " + addressId + " làm mặc định thành công!", usersService.setDefaultAddress(addressId));
    }

    /**
     * API 2.7: Xóa địa chỉ khỏi sổ địa chỉ
     */
    @DeleteMapping("/addresses/{addressId}")
    public ApiResponse<DeleteAddressResponse> deleteUserAddress(@PathVariable Long addressId) {
        return ApiResponse.ok("Đã xóa địa chỉ nhận hàng thành công!", usersService.deleteUserAddress(addressId));
    }
}
