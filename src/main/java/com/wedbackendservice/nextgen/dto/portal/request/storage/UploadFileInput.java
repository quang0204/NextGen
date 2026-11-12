package com.wedbackendservice.nextgen.dto.portal.request.storage;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UploadFileInput {
    @NotBlank(message = "Tên tập tin không được để trống")
    @JsonAlias({"originalFileName", "filename", "name"})
    String fileName;

    @NotBlank(message = "Content type không được để trống")
    @JsonAlias({"mimeType", "type"})
    String contentType;

    @NotNull(message = "Kích thước tập tin không được để trống")
    @Positive(message = "Kích thước tập tin phải lớn hơn 0")
    @JsonAlias({"size", "sizeBytes"})
    Long fileSize;
}
