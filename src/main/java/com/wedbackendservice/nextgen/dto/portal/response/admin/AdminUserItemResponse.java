package com.wedbackendservice.nextgen.dto.portal.response.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.enums.UserStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminUserItemResponse {
    Long userId;
    String fullName;
    String email;
    String phone;
    Role role;
    UserStatus status;
    String lockReason;
    Integer totalOrders;
    BigDecimal totalSpent;
    LocalDateTime createdAt;
}
