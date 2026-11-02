package com.wedbackendservice.nextgen.mapper;

import com.wedbackendservice.nextgen.dto.response.category.CategoryResponse;
import com.wedbackendservice.nextgen.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    @Mapping(target = "productCount", ignore = true)
    CategoryResponse tocategoryResponse(Category category);
}
