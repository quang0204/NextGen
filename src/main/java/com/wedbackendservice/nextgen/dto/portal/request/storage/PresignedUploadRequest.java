package com.wedbackendservice.nextgen.dto.portal.request.storage;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.wedbackendservice.nextgen.enums.StorageType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
    Long brandId;
    Long categoryId;
    String storageType;

    @NotEmpty(message = "Danh sách tệp tải lên không được để trống")
    @Valid
    List<FileItem> files;

    public void setStorageType(Object val) {
        if (val instanceof StorageType st) {
            this.storageType = st.name();
        } else if (val != null) {
            this.storageType = val.toString();
        } else {
            this.storageType = null;
        }
    }

    public StorageType getStorageTypeEnum() {
        if (storageType == null || storageType.isBlank()) {
            return StorageType.PRODUCTS;
        }
        String s = storageType.trim().toUpperCase();
        if (s.equals("BRAND") || s.equals("BRANDS")) return StorageType.BRANDS;
        if (s.equals("CATEGORY") || s.equals("CATEGORIES")) return StorageType.CATEGORIES;
        if (s.equals("AVATAR") || s.equals("AVATARS")) return StorageType.AVATARS;
        return StorageType.PRODUCTS;
    }

    public static class PresignedUploadRequestBuilder {
        public PresignedUploadRequestBuilder storageType(StorageType type) {
            this.storageType = (type != null) ? type.name() : null;
            return this;
        }

        public PresignedUploadRequestBuilder storageType(String type) {
            this.storageType = type;
            return this;
        }
    }

    @Data
    @Getter
    @Setter
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
}
