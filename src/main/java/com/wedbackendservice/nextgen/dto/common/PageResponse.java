package com.wedbackendservice.nextgen.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {
    List<T> content;
    int page;            // 1-based
    int size;
    int totalPages;
    long totalElements;  // dùng long, không ép (int)
    boolean hasNext;

    public static <E, T> PageResponse<T> from(Page<E> p, Function<E, T> mapper) {
        return PageResponse.<T>builder()
                .content(p.getContent().stream().map(mapper).toList())
                .page(p.getNumber() + 1)
                .size(p.getSize())
                .totalPages(p.getTotalPages())
                .totalElements(p.getTotalElements())
                .hasNext(p.hasNext())
                .build();
    }

    public static <T> PageResponse<T> from(Page<T> p) {
        return from(p, Function.identity());
    }

    public static <T> PageResponse<T> of(int page, int size, Page<?> result) {
        return PageResponse.<T>builder()
                .page(page)
                .size(size)
                .totalPages(result.getTotalPages())
                .totalElements(result.getTotalElements())
                .hasNext(result.hasNext())
                .build();
    }
}
