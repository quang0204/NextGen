package com.wedbackendservice.nextgen.dto.request.cart;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartSyncRequest {
    @JsonProperty("guestItems")
    @JsonAlias({"guestItems", "items"})
    @Valid
    List<CartItemAddRequest> guestItems;
}
