package com.wedbackendservice.nextgen.module.user.service.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.exception.ErrorCode;
import com.wedbackendservice.nextgen.core.security.SecurityHelper;
import com.wedbackendservice.nextgen.core.security.TokenRedisService;
import com.wedbackendservice.nextgen.module.notification.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.module.order.repository.OrderRepository;
import com.wedbackendservice.nextgen.module.user.controller.portal.AdminUsersController;
import com.wedbackendservice.nextgen.module.user.dto.portal.request.CreateInternalUserRequest;
import com.wedbackendservice.nextgen.module.user.dto.portal.request.UpdateUserRoleRequest;
import com.wedbackendservice.nextgen.module.user.dto.portal.request.UpdateUserStatusRequest;
import com.wedbackendservice.nextgen.module.user.dto.portal.response.AdminUserStatsResponse;
import com.wedbackendservice.nextgen.module.user.dto.portal.response.AdminUsersListResponse;
import com.wedbackendservice.nextgen.module.user.dto.portal.response.CreateInternalUserResponse;
import com.wedbackendservice.nextgen.module.user.entity.User;
import com.wedbackendservice.nextgen.module.user.enums.Role;
import com.wedbackendservice.nextgen.module.user.enums.UserStatus;
import com.wedbackendservice.nextgen.module.user.mapper.UsersMapper;
import com.wedbackendservice.nextgen.module.user.repository.UserAddressRepository;
import com.wedbackendservice.nextgen.module.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminUsersServiceRbacTest {

  @Mock private UserRepository userRepository;
  @Mock private UserAddressRepository userAddressRepository;
  @Mock private OrderRepository orderRepository;
  @Mock private AuditLogRepository auditLogRepository;
  @Mock private TokenRedisService tokenRedisService;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private UsersMapper usersMapper;

  private AdminUsersService adminUsersService;
  private User staffActor;
  private User adminActor;

  @BeforeEach
  void setUp() {
    SecurityHelper securityHelper = new SecurityHelper(userRepository);
    adminUsersService =
        new AdminUsersService(
            securityHelper,
            userRepository,
            userAddressRepository,
            orderRepository,
            auditLogRepository,
            tokenRedisService,
            passwordEncoder,
            usersMapper);

    staffActor =
        User.builder()
            .userId(2L)
            .fullName("QA Staff")
            .email("khoit670+qastaff@gmail.com")
            .role(Role.STAFF)
            .status(UserStatus.ACTIVE)
            .build();

    adminActor =
        User.builder()
            .userId(1L)
            .fullName("System Admin")
            .email("admin@nextgen.vn")
            .role(Role.ADMIN)
            .status(UserStatus.ACTIVE)
            .build();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void staffRole_IsDeniedAccessToAllAdminUsersOperations() {
    authenticateAs(staffActor);
    when(userRepository.findById(2L)).thenReturn(Optional.of(staffActor));

    CreateInternalUserRequest createReq =
        CreateInternalUserRequest.builder()
            .fullName("Staff 2")
            .email("staff2@nextgen.vn")
            .phone("0912345678")
            .password("Pass@1234")
            .role(Role.STAFF)
            .build();
    UpdateUserStatusRequest lockReq = new UpdateUserStatusRequest();
    lockReq.setStatus(UserStatus.LOCKED);
    lockReq.setLockReason("Spam");
    UpdateUserRoleRequest roleReq =
        UpdateUserRoleRequest.builder().role(Role.STAFF).reason("Test").build();

    assertForbidden(() -> adminUsersService.getUsersList(1, 10, null, null, null, null, null));
    assertForbidden(() -> adminUsersService.getUserStats());
    assertForbidden(() -> adminUsersService.getUserDetail(100L));
    assertForbidden(() -> adminUsersService.createInternalUser(createReq));
    assertForbidden(() -> adminUsersService.updateUserStatus(100L, lockReq));
    assertForbidden(() -> adminUsersService.updateUserRole(100L, roleReq));

    PreAuthorize classPreAuthorize = AdminUsersController.class.getAnnotation(PreAuthorize.class);
    assertThat(classPreAuthorize).isNotNull();
    assertThat(classPreAuthorize.value()).isEqualTo("hasRole('ADMIN')");
  }

  @Test
  void adminRole_CanAccessAdminUsersReadsAndCreateInternalUser() {
    authenticateAs(adminActor);
    when(userRepository.findById(1L)).thenReturn(Optional.of(adminActor));
    when(userRepository.searchUsers(any(), any(), any(), any())).thenReturn(Page.empty());
    when(userRepository.count()).thenReturn(5L);
    when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);
    when(userRepository.countByRole(Role.STAFF)).thenReturn(2L);
    when(userRepository.countByRole(Role.CUSTOMER)).thenReturn(2L);
    when(userRepository.countByStatus(UserStatus.LOCKED)).thenReturn(0L);

    AdminUsersListResponse listRes =
        adminUsersService.getUsersList(1, 10, null, null, null, null, null);
    AdminUserStatsResponse statsRes = adminUsersService.getUserStats();
    assertThat(listRes.getTotalUsers()).isEqualTo(5L);
    assertThat(statsRes.getTotalStaffs()).isEqualTo(2L);

    User createdUser =
        User.builder()
            .userId(300L)
            .fullName("Staff 2")
            .email("staff2@nextgen.vn")
            .phone("0912345678")
            .role(Role.STAFF)
            .status(UserStatus.ACTIVE)
            .build();
    when(userRepository.countByEmailOrPhone("staff2@nextgen.vn", "0912345678")).thenReturn(0L);
    when(passwordEncoder.encode("Pass@1234")).thenReturn("encoded");
    when(userRepository.save(any(User.class))).thenReturn(createdUser);
    when(usersMapper.toCreateInternalUserResponse(createdUser))
        .thenReturn(CreateInternalUserResponse.builder().userId(300L).role("STAFF").build());

    CreateInternalUserResponse createRes =
        adminUsersService.createInternalUser(
            CreateInternalUserRequest.builder()
                .fullName("Staff 2")
                .email("staff2@nextgen.vn")
                .phone("0912345678")
                .password("Pass@1234")
                .role(Role.STAFF)
                .build());
    assertThat(createRes.getUserId()).isEqualTo(300L);
    verify(auditLogRepository)
        .log(adminActor, "CREATE_INTERNAL_USER", "User", 300L, null, "STAFF");

    when(userRepository.findById(300L)).thenReturn(Optional.of(createdUser));
    when(orderRepository.getUserShoppingSummary(300L)).thenReturn(List.of());
    when(orderRepository.findByUserIdOrderByCreatedAtDesc(any(), any())).thenReturn(List.of());
    when(auditLogRepository.findUserTargetAuditLogs(any(), any())).thenReturn(List.of());
    adminUsersService.getUserDetail(300L);
    verify(auditLogRepository).findUserTargetAuditLogs(org.mockito.ArgumentMatchers.eq(300L), any());
  }

  private static void authenticateAs(User actor) {
    var auth =
        new UsernamePasswordAuthenticationToken(
            actor.getUserId().toString(),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + actor.getRole().name())));
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  private static void assertForbidden(Runnable call) {
    assertThatThrownBy(call::run)
        .isInstanceOf(AppException.class)
        .satisfies(
            ex -> {
              ErrorCode code = ((AppException) ex).getErrorCode();
              assertThat(code).isEqualTo(ErrorCode.FORBIDDEN);
              assertThat(code.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            });
  }
}
