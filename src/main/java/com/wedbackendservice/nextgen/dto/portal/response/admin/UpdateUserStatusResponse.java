package com.wedbackendservice.nextgen.dto.portal.response.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class UpdateUserStatusResponse {
  Long userId;
  String fullName;
  String status;
  String lockReason;
  String updatedBy;
  LocalDateTime updatedAt;
}
