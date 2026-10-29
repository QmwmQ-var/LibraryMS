package com.university.library.common.constant;

import java.util.List;

/**
 * 预约状态常量。
 *
 * <p>状态流转：
 * PENDING（排队中）→ READY（到书待领取）→ FULFILLED（已借出）；
 * PENDING / READY 都可以 CANCELLED；READY 超过保留时间变为 EXPIRED。</p>
 */
public final class ReservationStatus {

    public static final String PENDING = "PENDING";
    public static final String READY = "READY";
    public static final String FULFILLED = "FULFILLED";
    public static final String CANCELLED = "CANCELLED";
    public static final String EXPIRED = "EXPIRED";

    /** 仍占用预约名额、并使图书不能续借的状态。 */
    public static final List<String> ACTIVE = List.of(PENDING, READY);

    private ReservationStatus() {
    }

    public static boolean isActive(String status) {
        return PENDING.equals(status) || READY.equals(status);
    }

    public static boolean isValid(String status) {
        return PENDING.equals(status) || READY.equals(status)
                || FULFILLED.equals(status) || CANCELLED.equals(status) || EXPIRED.equals(status);
    }

    public static String label(String status) {
        return switch (status == null ? "" : status) {
            case PENDING -> "排队中";
            case READY -> "已到书";
            case FULFILLED -> "已借出";
            case CANCELLED -> "已取消";
            case EXPIRED -> "已过期";
            default -> "未知";
        };
    }
}
