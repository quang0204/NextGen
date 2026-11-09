package com.wedbackendservice.nextgen.dto.common;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageInfoResponse {
    int page;
    int size;
    int totalPages;
    long totalElements;
    boolean hasNext;

    public static PageInfoResponse of(int page, int size, Page<?> result) {
        return PageInfoResponse.builder()
                .page(page)
                .size(size)
                .totalPages(result.getTotalPages())
                .totalElements(result.getTotalElements())
                .hasNext(result.hasNext())
                .build();
    }
}
