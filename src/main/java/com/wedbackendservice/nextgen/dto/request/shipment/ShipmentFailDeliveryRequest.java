package com.wedbackendservice.nextgen.dto.request.shipment;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShipmentFailDeliveryRequest {
    @JsonProperty("failReason")
    @JsonAlias({"failReason", "reason"})
    @NotBlank(message = "Lý do thất bại không được để trống")
    String failReason;

    String note;
}
