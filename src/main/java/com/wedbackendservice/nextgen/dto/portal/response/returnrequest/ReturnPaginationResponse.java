package com.wedbackendservice.nextgen.dto.portal.response.returnrequest;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReturnPaginationResponse {
    int currentPage;
    int pageSize;
    long totalItems;
    int totalPages;
}
