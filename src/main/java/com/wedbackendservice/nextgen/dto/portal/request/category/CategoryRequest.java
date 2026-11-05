package com.wedbackendservice.nextgen.dto.portal.request.category;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryRequest {

    Long parentId;

    @JsonProperty("name")
    @JsonAlias({"name", "categoryName"})
    @NotBlank(message = "Tên danh mục không được để trống")
    String name;

    String slug;
    String description;
    String iconUrl;

    @JsonProperty("isVisible")
    @JsonAlias({"isVisible", "isActive"})
    Boolean isVisible;
}
