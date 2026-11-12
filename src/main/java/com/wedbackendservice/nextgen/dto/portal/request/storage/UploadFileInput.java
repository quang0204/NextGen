package com.wedbackendservice.nextgen.dto.portal.request.storage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    String fileName;

    @NotBlank(message = "Content type không được để trống")
    String contentType;

    @NotNull(message = "Kích thước tập tin không được để trống")
    Long fileSize;
}
