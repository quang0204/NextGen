package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.security.SecurityHelper;


import com.wedbackendservice.nextgen.dto.response.admin.AdminUserDetailResponse;
import com.wedbackendservice.nextgen.dto.response.admin.AdminUserItemResponse;
import com.wedbackendservice.nextgen.dto.response.admin.AdminUserListResponse;
import com.wedbackendservice.nextgen.dto.response.admin.PaginationResponse;
import com.wedbackendservice.nextgen.dto.request.admin.CreateInternalUserRequest;
import com.wedbackendservice.nextgen.dto.request.admin.UpdateUserRoleRequest;
import com.wedbackendservice.nextgen.dto.request.admin.UpdateUserStatusRequest;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.enums.UserStatus;
import com.wedbackendservice.nextgen.exception.GlobalExceptionHandler;
import com.wedbackendservice.nextgen.service.AdminUsersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class AdminUsersControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminUsersService adminUsersService;

    @InjectMocks
    private AdminUsersController adminUsersController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminUsersController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetUsersList_Success() throws Exception {
        AdminUserItemResponse item = AdminUserItemResponse.builder()
                .userId(101L)
                .fullName("Trần Thị Thu Huyền")
                .email("thuhuyen.tran@gmail.com")
                .phone("0987654321")
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        PaginationResponse pagination = PaginationResponse.builder()
                .page(1)
                .size(10)
                .totalItems(1)
                .totalPages(1)
                .build();

        AdminUserListResponse listResp = AdminUserListResponse.builder()
                .items(List.of(item))
                .pagination(pagination)
                .build();

        when(adminUsersService.getUsersList(eq("Bearer admin-token"), eq(1), eq(10), any(), any(), any()))
                .thenReturn(listResp);

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].userId").value(101))
                .andExpect(jsonPath("$.data.pagination.totalItems").value(1));
    }

    @Test
    void testGetUserDetail_Success() throws Exception {
        AdminUserDetailResponse detail = AdminUserDetailResponse.builder()
                .userId(101L)
                .fullName("Trần Thị Thu Huyền")
                .email("thuhuyen.tran@gmail.com")
                .phone("0987654321")
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        when(adminUsersService.getUserDetail("Bearer admin-token", 101L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/admin/users/101")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(101));
    }

    @Test
    void testCreateInternalUser_Success() throws Exception {
        CreateInternalUserRequest req = CreateInternalUserRequest.builder()
                .fullName("Lê Văn Kho")
                .email("kho.le@nextpick.vn")
                .phone("0934567890")
                .password("StaffPassword@123")
                .role(Role.STAFF)
                .build();

        doNothing().when(adminUsersService).createInternalUser(eq("Bearer admin-token"), any(CreateInternalUserRequest.class));

        mockMvc.perform(post("/api/v1/admin/users/internal")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("Cấp tài khoản nội bộ thành công!"));
    }

    @Test
    void testUpdateUserStatus_Success() throws Exception {
        UpdateUserStatusRequest req = UpdateUserStatusRequest.builder()
                .status(UserStatus.LOCKED)
                .lockReason("Bom hàng quá 3 lần")
                .build();

        doNothing().when(adminUsersService).updateUserStatus(eq("Bearer admin-token"), eq(101L), any(UpdateUserStatusRequest.class));

        mockMvc.perform(patch("/api/v1/admin/users/101/status")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cập nhật trạng thái tài khoản thành công!"));
    }

    @Test
    void testUpdateUserRole_Success() throws Exception {
        UpdateUserRoleRequest req = UpdateUserRoleRequest.builder()
                .role(Role.STAFF)
                .build();

        doNothing().when(adminUsersService).updateUserRole(eq("Bearer admin-token"), eq(101L), any(UpdateUserRoleRequest.class));

        mockMvc.perform(patch("/api/v1/admin/users/101/role")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Thay đổi quyền người dùng thành công!"));
    }
}
