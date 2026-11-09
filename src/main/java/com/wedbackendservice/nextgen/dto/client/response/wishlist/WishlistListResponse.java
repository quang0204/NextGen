package com.wedbackendservice.nextgen.dto.client.response.wishlist;

import com.wedbackendservice.nextgen.dto.common.PageResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WishlistListResponse {
    List<WishlistProductResponse> items;
    long total;
    PageResponse<?> pageInfo;
}
