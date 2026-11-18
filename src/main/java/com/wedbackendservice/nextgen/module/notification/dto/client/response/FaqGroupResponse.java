package com.wedbackendservice.nextgen.module.notification.dto.client.response;

import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FaqGroupResponse {
  String category;
  List<FaqResponse> items;
}
