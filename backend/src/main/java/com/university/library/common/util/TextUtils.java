package com.university.library.common.util;

import java.util.Locale;

/**
 * 文本匹配工具：与前端 mock 的 includesKeyword 行为保持一致（忽略大小写、包含匹配）。
 */
public final class TextUtils {

    private TextUtils() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public static String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    public static boolean containsIgnoreCase(String field, String keyword) {
        if (isBlank(keyword)) {
            return true;
        }
        if (field == null) {
            return false;
        }
        return field.toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT));
    }
}
