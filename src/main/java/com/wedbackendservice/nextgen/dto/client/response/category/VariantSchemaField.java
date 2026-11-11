package com.wedbackendservice.nextgen.dto.client.response.category;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VariantSchemaField implements Serializable {
    String key;
    String label;
    String storage; // "COLUMN" | "ATTRIBUTE"
    String type;    // "text" | "number" | "select"
    Boolean required;
    List<Object> options;
    String unit;
}
