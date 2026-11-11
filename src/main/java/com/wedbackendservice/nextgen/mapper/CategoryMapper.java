package com.wedbackendservice.nextgen.mapper;

import com.wedbackendservice.nextgen.dto.client.response.category.CategoryResponse;
import com.wedbackendservice.nextgen.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    @Mapping(source = "categoryName", target = "name")
    @Mapping(source = "parent.categoryId", target = "parentId")
    @Mapping(source = "parent.categoryName", target = "parentName")
    @Mapping(source = "isActive", target = "isVisible")
    @Mapping(target = "productCount", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "effectiveVariantSchema", ignore = true)
    CategoryResponse tocategoryResponse(Category category);
}
