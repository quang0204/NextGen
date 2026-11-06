package com.wedbackendservice.nextgen.dto.response.wishlist;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WishlistToggleResponse {
    Long productId;
    boolean isFavorite;
    String message;
}
