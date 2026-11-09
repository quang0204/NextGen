package com.wedbackendservice.nextgen.dto.response.faq;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

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
