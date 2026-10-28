package com.university.library.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 读者详情视图：在读者视图基础上补充在借、最近历史与罚款。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReaderDetailVO extends ReaderVO {

    private List<BorrowRecordVO> activeBorrows;

    private List<BorrowRecordVO> recentHistory;

    private List<PenaltyVO> penalties;
}
