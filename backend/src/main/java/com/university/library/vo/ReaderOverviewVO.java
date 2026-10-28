package com.university.library.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 读者概览统计。
 */
@Data
public class ReaderOverviewVO {

    private String role = "READER";

    private Long activeLoans;

    private Long overdueLoans;

    private Long dueSoon;

    private Long availableQuota;

    private Integer maxBorrow;

    private Long unpaidCount;

    private BigDecimal unpaidAmount;

    private Long totalRead;

    private String favoriteCategory;

    private Long favoriteCategoryCount;

    private String cardStatus;
}
