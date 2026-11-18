package com.wedbackendservice.nextgen.module.notification.dto.portal;

import com.wedbackendservice.nextgen.module.notification.enums.StorageType;
import java.io.Serializable;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StorageUploadSession implements Serializable {
  String uploadId;
  StorageType storageType;
  Long brandId;
  Long userId;
  String objectKey;
  String fileName;
  String contentType;
  Long fileSize;
  String canonicalUrl;
  String status; // PENDING, COMPLETED, ATTACHED
  Long createdAt;
  Long expiresAt;
}
