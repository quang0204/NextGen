package com.wedbackendservice.nextgen.dto.request.brand;

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
public class BrandRequest {

    @JsonProperty("name")
    @JsonAlias({"name", "brandName"})
    @NotBlank(message = "Tên thương hiệu không được để trống")
    String name;

    String slug;
    String logoUrl;
    String description;

    @JsonProperty("isVisible")
    @JsonAlias({"isVisible", "isActive"})
    Boolean isVisible;

    public String getBrandName() {
        return name;
    }

    public void setBrandName(String brandName) {
        this.name = brandName;
    }

    public Boolean getIsActive() {
        return isVisible;
    }

    public void setIsActive(Boolean isActive) {
        this.isVisible = isActive;
    }
}
