package com.wedbackendservice.nextgen.service;

import com.nimbusds.jwt.SignedJWT;
import com.wedbackendservice.nextgen.dto.request.admin.CreateInternalUserRequest;
import com.wedbackendservice.nextgen.dto.request.admin.UpdateUserRoleRequest;
import com.wedbackendservice.nextgen.dto.request.admin.UpdateUserStatusRequest;
import com.wedbackendservice.nextgen.dto.response.PageResponse;
import com.wedbackendservice.nextgen.dto.response.admin.*;
import com.wedbackendservice.nextgen.dto.response.user.UserAddressItemResponse;
import com.wedbackendservice.nextgen.dto.session.TokenPair;
import com.wedbackendservice.nextgen.entity.AuditLog;
import com.wedbackendservice.nextgen.entity.Order;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.enums.UserStatus;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.mapper.UsersMapper;
import com.wedbackendservice.nextgen.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.repository.OrderRepository;
import com.wedbackendservice.nextgen.repository.UserAddressRepository;
import com.wedbackendservice.nextgen.repository.UserRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import com.wedbackendservice.nextgen.util.PaginationUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUsersService {

    private static final int DETAIL_HISTORY_LIMIT = 20;

    private final SecurityHelper securityHelper;
    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final OrderRepository orderRepository;
    private final AuditLogRepository auditLogRepository;
    private final TokenRedisService tokenRedisService;
    private final PasswordEncoder passwordEncoder;
    private final UsersMapper  usersMapper;

    /**
     * API 2.8: Danh sách người dùng (Tìm kiếm, phân trang, lọc theo Role & Status)
     */
    public PageResponse<AdminUserItemResponse> getUsersList(int page, int limit,
                                                            String search, String role, String status,
                                                            String sortBy, String sortDir) {
        log.debug("Get users list: page={}, limit={}, search={}, role={}, status={}", page, limit, search, role, status);

        String sortProperty = "createdAt";
        if (sortBy != null && !sortBy.isBlank()) {
            if ("fullName".equalsIgnoreCase(sortBy.trim())) {
                sortProperty = "fullName";
            } else if ("status".equalsIgnoreCase(sortBy.trim())) {
                sortProperty = "status";
            }
        }
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PaginationUtil.createPageable(page, limit, Sort.by(direction, sortProperty));

        // Validate role input if provided
        final Role roleFilter;
        if (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role.trim())) {
            try {
                roleFilter = Role.valueOf(role.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.ROLE_INVALID);
            }
        } else {
            roleFilter = null;
        }

        // Validate status input if provided
        final UserStatus statusFilter;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            try {
                statusFilter = UserStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.STATUS_INVALID);
            }
        } else {
            statusFilter = null;
        }

        String kw = (search != null && !search.isBlank()) ? "%" + search.trim().toLowerCase() + "%" : null;
        Page<User> pageResult = userRepository.searchUsers(roleFilter, statusFilter, kw, pageable);
        List<User> users = pageResult.getContent();
        if (users.isEmpty()) {
            return PageResponse.from(pageResult, u -> null);
        }

        List<Long> userIds = users.stream().map(User::getUserId).toList();
        Map<Long, Object[]> stats = orderRepository.statsByUserIds(userIds).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> r, (a, b) -> a));

        Page<AdminUserItemResponse> mapped = pageResult.map(u -> {
            Object[] s = stats.get(u.getUserId());
            return AdminUserItemResponse.builder()
                    .userId(u.getUserId())
                    .fullName(u.getFullName())
                    .email(u.getEmail())
                    .phone(u.getPhone())
                    .role(u.getRole())
                    .status(u.getStatus())
                    .lockReason(u.getLockReason())
                    .createdAt(u.getCreatedAt())
                    .totalOrders(s == null ? 0 : ((Number) s[1]).intValue())
                    .totalSpent(s == null ? BigDecimal.ZERO : (BigDecimal) s[2])
                    .build();
        });

        return PageResponse.from(mapped, Function.identity());
    }

    /**
     * API 2.9: Xem hồ sơ toàn diện của một tài khoản (Profile, Addresses, ShoppingSummary, RecentOrders)
     */
    public AdminUserDetailResponse getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));

        List<UserAddressItemResponse> addresses = userAddressRepository
                .findByUserIdOrderByIsDefaultDescAddressIdDesc(userId).stream()
                .map(a -> UserAddressItemResponse.builder()
                        .addressId(a.getAddressId())
                        .recipientName(a.getRecipientName())
                        .phone(a.getPhone())
                        .addressLine(a.getAddressLine())
                        .isDefault(a.getIsDefault())
                        .build())
                .toList();

        long totalOrders = orderRepository.countByUser_UserId(userId);
        long completedOrders = orderRepository.countByUser_UserIdAndStatus(userId, "COMPLETED");
        long cancelledOrders = orderRepository.countByUser_UserIdAndStatus(userId, "CANCELLED");
        BigDecimal lifetimeValue = orderRepository.sumLifetimeValueByUserId(userId);

        AdminUserDetailResponse.ShoppingSummary shoppingSummary = AdminUserDetailResponse.ShoppingSummary.builder()
                .totalOrders((int) totalOrders)
                .completedOrders((int) completedOrders)
                .cancelledOrders((int) cancelledOrders)
                .lifetimeValue(lifetimeValue != null ? lifetimeValue : BigDecimal.ZERO)
                .build();

        List<Order> userOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(0, DETAIL_HISTORY_LIMIT));

        List<AdminUserDetailResponse.RecentOrderSummary> recentOrders = userOrders.stream()
                .map(o -> AdminUserDetailResponse.RecentOrderSummary.builder()
                        .orderId(o.getOrderId())
                        .orderCode(o.getOrderCode())
                        .totalAmount(o.getFinalAmount())
                        .status(o.getStatus())
                        .createdAt(o.getCreatedAt())
                        .build())
                .toList();

        AdminUserDetailResponse.UserProfileSummary profileSummary = AdminUserDetailResponse.UserProfileSummary.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .lockReason(user.getLockReason())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();

        List<AdminUserDetailResponse.UserOrderSummary> orders = userOrders.stream()
                .map(o -> AdminUserDetailResponse.UserOrderSummary.builder()
                        .orderId(o.getOrderId())
                        .orderCode(o.getOrderCode())
                        .finalAmount(o.getFinalAmount())
                        .status(o.getStatus())
                        .createdAt(o.getCreatedAt())
                        .build())
                .toList();

        List<AdminUserDetailResponse.UserAuditLogSummary> auditLogs = auditLogRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, DETAIL_HISTORY_LIMIT)).stream()
                .map(a -> AdminUserDetailResponse.UserAuditLogSummary.builder()
                        .logId(a.getLogId())
                        .action(a.getAction())
                        .entityName(a.getEntityName())
                        .oldValue(a.getOldValue())
                        .newValue(a.getNewValue())
                        .createdAt(a.getCreatedAt())
                        .build())
                .toList();

        return AdminUserDetailResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .lockReason(user.getLockReason())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .profile(profileSummary)
                .addresses(addresses)
                .shoppingSummary(shoppingSummary)
                .recentOrders(recentOrders)
                .orders(orders)
                .auditLogs(auditLogs)
                .build();
    }

    /**
     * API 2.10: Admin tạo tài khoản nội bộ trực tiếp cho nhân viên (STAFF) hoặc ADMIN
     */
    @Transactional
    public CreateInternalUserResponse createInternalUser(CreateInternalUserRequest request) {
        User adminUser = securityHelper.getCurrentUser();
        String email = request.getEmail().trim().toLowerCase();
        String phone = request.getPhone().trim();
        String rawPassword = request.getPassword();

        if (rawPassword == null || rawPassword.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mật khẩu không được để trống");
        }

        if (userRepository.countByEmailOrPhone(email, phone) > 0) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        User newUser = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(newUser);

        auditLogRepository.log(adminUser, "CREATE_INTERNAL_USER", "User", savedUser.getUserId(), null, savedUser.getRole().name());
        CreateInternalUserResponse response= usersMapper.toCreateInternalUserResponse(savedUser);
        response.setMustChangePassword(true);
        return response;
    }

    /**
     * API 2.11: Khóa / Mở khóa tài khoản người dùng
     */
    @Transactional
    public UpdateUserStatusResponse updateUserStatus(Long userId, UpdateUserStatusRequest request) {
        User adminUser = securityHelper.authenticateAdmin();
        if (adminUser.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Quản trị viên không thể tự khóa tài khoản của chính mình.");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));

        UserStatus oldStatus = targetUser.getStatus();

        if (request.getStatus() == UserStatus.LOCKED) {
            // Chặn khóa Admin nếu là Admin duy nhất còn hoạt động
            if (targetUser.getRole() == Role.ADMIN) {
                long activeAdminCount = userRepository.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE);
                if (activeAdminCount <= 1) {
                    throw new AppException(ErrorCode.CANNOT_LOCK_LAST_ADMIN);
                }
            }

            if (request.getLockReason() == null || request.getLockReason().isBlank()) {
                throw new AppException(ErrorCode.LOCK_REASON_REQUIRED);
            }
            targetUser.setStatus(UserStatus.LOCKED);
            targetUser.setLockReason(request.getLockReason().trim());

            // Thu hồi toàn bộ JWT Token đang hoạt động (Blacklist refresh token trong Redis)
            revokeUserTokens(targetUser.getUserId());
        } else {
            targetUser.setStatus(UserStatus.ACTIVE);
            targetUser.setLockReason(null);
        }

        targetUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(targetUser);

        // Ghi log vết kiểm toán vào Audit_Logs
        auditLogRepository.log(adminUser, request.getStatus() == UserStatus.LOCKED ? "LOCK_USER" : "UNLOCK_USER",
                "User", targetUser.getUserId(), oldStatus != null ? oldStatus.name() : null, request.getStatus().name());

        String operatorEmail = adminUser.getEmail() != null ? adminUser.getEmail() : adminUser.getFullName();

        return UpdateUserStatusResponse.builder()
                .userId(targetUser.getUserId())
                .fullName(targetUser.getFullName())
                .status(targetUser.getStatus().name())
                .lockReason(targetUser.getLockReason())
                .updatedBy(operatorEmail)
                .build();
    }

    /**
     * API 2.12: Thay đổi phân quyền RBAC của tài khoản người dùng
     */
    @Transactional
    public UpdateUserRoleResponse updateUserRole(Long userId, UpdateUserRoleRequest request) {
        User adminUser = securityHelper.authenticateAdmin();
        if (adminUser.getUserId().equals(userId) && request.getRole() != Role.ADMIN) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Quản trị viên không thể tự hạ quyền của chính mình.");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));

        Role oldRole = targetUser.getRole();

        // Chặn hạ quyền Admin nếu là Admin duy nhất còn hoạt động
        if (oldRole == Role.ADMIN && request.getRole() != Role.ADMIN) {
            long activeAdminCount = userRepository.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE);
            if (activeAdminCount <= 1) {
                throw new AppException(ErrorCode.CANNOT_DEMOTE_LAST_ADMIN);
            }
        }

        targetUser.setRole(request.getRole());
        userRepository.save(targetUser);

        // Thu hồi toàn bộ session đăng nhập cũ khi thay đổi vai trò để quyền mới có hiệu lực ngay
        revokeUserTokens(targetUser.getUserId());

        // Ghi nhận Audit_Logs với old_value và new_value
        auditLogRepository.log(adminUser, "UPDATE_ROLE", "User", targetUser.getUserId(),
                oldRole != null ? oldRole.name() : null, request.getRole().name());

        String operatorEmail = adminUser.getEmail() != null ? adminUser.getEmail() : adminUser.getFullName();

        return UpdateUserRoleResponse.builder()
                .userId(targetUser.getUserId())
                .fullName(targetUser.getFullName())
                .previousRole(oldRole != null ? oldRole.name() : null)
                .newRole(request.getRole().name())
                .reason(request.getReason())
                .updatedBy(operatorEmail)
                .build();
    }

    private void revokeUserTokens(Long userId) {
        TokenPair tokenPair = tokenRedisService.getTokenPair(userId);
        if (tokenPair != null && tokenPair.getRefreshToken() != null) {
            try {
                Date refreshExp = SignedJWT.parse(tokenPair.getRefreshToken()).getJWTClaimsSet().getExpirationTime();
                tokenRedisService.blacklistToken(userId, tokenPair, refreshExp);
            } catch (Exception ignored) {
            }
        }
        tokenRedisService.deleteToken(userId);
    }

    private static String escapeLike(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
