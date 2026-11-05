package com.wedbackendservice.nextgen.util;

import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PaginationUtil {

    public static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(5, 10, 15, 20, 50, 100);
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 15;

    private PaginationUtil() {}

    public static void validate(int page, int size) {
        if (page < 1) {
            throw new AppException(ErrorCode.INVALID_PAGE_NUMBER);
        }
        if (!ALLOWED_PAGE_SIZES.contains(size)) {
            throw new AppException(ErrorCode.INVALID_PAGE_SIZE);
        }
    }

    public static Pageable createPageable(int page, int size, Sort sort) {
        validate(page, size);
        return PageRequest.of(page - 1, size, sort != null ? sort : Sort.unsorted());
    }

    public static Pageable createPageable(int page, int size) {
        return createPageable(page, size, Sort.unsorted());
    }
}
