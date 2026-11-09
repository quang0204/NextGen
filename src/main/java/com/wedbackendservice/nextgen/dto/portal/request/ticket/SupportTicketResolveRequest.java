package com.wedbackendservice.nextgen.dto.portal.request.ticket;

import com.wedbackendservice.nextgen.enums.TicketStatus;
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

    TicketStatus status;

    @Size(max = 2000, message = "Ghi chú xử lý tối đa 2000 ký tự")
    String resolutionNote;
}
