package com.wedbackendservice.nextgen.module.notification.dto.client.response;

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
public class SupportTicketResponse {
  Long ticketId;
  Long userId;
  String customerName;
  String customerEmail;
  String customerPhone;
  String subject;
  String description;
  String priority;
  String status;
  Long handledById;
  String handledByName;
  String resolutionNote;
  LocalDateTime resolvedAt;
  LocalDateTime createdAt;
  LocalDateTime updatedAt;
}
