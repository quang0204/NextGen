package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.request.admin.CreateInternalUserRequest;
import com.wedbackendservice.nextgen.dto.request.admin.UpdateUserRoleRequest;
import com.wedbackendservice.nextgen.dto.request.admin.UpdateUserStatusRequest;
import com.wedbackendservice.nextgen.dto.response.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.PageResponse;
import com.wedbackendservice.nextgen.dto.response.admin.AdminUserDetailResponse;
import com.wedbackendservice.nextgen.dto.response.admin.AdminUserItemResponse;
import com.wedbackendservice.nextgen.dto.response.admin.CreateInternalUserResponse;
import com.wedbackendservice.nextgen.dto.response.admin.UpdateUserRoleResponse;
import com.wedbackendservice.nextgen.dto.response.admin.UpdateUserStatusResponse;
import com.wedbackendservice.nextgen.service.AdminUsersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

        return ApiResponse.ok("Lấy danh sách người dùng thành công!",
                adminUsersService.getUsersList(page, effectiveLimit, effectiveSearch, role, status, sortBy, sortDir));
    }

    /**
     * API 2.9: Xem hồ sơ toàn diện của một tài khoản (Profile, Addresses, ShoppingSummary, RecentOrders)
     */
    @GetMapping("/{userId}")
    public ApiResponse<AdminUserDetailResponse> getUserDetail(@PathVariable Long userId) {
        return ApiResponse.ok("Lấy chi tiết tài khoản thành công!", adminUsersService.getUserDetail(userId));
    }

    /**
     * API 2.10: Admin tạo tài khoản nội bộ trực tiếp cho nhân viên (STAFF) hoặc ADMIN
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/internal")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateInternalUserResponse> createInternalUser(@Valid @RequestBody CreateInternalUserRequest request) {
        return ApiResponse.created("Cấp tài khoản nhân viên nội bộ thành công!", adminUsersService.createInternalUser(request));
    }

    /**
     * API 2.11: Khóa / Mở khóa tài khoản (kèm lý do lockReason, thu hồi JWT Session)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}/status")
    public ApiResponse<UpdateUserStatusResponse> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        return ApiResponse.ok("Cập nhật trạng thái tài khoản thành công!", adminUsersService.updateUserStatus(userId, request));
    }

    /**
     * API 2.12: Thay đổi phân quyền RBAC của tài khoản người dùng
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}/role")
    public ApiResponse<UpdateUserRoleResponse> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        return ApiResponse.ok("Thay đổi quyền hạn tài khoản thành công!", adminUsersService.updateUserRole(userId, request));
    }
}
