package com.wedbackendservice.nextgen.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private DateTimeUtil() {}

    public static String formatIsoInstant(LocalDateTime ldt) {
        if (ldt == null) {
            return null;
        }
        return ISO_FORMATTER.format(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }
}
