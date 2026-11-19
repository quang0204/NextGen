package com.wedbackendservice.nextgen.module.product.mapper;

import com.wedbackendservice.nextgen.module.product.dto.client.response.BrandResponse;
import com.wedbackendservice.nextgen.module.product.entity.Brand;
import com.wedbackendservice.nextgen.module.product.entity.Category;
import java.util.Collections;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BrandMapper {
  @Mapping(source = "brand.brandName", target = "name")
  @Mapping(source = "brand.isActive", target = "isVisible")
  @Mapping(source = "logoUrl", target = "logoUrl")
  @Mapping(source = "totalProducts", target = "totalProducts")
  @Mapping(target = "categoryIds", expression = "java(mapCategoryIds(brand))")
  BrandResponse toBrandResponse(Brand brand, String logoUrl, int totalProducts);

  default List<Long> mapCategoryIds(Brand brand) {
    if (brand == null || brand.getCategories() == null) {
      return Collections.emptyList();
    }
    return brand.getCategories().stream()
        .map(Category::getCategoryId)
        .sorted()
        .toList();
  }
}
