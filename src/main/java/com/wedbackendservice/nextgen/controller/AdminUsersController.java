package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.response.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.admin.AdminUserDetailResponse;
import com.wedbackendservice.nextgen.dto.response.admin.AdminUserListResponse;
import com.wedbackendservice.nextgen.dto.request.admin.CreateInternalUserRequest;
import com.wedbackendservice.nextgen.dto.request.admin.UpdateUserRoleRequest;
import com.wedbackendservice.nextgen.dto.request.admin.UpdateUserStatusRequest;
import com.wedbackendservice.nextgen.service.AdminUsersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUsersController {

    private final AdminUsersService adminUsersService;

    /**
     * API 8: Danh sách người dùng (Tìm kiếm, phân trang, lọc theo Role & Status)
     */
    @GetMapping
    public ApiResponse<AdminUserListResponse> getUsersList(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        return ApiResponse.<AdminUserListResponse>builder()
                .success(true)
                .statusCode(200)
                .data(adminUsersService.getUsersList(authHeader, page, size, keyword, role, status))
                .build();
    }

    /**
     * API 9 (theo bảng tổng hợp): Xem chi tiết 1 người dùng kèm đơn hàng, địa chỉ, nhật ký
     */
    @GetMapping("/{userId}")
    public ApiResponse<AdminUserDetailResponse> getUserDetail(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long userId) {
        return ApiResponse.<AdminUserDetailResponse>builder()
                .success(true)
                .statusCode(200)
                .data(adminUsersService.getUserDetail(authHeader, userId))
                .build();
    }

    /**
     * API 10 (theo bảng) / API 9 (theo đặc tả): Cấp tài khoản nội bộ (STAFF hoặc ADMIN)
     */
    @PostMapping("/internal")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> createInternalUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody CreateInternalUserRequest request) {
        adminUsersService.createInternalUser(authHeader, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .statusCode(201)
                .message("Cấp tài khoản nội bộ thành công!")
                .build();
    }

    /**
     * API 11 (theo bảng & đặc tả): Phân quyền vai trò RBAC (CUSTOMER ↔ STAFF ↔ ADMIN)
     */
    @PatchMapping("/{userId}/role")
    public ApiResponse<Void> updateUserRole(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        adminUsersService.updateUserRole(authHeader, userId, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .statusCode(200)
                .message("Thay đổi quyền người dùng thành công!")
                .build();
    }

    /**
     * API 12 (theo bảng) / API 10 (theo đặc tả): Khóa / Mở khóa tài khoản
     */
    @PatchMapping("/{userId}/status")
    public ApiResponse<Void> updateUserStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        adminUsersService.updateUserStatus(authHeader, userId, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .statusCode(200)
                .message("Cập nhật trạng thái tài khoản thành công!")
                .build();
    }
}
