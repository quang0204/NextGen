package com.wedbackendservice.nextgen.module.notification.controller.portal;

import com.wedbackendservice.nextgen.core.dto.ApiResponse;
import com.wedbackendservice.nextgen.module.notification.dto.portal.request.ConfirmUploadRequest;
import com.wedbackendservice.nextgen.module.notification.dto.portal.request.PresignedUploadRequest;
import com.wedbackendservice.nextgen.module.notification.dto.portal.response.PresignedUploadItemResponse;
import com.wedbackendservice.nextgen.module.notification.dto.portal.response.StorageFileResponse;
import com.wedbackendservice.nextgen.module.notification.service.StorageFileService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
public class StorageController {

  private final StorageFileService storageFileService;

  /**
   * Bước 1: Tạo liên kết Presigned URL để tải trực tiếp lên MinIO. STAFF / ADMIN: ảnh sản phẩm,
   * thương hiệu, danh mục. Khách đã đăng nhập: chỉ ảnh đánh giá (storageType = REVIEWS).
   */
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/presigned-urls")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<List<PresignedUploadItemResponse>> createPresignedUploads(
      @Valid @RequestBody PresignedUploadRequest request) {
    List<PresignedUploadItemResponse> items = storageFileService.createPresignedUploads(request);
    return ApiResponse.created("Tạo liên kết tải lên thành công!", items);
  }

  /**
   * Bước 3: Xác nhận tải lên và kiểm tra tính toàn vẹn của tập tin trên MinIO. Khách chỉ được xác
   * nhận phiên ảnh đánh giá của chính mình.
   */
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/confirm")
  public ApiResponse<StorageFileResponse> confirmUpload(
      @Valid @RequestBody ConfirmUploadRequest request) {
    StorageFileResponse response = storageFileService.confirmUpload(request);
    return ApiResponse.ok("Xác nhận tải lên tập tin thành công!", response);
  }
}
