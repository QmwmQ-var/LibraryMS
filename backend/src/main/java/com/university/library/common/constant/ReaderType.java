package com.university.library.common.constant;

import lombok.Getter;

/**
 * 读者类型及其借阅规则。
 *
 * <p>规则来源：本科生 5 本/30 天，研究生 10 本/60 天，教师 15 本/90 天。</p>
 */
@Getter
public enum ReaderType {

    UNDERGRADUATE("本科生", 5, 30),
    GRADUATE("研究生", 10, 60),
    TEACHER("教师", 15, 90);

    private final String label;
    private final int maxBorrow;
    private final int borrowDays;

    ReaderType(String label, int maxBorrow, int borrowDays) {
        this.label = label;
        this.maxBorrow = maxBorrow;
        this.borrowDays = borrowDays;
    }

    /**
     * 容错解析：数据库中若出现未知类型，回退为本科生规则。
     */
    public static ReaderType from(String value) {
        if (value != null) {
            for (ReaderType type : values()) {
                if (type.name().equals(value)) {
                    return type;
                }
            }
        }
        return UNDERGRADUATE;
    }

    public static boolean isValid(String value) {
        for (ReaderType type : values()) {
            if (type.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
