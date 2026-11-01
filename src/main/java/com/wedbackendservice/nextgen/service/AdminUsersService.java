package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.security.SecurityHelper;

import com.nimbusds.jwt.SignedJWT;
import com.wedbackendservice.nextgen.dto.session.TokenPair;
import com.wedbackendservice.nextgen.dto.response.admin.*;
import com.wedbackendservice.nextgen.dto.response.user.UserAddressItemResponse;
import com.wedbackendservice.nextgen.dto.request.admin.CreateInternalUserRequest;
import com.wedbackendservice.nextgen.dto.request.admin.UpdateUserRoleRequest;
import com.wedbackendservice.nextgen.dto.request.admin.UpdateUserStatusRequest;
import com.wedbackendservice.nextgen.entity.AuditLog;
import com.wedbackendservice.nextgen.entity.Order;
import com.wedbackendservice.nextgen.entity.UserAddress;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.enums.UserStatus;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.repository.OrderRepository;
import com.wedbackendservice.nextgen.repository.UserAddressRepository;
import com.wedbackendservice.nextgen.repository.UserRepository;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUsersService {

    private final SecurityHelper securityHelper;
    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final OrderRepository orderRepository;
    private final AuditLogRepository auditLogRepository;
    private final TokenRedisService tokenRedisService;
    private final PasswordEncoder passwordEncoder;

    /**
     * API 8: Danh sách người dùng (Admin)
     */
    public AdminUserListResponse getUsersList(String authHeader, int page, int size,
                                             String keyword, String role, String status) {
        securityHelper.authenticateAdmin(authHeader);

        int safePage = Math.max(page, 1);
        int safeSize = size > 0 ? size : 10;
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String searchPattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate namePredicate = cb.like(cb.lower(root.get("fullName")), searchPattern);
                Predicate emailPredicate = cb.like(cb.lower(root.get("email")), searchPattern);
                Predicate phonePredicate = cb.like(root.get("phone"), searchPattern);
                predicates.add(cb.or(namePredicate, emailPredicate, phonePredicate));
            }

            if (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role.trim())) {
                try {
                    Role roleEnum = Role.valueOf(role.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("role"), roleEnum));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
                try {
                    UserStatus statusEnum = UserStatus.valueOf(status.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), statusEnum));
                } catch (IllegalArgumentException ignored) {
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> pageResult = userRepository.findAll(spec, pageable);

        List<AdminUserItemResponse> items = pageResult.getContent().stream()
                .map(u -> AdminUserItemResponse.builder()
                        .userId(u.getUserId())
                        .fullName(u.getFullName())
                        .email(u.getEmail())
                        .phone(u.getPhone())
                        .role(u.getRole())
                        .status(u.getStatus())
                        .lockReason(u.getLockReason())
                        .createdAt(u.getCreatedAt())
                        .build())
                .toList();

        PaginationResponse pagination = PaginationResponse.builder()
                .page(safePage)
                .size(safeSize)
                .totalItems(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .build();

        return AdminUserListResponse.builder()
                .items(items)
                .pagination(pagination)
                .build();
    }

    /**
     * API 9 (bảng tổng hợp): Xem chi tiết 1 người dùng kèm đơn hàng, địa chỉ, nhật ký
     */
    public AdminUserDetailResponse getUserDetail(String authHeader, Long userId) {
        securityHelper.authenticateAdmin(authHeader);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIT));

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

        List<AdminUserDetailResponse.UserOrderSummary> orders = orderRepository
                .findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(o -> AdminUserDetailResponse.UserOrderSummary.builder()
                        .orderId(o.getOrderId())
                        .orderCode(o.getOrderCode())
                        .finalAmount(o.getFinalAmount())
                        .status(o.getStatus())
                        .createdAt(o.getCreatedAt())
                        .build())
                .toList();

        List<AdminUserDetailResponse.UserAuditLogSummary> auditLogs = auditLogRepository
                .findByUserIdOrderByCreatedAtDesc(userId).stream()
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
                .addresses(addresses)
                .orders(orders)
                .auditLogs(auditLogs)
                .build();
    }

    /**
     * API 10 (theo bảng) / API 9 (theo đặc tả): Cấp tài khoản nội bộ (STAFF hoặc ADMIN)
     */
    @Transactional
    public void createInternalUser(String authHeader, CreateInternalUserRequest request) {
        User adminUser = securityHelper.authenticateAdmin(authHeader);

        String email = request.getEmail().trim();
        String phone = request.getPhone().trim();

        if (userRepository.countByEmailOrPhone(email, phone) > 0) {
            log.warn("Email [{}] hoặc SĐT [{}] đã tồn tại khi tạo user nội bộ", email, phone);
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        if (request.getRole() != Role.STAFF && request.getRole() != Role.ADMIN) {
            throw new AppException(ErrorCode.ROLE_INVALID, "Chỉ được phép tạo tài khoản vai trò STAFF hoặc ADMIN");
        }

        User newUser = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        newUser = userRepository.save(newUser);

        auditLogRepository.save(AuditLog.builder()
                .user(adminUser)
                .action("CREATE_INTERNAL_USER")
                .entityName("User")
                .entityId(newUser.getUserId())
                .newValue(newUser.getRole().name())
                .createdAt(LocalDateTime.now())
                .build());
    }

    /**
     * API 12 (theo bảng) / API 10 (theo đặc tả): Khóa / Mở khóa tài khoản
     */
    @Transactional
    public void updateUserStatus(String authHeader, Long userId, UpdateUserStatusRequest request) {
        User adminUser = securityHelper.authenticateAdmin(authHeader);

        if (adminUser.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Quản trị viên không thể tự khóa tài khoản của chính mình.");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIT));

        UserStatus oldStatus = targetUser.getStatus();

        if (request.getStatus() == UserStatus.LOCKED) {
            if (request.getLockReason() == null || request.getLockReason().isBlank()) {
                throw new AppException(ErrorCode.LOCK_REASON_REQUIRED);
            }
            targetUser.setStatus(UserStatus.LOCKED);
            targetUser.setLockReason(request.getLockReason().trim());

            // Vô hiệu hóa token và xóa session Redis của user bị khóa
            TokenPair tokenPair = tokenRedisService.getTokenPair(targetUser.getUserId());
            if (tokenPair != null && tokenPair.getRefreshToken() != null) {
                try {
                    Date refreshExp = SignedJWT.parse(tokenPair.getRefreshToken()).getJWTClaimsSet().getExpirationTime();
                    tokenRedisService.blacklistToken(targetUser.getUserId(), tokenPair, refreshExp);
                } catch (Exception ignored) {
                }
            }
            tokenRedisService.deleteToken(targetUser.getUserId());
        } else {
            targetUser.setStatus(UserStatus.ACTIVE);
            targetUser.setLockReason(null);
        }

        targetUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(targetUser);

        auditLogRepository.save(AuditLog.builder()
                .user(adminUser)
                .action(request.getStatus() == UserStatus.LOCKED ? "LOCK_USER" : "UNLOCK_USER")
                .entityName("User")
                .entityId(targetUser.getUserId())
                .oldValue(oldStatus != null ? oldStatus.name() : null)
                .newValue(request.getStatus().name())
                .createdAt(LocalDateTime.now())
                .build());
    }

    /**
     * API 11: Thay đổi vai trò người dùng (CUSTOMER ↔ STAFF ↔ ADMIN)
     */
    @Transactional
    public void updateUserRole(String authHeader, Long userId, UpdateUserRoleRequest request) {
        User adminUser = securityHelper.authenticateAdmin(authHeader);

        if (adminUser.getUserId().equals(userId) && request.getRole() != Role.ADMIN) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Quản trị viên không thể tự hạ quyền của chính mình.");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIT));

        Role oldRole = targetUser.getRole();
        targetUser.setRole(request.getRole());
        targetUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(targetUser);

        auditLogRepository.save(AuditLog.builder()
                .user(adminUser)
                .action("UPDATE_ROLE")
                .entityName("User")
                .entityId(targetUser.getUserId())
                .oldValue(oldRole != null ? oldRole.name() : null)
                .newValue(request.getRole().name())
                .createdAt(LocalDateTime.now())
                .build());
    }
}
