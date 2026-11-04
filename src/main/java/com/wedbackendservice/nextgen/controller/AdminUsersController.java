package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.response.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.PageResponse;
import com.wedbackendservice.nextgen.dto.response.admin.*;
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
     * API 2.8: Quản lý danh sách người dùng trong hệ thống (lọc RBAC, trạng thái, tìm kiếm, phân trang)
     */
    @GetMapping
    public ApiResponse<PageResponse<AdminUserItemResponse>> getUsersList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        int effectiveLimit = limit != null ? limit : (size != null ? size : 15);
        String effectiveSearch = search != null ? search : keyword;

        PageResponse<AdminUserItemResponse> data = adminUsersService.getUsersList(
                page, effectiveLimit, effectiveSearch, role, status, sortBy, sortDir);

        return ApiResponse.<PageResponse<AdminUserItemResponse>>builder()
                .success(true)
                .statusCode(200)
                .message("Lấy danh sách người dùng thành công!")
                .data(data)
                .build();
    }

    /**
     * API 2.9: Xem hồ sơ toàn diện của một tài khoản (Profile, Addresses, ShoppingSummary, RecentOrders)
     */
    @GetMapping("/{userId}")
    public ApiResponse<AdminUserDetailResponse> getUserDetail(
            @PathVariable Long userId) {
        return ApiResponse.<AdminUserDetailResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Lấy chi tiết tài khoản thành công!")
                .data(adminUsersService.getUserDetail(userId))
                .build();
    }

    /**
     * API 2.10: Admin tạo tài khoản nội bộ trực tiếp cho nhân viên (STAFF) hoặc ADMIN
     */
    @PostMapping("/internal")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateInternalUserResponse> createInternalUser(
            @Valid @RequestBody CreateInternalUserRequest request) {
        CreateInternalUserResponse response = adminUsersService.createInternalUser(request);
        return ApiResponse.<CreateInternalUserResponse>builder()
                .success(true)
                .statusCode(201)
                .message("Cấp tài khoản nhân viên nội bộ thành công!")
                .data(response)
                .build();
    }

    /**
     * API 2.11: Khóa / Mở khóa tài khoản (kèm lý do lockReason, thu hồi JWT Session)
     */
    @PatchMapping("/{userId}/status")
    public ApiResponse<UpdateUserStatusResponse> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        UpdateUserStatusResponse response = adminUsersService.updateUserStatus(userId, request);
        return ApiResponse.<UpdateUserStatusResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Cập nhật trạng thái tài khoản thành công!")
                .data(response)
                .build();
    }

    /**
     * API 2.12: Thay đổi phân quyền RBAC của tài khoản người dùng
     */
    @PatchMapping("/{userId}/role")
    public ApiResponse<UpdateUserRoleResponse> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        UpdateUserRoleResponse response = adminUsersService.updateUserRole(userId, request);
        return ApiResponse.<UpdateUserRoleResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Thay đổi quyền hạn tài khoản thành công!")
                .data(response)
                .build();
    }
}
