package com.wedbackendservice.nextgen.dto.request.wishlist;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WishlistToggleRequest {

    @NotNull(message = "productId không được để trống")
    Long productId;
}
