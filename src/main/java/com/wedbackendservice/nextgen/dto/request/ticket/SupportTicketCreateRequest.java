package com.wedbackendservice.nextgen.dto.request.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SupportTicketCreateRequest {

    @NotBlank(message = "Tiêu đề phiếu hỗ trợ không được để trống")
    @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
    String subject;

    @NotBlank(message = "Nội dung phiếu hỗ trợ không được để trống")
    @Size(max = 5000, message = "Nội dung tối đa 5000 ký tự")
    String description;

    String priority;
}
