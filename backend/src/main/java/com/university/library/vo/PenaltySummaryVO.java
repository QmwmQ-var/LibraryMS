package com.university.library.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 罚款汇总。
 */
@Data
public class PenaltySummaryVO {

    private Long pendingCount;

    private BigDecimal pendingAmount;

    private BigDecimal paidThisMonth;

    private Long paidThisMonthCount;

    private BigDecimal collectedTotal;

    private BigDecimal rate;
}
