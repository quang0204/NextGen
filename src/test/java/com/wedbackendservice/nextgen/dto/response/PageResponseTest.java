package com.wedbackendservice.nextgen.dto.response;

import com.wedbackendservice.nextgen.dto.response.inventory.AdminInventoryListResponse;
import com.wedbackendservice.nextgen.dto.response.inventory.InventoryItemResponse;
import com.wedbackendservice.nextgen.dto.response.inventory.InventorySummaryResponse;
import com.wedbackendservice.nextgen.dto.response.order.AdminOrderItemResponse;
import com.wedbackendservice.nextgen.dto.response.order.AdminOrderListResponse;
import com.wedbackendservice.nextgen.dto.response.order.PipelineSummaryResponse;
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
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(25L);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.isHasNext()).isTrue();
    }

    @Test
    void testFrom_IdentityOverload() {
        List<String> rawItems = List.of("X", "Y");
        Page<String> springPage = new PageImpl<>(rawItems, PageRequest.of(1, 2), 2);

        PageResponse<String> response = PageResponse.from(springPage);

        assertThat(response.getContent()).containsExactly("X", "Y");
        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.isHasNext()).isFalse();
    }

    @Test
    void testAdminInventoryListResponse_InheritsPageResponse() {
        AdminInventoryListResponse response = AdminInventoryListResponse.builder()
                .content(List.of(InventoryItemResponse.builder().inventoryId(1L).build()))
                .page(1)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .hasNext(false)
                .summary(InventorySummaryResponse.builder().totalOnHand(100).build())
                .build();

        assertThat(response).isInstanceOf(PageResponse.class);
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getSummary().getTotalOnHand()).isEqualTo(100);
    }

    @Test
    void testAdminOrderListResponse_InheritsPageResponse() {
        AdminOrderListResponse response = AdminOrderListResponse.builder()
                .content(List.of(AdminOrderItemResponse.builder().orderId(10L).build()))
                .page(1)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .hasNext(false)
                .pipelineSummary(PipelineSummaryResponse.builder().confirmed(2L).build())
                .build();

        assertThat(response).isInstanceOf(PageResponse.class);
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getPipelineSummary().getConfirmed()).isEqualTo(2L);
    }
}
