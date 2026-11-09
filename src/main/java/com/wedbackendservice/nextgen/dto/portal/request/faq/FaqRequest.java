package com.wedbackendservice.nextgen.dto.portal.request.faq;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FaqRequest {

    @NotBlank(message = "Câu hỏi không được để trống")
    @Size(max = 500, message = "Câu hỏi tối đa 500 ký tự")
    String question;

    @NotBlank(message = "Câu trả lời không được để trống")
    @Size(max = 5000, message = "Câu trả lời tối đa 5000 ký tự")
    String answer;

    @Size(max = 100, message = "Danh mục tối đa 100 ký tự")
    String category;

    Integer displayOrder;

    Boolean isActive;
}
