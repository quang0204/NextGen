package com.wedbackendservice.nextgen.dto.client.request.returnrequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

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
}
