package com.wedbackendservice.nextgen.controller.client;

import com.wedbackendservice.nextgen.dto.client.request.user.ProfileResendOtpRequest;
import com.wedbackendservice.nextgen.dto.client.request.user.ProfileUpdateConfirmRequest;
import com.wedbackendservice.nextgen.dto.client.request.user.ProfileUpdateRequest;
import com.wedbackendservice.nextgen.dto.client.request.user.UserAddressRequest;
import com.wedbackendservice.nextgen.dto.client.response.user.*;
import com.wedbackendservice.nextgen.dto.common.ApiResponse;
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
     * 1. POST /api/v1/users/profile/update-request
     * Bước 1 – Gửi thông tin cá nhân mới cần cập nhật, kiểm tra hợp lệ, phát sinh OTP & cấp sessionToken
     */
    @PostMapping("/profile/update-request")
    public ApiResponse<ProfileUpdateOtpResponse> requestProfileUpdate(@Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.ok("Thông tin hợp lệ. Mã xác thực OTP đã được gửi đến email của bạn để xác nhận cập nhật!",
                usersService.requestProfileUpdate(request));
    }

    /**
     * 2. POST /api/v1/users/profile/update-confirm
     * Bước 2 – Nhập mã OTP 6 số, đối chiếu khớp mã, lưu vào Users, ghi AuditLog & trả về profile mới
     */
    @PostMapping("/profile/update-confirm")
    public ApiResponse<UserProfileResponse> confirmProfileUpdate(@Valid @RequestBody ProfileUpdateConfirmRequest request) {
        return ApiResponse.ok("Xác thực OTP thành công. Thông tin cá nhân đã được cập nhật!",
                usersService.confirmProfileUpdate(request));
    }

    /**
     * 3. POST /api/v1/users/profile/resend-otp
     * Dành cho nút "Gửi lại mã OTP" khi đợi quá 60s
     */
    @PostMapping("/profile/resend-otp")
    public ApiResponse<ProfileUpdateOtpResponse> resendProfileUpdateOtp(@Valid @RequestBody ProfileResendOtpRequest request) {
        return ApiResponse.ok("Mã xác thực OTP mới đã được gửi lại vào email của bạn!",
                usersService.resendProfileUpdateOtp(request));
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
