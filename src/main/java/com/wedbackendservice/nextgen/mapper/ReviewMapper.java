package com.wedbackendservice.nextgen.mapper;

import com.wedbackendservice.nextgen.dto.client.response.review.ReviewResponse;
import com.wedbackendservice.nextgen.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    @Mapping(source = "review.orderItem.orderItemId", target = "orderItemId")
    @Mapping(source = "review.orderItem.variant.product.productId", target = "productId")
    @Mapping(source = "review.orderItem.productNameSnapshot", target = "productName")
    @Mapping(source = "review.orderItem.variant.variantName", target = "variantName")
    @Mapping(source = "review.orderItem.order.user.fullName", target = "customerName")
    @Mapping(source = "imageUrls", target = "imageUrls")
    ReviewResponse toResponse(Review review, List<String> imageUrls);
}
