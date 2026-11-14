package com.wedbackendservice.nextgen.dto.client.request.review;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewCreateRequest {

    @NotNull(message = "orderItemId không được để trống")
    Long orderItemId;

    @NotNull(message = "Số sao đánh giá không được để trống")
    @Min(value = 1, message = "Số sao đánh giá tối thiểu là 1")
    @Max(value = 5, message = "Số sao đánh giá tối đa là 5")
    Integer rating;

    @Size(max = 2000, message = "Nội dung đánh giá tối đa 2000 ký tự")
    String comment;

    /**
     * Ảnh đánh giá (tối đa 5): uploadId nhận từ POST /api/v1/storage/presigned-urls (storageType = REVIEWS)
     * và đã xác nhận qua POST /api/v1/storage/confirm.
     */
    @Size(max = 5, message = "Mỗi đánh giá chỉ được gửi tối đa 5 ảnh")
    List<String> uploadIds;
}
