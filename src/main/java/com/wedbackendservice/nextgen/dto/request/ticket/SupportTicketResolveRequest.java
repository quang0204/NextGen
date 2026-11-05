package com.wedbackendservice.nextgen.dto.request.ticket;

import com.wedbackendservice.nextgen.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SupportTicketResolveRequest {

    @NotNull(message = "Trạng thái ticket không được để trống")
    TicketStatus status;

    @Size(max = 2000, message = "Ghi chú xử lý tối đa 2000 ký tự")
    String resolutionNote;
}
