package com.wedbackendservice.nextgen.module.user.dto.portal.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wedbackendservice.nextgen.core.dto.PageResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminUsersListResponse extends PageResponse<AdminUserItemResponse> {
  UserSummaryResponse summary;
  Long totalUsers;
  Long adminCount;
  Long staffCount;
  Long customerCount;
  Long lockedCount;
}
