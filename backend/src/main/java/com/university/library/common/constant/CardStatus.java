package com.university.library.common.constant;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 借阅证状态。
 */
public final class CardStatus {

    public static final String ACTIVE = "ACTIVE";
    public static final String LOST = "LOST";
    public static final String CANCELLED = "CANCELLED";

    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    static {
        LABELS.put(ACTIVE, "正常");
        LABELS.put(LOST, "挂失");
        LABELS.put(CANCELLED, "注销");
    }

    private CardStatus() {
    }

    public static boolean isValid(String value) {
        return LABELS.containsKey(value);
    }

    public static String label(String value) {
        return LABELS.getOrDefault(value, "异常");
    }
}
