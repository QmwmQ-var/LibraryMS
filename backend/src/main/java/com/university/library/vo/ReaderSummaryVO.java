package com.university.library.vo;

import lombok.Data;

/**
 * 读者列表汇总。
 */
@Data
public class ReaderSummaryVO {

    private Long total;

    private Long active;

    private Long lost;

    private Long cancelled;

    private Long undergraduate;

    private Long graduate;

    private Long teacher;

    private Long activeLoans;

    private Long overdue;
}
