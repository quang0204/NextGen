package com.wedbackendservice.nextgen.dto.response.voucher;

import com.wedbackendservice.nextgen.dto.response.admin.PaginationResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminVoucherListResponse {
    List<AdminVoucherItemResponse> items;
    PaginationResponse pagination;
}
