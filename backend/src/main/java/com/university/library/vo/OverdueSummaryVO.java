package com.university.library.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 逾期统计摘要。
 */
@Data
public class OverdueSummaryVO {

    private Long count;

    private Long maxDays;

    private BigDecimal pendingAmount;

    private Long totalRecords;

    private Double overdueRate;
}
