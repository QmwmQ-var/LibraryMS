package com.university.library.vo;

import lombok.Data;

/**
 * 预约汇总。
 */
@Data
public class ReservationSummaryVO {

    private Long pendingCount;

    private Long readyCount;

    private Long fulfilledCount;

    private Long cancelledCount;

    private Long expiredCount;

    private Long activeCount;

    private Long totalCount;
}
