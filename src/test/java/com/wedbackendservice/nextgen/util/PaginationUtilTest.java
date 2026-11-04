package com.wedbackendservice.nextgen.util;

import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaginationUtilTest {

    @Test
    void validate_PageLessThanOne_ThrowsInvalidPageNumber() {
        assertThatThrownBy(() -> PaginationUtil.validate(0, 15))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PAGE_NUMBER);

        assertThatThrownBy(() -> PaginationUtil.validate(-5, 15))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PAGE_NUMBER);
    }

    @Test
    void validate_InvalidSize_ThrowsInvalidPageSize() {
        assertThatThrownBy(() -> PaginationUtil.validate(1, 0))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PAGE_SIZE);

        assertThatThrownBy(() -> PaginationUtil.validate(1, 999))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PAGE_SIZE);
    }

    @Test
    void createPageable_ValidValues_ConvertsToOneBasedToZeroBased() {
        Pageable pageable = PaginationUtil.createPageable(1, 20, Sort.by("id").descending());

        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("id")).isNotNull();
    }
}
