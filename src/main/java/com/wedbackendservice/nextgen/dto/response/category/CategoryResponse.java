package com.wedbackendservice.nextgen.dto.response.category;

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

    Integer productCount;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    List<CategoryResponse> children;

    public String getCategoryName() {
        return name;
    }

    public Boolean getIsActive() {
        return isVisible;
    }
}
