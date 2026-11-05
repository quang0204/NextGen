package com.wedbackendservice.nextgen.dto.client.response.user;

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
public class SetDefaultAddressResponse {
    Long defaultAddressId;
    String recipientName;
    String phone;
    String addressLine;
    Boolean isDefault;
}
