package com.wedbackendservice.nextgen.mapper;

import com.wedbackendservice.nextgen.dto.client.response.brand.BrandResponse;
import com.wedbackendservice.nextgen.entity.Brand;
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
