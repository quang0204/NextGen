package com.wedbackendservice.nextgen.module.user.dto.portal.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminUserStatsResponse {
  Long totalUsers;
  Long totalAdmins;
  Long totalStaffs;
  Long totalLocked;
}
