package com.wedbackendservice.nextgen.dto.client.request.returnrequest;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BankInfoRequest {
    String bankName;
    String accountNumber;
    String accountHolder;
}
