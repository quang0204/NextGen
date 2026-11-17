package com.wedbackendservice.nextgen.dto.client.response.faq;

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
