package com.wedbackendservice.nextgen.dto.response;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void testFrom_MapsPageCorrectlyWithOneBasedPageNumber() {
        List<String> rawItems = List.of("Item A", "Item B", "Item C");
        Page<String> springPage = new PageImpl<>(rawItems, PageRequest.of(0, 10), 25);

        PageResponse<String> response = PageResponse.from(springPage, String::toUpperCase);

        assertThat(response.getContent()).containsExactly("ITEM A", "ITEM B", "ITEM C");
        assertThat(response.getPage()).isEqualTo(1); // 1-based page index
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(25L);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.isHasNext()).isTrue();
    }

    @Test
    void testFrom_LastPageHasNextIsFalse() {
        List<Integer> rawItems = List.of(21, 22);
        Page<Integer> springPage = new PageImpl<>(rawItems, PageRequest.of(2, 10), 22);

        PageResponse<String> response = PageResponse.from(springPage, String::valueOf);

        assertThat(response.getPage()).isEqualTo(3);
        assertThat(response.getTotalElements()).isEqualTo(22L);
        assertThat(response.isHasNext()).isFalse();
    }
}
