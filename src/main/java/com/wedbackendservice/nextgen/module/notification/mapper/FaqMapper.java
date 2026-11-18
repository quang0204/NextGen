package com.wedbackendservice.nextgen.module.notification.mapper;

import com.wedbackendservice.nextgen.module.notification.dto.client.response.FaqResponse;
import com.wedbackendservice.nextgen.module.notification.entity.FAQ;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FaqMapper {
  FaqResponse toResponse(FAQ faq);
}
