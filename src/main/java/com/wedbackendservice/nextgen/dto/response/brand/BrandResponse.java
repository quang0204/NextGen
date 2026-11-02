package com.wedbackendservice.nextgen.dto.response.brand;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrandResponse {
    Long brandId;

    @JsonProperty("name")
    String name;

    String slug;
    String logoUrl;
    String description;

    @JsonProperty("isVisible")
    Boolean isVisible;

    Integer totalProducts;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
