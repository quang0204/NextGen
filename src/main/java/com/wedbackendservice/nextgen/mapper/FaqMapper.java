package com.wedbackendservice.nextgen.mapper;

import com.wedbackendservice.nextgen.dto.client.response.faq.FaqResponse;
import com.wedbackendservice.nextgen.entity.FAQ;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FaqMapper {
    FaqResponse toResponse(FAQ faq);
}
