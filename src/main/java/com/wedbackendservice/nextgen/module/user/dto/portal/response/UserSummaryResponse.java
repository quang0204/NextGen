package com.wedbackendservice.nextgen.module.user.dto.portal.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserSummaryResponse {
  long totalUsers;
  long adminCount;
  long staffCount;
  long customerCount;
  long lockedCount;
}
