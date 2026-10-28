package com.university.library.vo;

import lombok.Data;

import java.util.List;

/**
 * 逾期统计响应。
 */
@Data
public class OverdueStatsVO {

    private OverdueSummaryVO summary;

    private MonthlyOverdueTrendVO trend;

    private List<OverdueBucketVO> buckets;

    private List<BorrowRecordVO> records;

    private List<PenaltyVO> recentPenalties;
}
