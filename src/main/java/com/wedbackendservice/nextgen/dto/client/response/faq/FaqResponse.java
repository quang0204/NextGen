package com.wedbackendservice.nextgen.dto.client.response.faq;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

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
