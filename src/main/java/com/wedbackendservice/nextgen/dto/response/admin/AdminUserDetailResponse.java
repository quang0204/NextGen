package com.wedbackendservice.nextgen.dto.response.admin;

import com.wedbackendservice.nextgen.dto.response.user.UserAddressItemResponse;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.enums.UserStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUserDetailResponse {
    Long userId;
    String fullName;
    String email;
    String phone;
    Role role;
    UserStatus status;
    String lockReason;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    List<UserAddressItemResponse> addresses;
    List<UserOrderSummary> orders;
    List<UserAuditLogSummary> auditLogs;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UserOrderSummary {
        Long orderId;
        String orderCode;
        BigDecimal finalAmount;
        String status;
        LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UserAuditLogSummary {
        Long logId;
        String action;
        String entityName;
        String oldValue;
        String newValue;
        LocalDateTime createdAt;
    }
}
