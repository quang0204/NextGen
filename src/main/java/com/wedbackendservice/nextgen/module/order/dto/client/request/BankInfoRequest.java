package com.wedbackendservice.nextgen.module.order.dto.client.request;

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
