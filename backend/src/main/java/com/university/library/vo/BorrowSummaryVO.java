package com.university.library.vo;

import lombok.Data;

/**
 * 我的借阅汇总（与前端 mock 一致：activity/overdue/returned 基于过滤后的记录，
 * total 为读者全部借阅记录数）。
 */
@Data
public class BorrowSummaryVO {

    private Long active;

    private Long overdue;

    private Long returned;

    private Long total;
}
