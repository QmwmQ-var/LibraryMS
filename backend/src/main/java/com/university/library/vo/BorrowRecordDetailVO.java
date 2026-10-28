package com.university.library.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 借阅记录详情：附带对应的罚款记录（如有）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BorrowRecordDetailVO extends BorrowRecordVO {

    private PenaltyVO penalty;
}
