package com.wedbackendservice.nextgen.util;

import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class DateTimeUtil {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private DateTimeUtil() {}

    public static String formatIsoInstant(LocalDateTime ldt) {
        if (ldt == null) {
            return null;
        }
        return ISO_FORMATTER.format(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDateTime parseIsoDateTime(String text, String fieldName, boolean isEndOfDay) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String trimmed = text.trim();
        try {
            return LocalDateTime.ofInstant(Instant.parse(trimmed), ZoneId.systemDefault());
        } catch (DateTimeParseException ex1) {
            try {
                return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException ex2) {
                try {
                    LocalDate date = LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
                    return isEndOfDay ? date.atTime(23, 59, 59) : date.atStartOfDay();
                } catch (Exception e) {
                    throw new AppException(ErrorCode.INVALID_REQUEST, "Định dạng thời gian " + fieldName + " không hợp lệ (chuẩn ISO-8601)!");
                }
            }
        }
    }
}
