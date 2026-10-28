package com.university.library.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期格式化工具（成功提示语中的中文日期）。
 */
public final class DateUtils {

    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("M月d日");

    private DateUtils() {
    }

    public static String formatMonthDay(LocalDate date) {
        return date == null ? "—" : date.format(MONTH_DAY);
    }

    public static String iso(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toString();
    }
}
