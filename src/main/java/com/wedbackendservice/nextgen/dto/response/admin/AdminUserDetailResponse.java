package com.wedbackendservice.nextgen.dto.response.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
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

    UserProfileSummary profile;
    List<UserAddressItemResponse> addresses;
    ShoppingSummary shoppingSummary;
    List<RecentOrderSummary> recentOrders;
    List<UserOrderSummary> orders;
    List<UserAuditLogSummary> auditLogs;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserAuditLogSummary {
        Long logId;
        String action;
        String entityName;
        String oldValue;
        String newValue;
        LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserProfileSummary {
        Long userId;
        String fullName;
        String email;
        String phone;
        Role role;
        UserStatus status;
        String lockReason;
        LocalDateTime createdAt;
        LocalDateTime updatedAt;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ShoppingSummary {
        Integer totalOrders;
        Integer completedOrders;
        Integer cancelledOrders;
        BigDecimal lifetimeValue;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecentOrderSummary {
        Long orderId;
        String orderCode;
        BigDecimal totalAmount;
        String status;
        LocalDateTime createdAt;
    }
}
