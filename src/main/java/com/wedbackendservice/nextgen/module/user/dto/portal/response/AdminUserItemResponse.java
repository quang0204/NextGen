package com.wedbackendservice.nextgen.module.user.dto.portal.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wedbackendservice.nextgen.module.user.enums.Role;
import com.wedbackendservice.nextgen.module.user.enums.UserStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
