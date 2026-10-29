package com.university.library.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理员概览统计。
 */
@Data
public class AdminOverviewVO {

    private String role = "ADMIN";

    private Long totalTitles;

    private Long totalCopies;

    private Long availableCopies;

    private Long readerCount;

    private Long activeLoans;

    private Long overdueLoans;

    private Long todayBorrows;

    private Long monthBorrows;

    private Double monthBorrowDelta;

    private Long activeMonthReaders;

    private Long pendingFineCount;

    private BigDecimal pendingFineAmount;

    private Long lowStockCount;

    private Long rangeBorrowCount;

    private Long pendingReservations;

    private Long readyReservations;
}
