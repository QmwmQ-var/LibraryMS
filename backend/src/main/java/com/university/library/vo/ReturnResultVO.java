package com.university.library.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 还书成功返回：记录 + 罚款（无逾期为 null）+ 图书最新库存。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnResultVO {

    private BorrowRecordVO record;

    private PenaltyVO penalty;

    private BookVO book;
}
