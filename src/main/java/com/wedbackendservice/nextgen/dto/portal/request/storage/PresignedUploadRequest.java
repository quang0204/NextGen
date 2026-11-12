package com.wedbackendservice.nextgen.dto.portal.request.storage;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import com.wedbackendservice.nextgen.enums.StorageType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PresignedUploadRequest {

    Long productId;

    String storageType;

    @NotEmpty(message = "Danh sách tệp tải lên không được để trống")
    @Valid
    List<FileItem> files;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class FileItem {
        @NotBlank(message = "Tên tệp không được để trống")
        @JsonAlias({"originalFileName", "filename", "name"})
        String fileName;

        @NotBlank(message = "Content-Type không được để trống")
        @JsonAlias({"mimeType", "type"})
        String contentType;

        @NotNull(message = "Kích thước tệp không được để trống")
        @Positive(message = "Kích thước tệp phải lớn hơn 0")
        @JsonAlias({"size", "sizeBytes"})
        Long fileSize;
    }
    @NotNull(message = "Loại lưu trữ (storageType) không được để trống")
    StorageType storageType;

    Long brandId;

    @NotEmpty(message = "Danh sách tập tin không được để trống")
    @Valid
    List<UploadFileInput> files;
}
