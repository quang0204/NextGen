package com.wedbackendservice.nextgen.dto.response.admin;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaginationResponse {
    int page;
    int size;
    long totalItems;
    int totalPages;
}
