package com.university.library.common.constant;

/**
 * 借阅记录状态（显式维护，不依赖日期推算）。
 */
public final class BorrowStatus {

    public static final String BORROWED = "BORROWED";
    public static final String RETURNED = "RETURNED";
    public static final String OVERDUE = "OVERDUE";

    private BorrowStatus() {
    }

    public static boolean isActive(String status) {
        return BORROWED.equals(status) || OVERDUE.equals(status);
    }
}
