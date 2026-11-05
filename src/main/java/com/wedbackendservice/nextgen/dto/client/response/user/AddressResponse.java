package com.wedbackendservice.nextgen.dto.client.response.user;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressResponse {
    Long addressId;
    String recipientName;
    String phone;
    String addressLine;
    Boolean isDefault;
}
