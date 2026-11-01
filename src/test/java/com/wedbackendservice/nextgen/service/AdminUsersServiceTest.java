package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.security.SecurityHelper;


import com.wedbackendservice.nextgen.dto.response.admin.AdminUserDetailResponse;
import com.wedbackendservice.nextgen.dto.response.admin.AdminUserListResponse;
import com.wedbackendservice.nextgen.dto.request.admin.CreateInternalUserRequest;
import com.wedbackendservice.nextgen.dto.request.admin.UpdateUserRoleRequest;
import com.wedbackendservice.nextgen.dto.request.admin.UpdateUserStatusRequest;
import com.wedbackendservice.nextgen.entity.AuditLog;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.enums.UserStatus;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.repository.OrderRepository;
import com.wedbackendservice.nextgen.repository.UserAddressRepository;
import com.wedbackendservice.nextgen.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminUsersServiceTest {

    @Mock
    private SecurityHelper securityHelper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserAddressRepository userAddressRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private TokenRedisService tokenRedisService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUsersService adminUsersService;

    private User adminUser;
    private User targetUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .userId(1L)
                .fullName("Quản Trị Viên")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        targetUser = User.builder()
                .userId(101L)
                .fullName("Trần Thị Thu Huyền")
                .email("thuhuyen.tran@gmail.com")
                .phone("0987654321")
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testGetUsersList_Success() {
        when(securityHelper.authenticateAdmin("Bearer admin-token")).thenReturn(adminUser);

        Page<User> page = new PageImpl<>(List.of(targetUser));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        AdminUserListResponse resp = adminUsersService.getUsersList("Bearer admin-token", 1, 10, null, "ALL", "ALL");

        assertNotNull(resp);
        assertEquals(1, resp.getItems().size());
        assertEquals(101L, resp.getItems().get(0).getUserId());
        assertEquals(1, resp.getPagination().getTotalItems());
    }

    @Test
    void testGetUserDetail_Success() {
        when(securityHelper.authenticateAdmin("Bearer admin-token")).thenReturn(adminUser);
        when(userRepository.findById(101L)).thenReturn(Optional.of(targetUser));
        when(userAddressRepository.findByUserIdOrderByIsDefaultDescAddressIdDesc(101L)).thenReturn(List.of());
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(101L)).thenReturn(List.of());
        when(auditLogRepository.findByUserIdOrderByCreatedAtDesc(101L)).thenReturn(List.of());

        AdminUserDetailResponse resp = adminUsersService.getUserDetail("Bearer admin-token", 101L);

        assertNotNull(resp);
        assertEquals(101L, resp.getUserId());
        assertEquals("Trần Thị Thu Huyền", resp.getFullName());
    }

    @Test
    void testCreateInternalUser_Success() {
        when(securityHelper.authenticateAdmin("Bearer admin-token")).thenReturn(adminUser);
        when(userRepository.countByEmailOrPhone("kho.le@nextpick.vn", "0934567890")).thenReturn(0L);
        when(passwordEncoder.encode("StaffPassword@123")).thenReturn("hashedPassword");

        User savedUser = User.builder()
                .userId(102L)
                .fullName("Lê Văn Kho")
                .email("kho.le@nextpick.vn")
                .phone("0934567890")
                .role(Role.STAFF)
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        CreateInternalUserRequest req = CreateInternalUserRequest.builder()
                .fullName("Lê Văn Kho")
                .email("kho.le@nextpick.vn")
                .phone("0934567890")
                .password("StaffPassword@123")
                .role(Role.STAFF)
                .build();

        adminUsersService.createInternalUser("Bearer admin-token", req);

        verify(userRepository, times(1)).save(any(User.class));
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void testUpdateUserStatus_Lock_Success() {
        when(securityHelper.authenticateAdmin("Bearer admin-token")).thenReturn(adminUser);
        when(userRepository.findById(101L)).thenReturn(Optional.of(targetUser));

        UpdateUserStatusRequest req = UpdateUserStatusRequest.builder()
                .status(UserStatus.LOCKED)
                .lockReason("Bom hàng quá 3 lần")
                .build();

        adminUsersService.updateUserStatus("Bearer admin-token", 101L, req);

        assertEquals(UserStatus.LOCKED, targetUser.getStatus());
        assertEquals("Bom hàng quá 3 lần", targetUser.getLockReason());
        verify(userRepository, times(1)).save(targetUser);
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
        verify(tokenRedisService, times(1)).deleteToken(101L);
    }

    @Test
    void testUpdateUserStatus_LockWithoutReason_ThrowsException() {
        when(securityHelper.authenticateAdmin("Bearer admin-token")).thenReturn(adminUser);
        when(userRepository.findById(101L)).thenReturn(Optional.of(targetUser));

        UpdateUserStatusRequest req = UpdateUserStatusRequest.builder()
                .status(UserStatus.LOCKED)
                .lockReason("")
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                adminUsersService.updateUserStatus("Bearer admin-token", 101L, req));
        assertEquals(ErrorCode.LOCK_REASON_REQUIRED, ex.getErrorCode());
    }

    @Test
    void testUpdateUserRole_Success() {
        when(securityHelper.authenticateAdmin("Bearer admin-token")).thenReturn(adminUser);
        when(userRepository.findById(101L)).thenReturn(Optional.of(targetUser));

        UpdateUserRoleRequest req = UpdateUserRoleRequest.builder()
                .role(Role.STAFF)
                .build();

        adminUsersService.updateUserRole("Bearer admin-token", 101L, req);

        assertEquals(Role.STAFF, targetUser.getRole());
        verify(userRepository, times(1)).save(targetUser);
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void testUpdateUserStatus_AdminSelfLockout_ThrowsException() {
        when(securityHelper.authenticateAdmin("Bearer admin-token")).thenReturn(adminUser);

        UpdateUserStatusRequest req = UpdateUserStatusRequest.builder()
                .status(UserStatus.LOCKED)
                .lockReason("Tự khóa")
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                adminUsersService.updateUserStatus("Bearer admin-token", adminUser.getUserId(), req));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    void testUpdateUserRole_AdminSelfDemote_ThrowsException() {
        when(securityHelper.authenticateAdmin("Bearer admin-token")).thenReturn(adminUser);

        UpdateUserRoleRequest req = UpdateUserRoleRequest.builder()
                .role(Role.STAFF)
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                adminUsersService.updateUserRole("Bearer admin-token", adminUser.getUserId(), req));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }
}
