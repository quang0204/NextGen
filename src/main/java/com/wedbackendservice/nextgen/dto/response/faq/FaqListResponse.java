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
public class FaqListResponse {
    List<FaqResponse> items;
    long total;
    int page;
    int size;
    int totalPages;
    boolean hasNext;
}
