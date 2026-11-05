package com.wedbackendservice.nextgen.dto.response.admin;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Danh sách giao dịch thanh toán kèm thông tin phân trang (1-based).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminPaymentListResponse {
    List<AdminPaymentItemResponse> items;
    int page;
    int size;
    int totalPages;
    long totalElements;
    boolean hasNext;
}
