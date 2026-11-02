package com.wedbackendservice.nextgen.dto.response.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateAddressResponse {
    Long addressId;
    Long userId;
    String recipientName;
    String phone;
    String addressLine;
    Boolean isDefault;
}
