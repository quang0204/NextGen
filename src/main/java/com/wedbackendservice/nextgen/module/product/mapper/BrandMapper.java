package com.wedbackendservice.nextgen.module.product.mapper;

import com.wedbackendservice.nextgen.module.product.dto.client.response.BrandResponse;
import com.wedbackendservice.nextgen.module.product.entity.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BrandMapper {
  @Mapping(source = "brand.brandName", target = "name")
  @Mapping(source = "brand.isActive", target = "isVisible")
  @Mapping(source = "logoUrl", target = "logoUrl")
  @Mapping(source = "totalProducts", target = "totalProducts")
  BrandResponse toBrandResponse(Brand brand, String logoUrl, int totalProducts);
}
