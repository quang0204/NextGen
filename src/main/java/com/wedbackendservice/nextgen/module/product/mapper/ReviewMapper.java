package com.wedbackendservice.nextgen.module.product.mapper;

import com.wedbackendservice.nextgen.module.product.dto.client.response.ReviewResponse;
import com.wedbackendservice.nextgen.module.product.entity.Review;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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
