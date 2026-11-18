package com.wedbackendservice.nextgen.module.order.dto.client.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateReturnRequest {

  @NotNull(message = "Mã đơn hàng (orderId) không được để trống!")
  Long orderId;

  @NotBlank(message = "Lý do đổi trả không được để trống!")
  @Size(min = 10, message = "Lý do đổi trả phải có tối thiểu 10 ký tự!")
  String reason;

  @NotEmpty(message = "Cần cung cấp tối thiểu 1 hình ảnh chứng minh lỗi sản phẩm!")
  List<String> evidenceImages;

  BankInfoRequest bankInfo;

  List<ReturnItemRequest> items;
}
