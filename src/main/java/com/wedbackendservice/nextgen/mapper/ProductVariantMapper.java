package com.wedbackendservice.nextgen.mapper;

import com.wedbackendservice.nextgen.dto.client.response.product.ProductDetailResponse;
import com.wedbackendservice.nextgen.entity.Inventory;
import com.wedbackendservice.nextgen.entity.ProductVariant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface ProductVariantMapper {
    @Mapping(source = "v.variantId", target = "variantId")
    @Mapping(source = "v.sku", target = "sku")
    @Mapping(source = "v.variantName", target = "variantName")
    @Mapping(source = "v.color", target = "color")
    @Mapping(source = "v.ramGb", target = "ramGb")
    @Mapping(source = "v.romGb", target = "romGb")
    @Mapping(source = "v.connectionType", target = "connectionType")
    @Mapping(source = "v.attributes", target = "attributes")
    @Mapping(source = "v.imageUrl", target = "imageUrl")
    @Mapping(source = "v.price", target = "salePrice")
    @Mapping(source = "v.price", target = "price")
    @Mapping(target = "listPrice", expression = "java(resolveOrigPrice(v))")
    @Mapping(target = "compareAtPrice", expression = "java(resolveOrigPrice(v))")
    @Mapping(target = "availableQty", expression = "java(inv != null ? inv.getAvailableQty() : 0)")
    @Mapping(target = "isActive", expression = "java(\"ACTIVE\".equalsIgnoreCase(v.getStatus()))")
    ProductDetailResponse.VariantDetailResponse toVariantDetailResponse(ProductVariant v, Inventory inv);

    default BigDecimal resolveOrigPrice(ProductVariant v) {
        return v.getCompareAtPrice() != null && v.getCompareAtPrice().compareTo(BigDecimal.ZERO) > 0
                ? v.getCompareAtPrice()
                : v.getPrice();
    }
}
