package com.wedbackendservice.nextgen.dto.client.response.category;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryResponse {
    Long categoryId;
    Long parentId;
    String parentName;

    @JsonProperty("name")
    String name;

    String slug;
    String description;
    String iconUrl;

    @JsonProperty("isVisible")
    Boolean isVisible;

    List<VariantSchemaField> variantSchema;
    List<VariantSchemaField> effectiveVariantSchema;

    Integer productCount;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    List<CategoryResponse> children;
}
