package com.wedbackendservice.nextgen.module.product.dto.client.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
