package com.swiftsprinttech.wms01.config;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author liujiawei
 */
public class TimeUtil {
    private static final DateTimeFormatter ISO8601_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public static String format(LocalDateTime dateTime) {
        return dateTime.format(ISO8601_FORMATTER);
    }

    public static LocalDateTime parse(String dateTime) {
        return LocalDateTime.parse(dateTime, ISO8601_FORMATTER);
    }
}