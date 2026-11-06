package com.wedbackendservice.nextgen.dto.response.wishlist;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WishlistProductResponse {
    Long wishlistId;
    Long productId;
    String productName;
    String slug;
    String thumbnailUrl;
    BigDecimal basePrice;
    BigDecimal priceFrom;
    String status;
    List<String> images;
    List<String> specs;
    int variantCount;
    LocalDateTime addedAt;
}
