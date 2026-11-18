package com.wedbackendservice.nextgen.module.notification.dto.client.response;

import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FaqResponse {
  Long faqId;
  String question;
  String answer;
  String category;
  Integer displayOrder;
  Boolean isActive;
  LocalDateTime createdAt;
  LocalDateTime updatedAt;
}
