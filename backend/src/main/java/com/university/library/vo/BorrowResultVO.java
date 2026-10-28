package com.university.library.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 借书成功返回：新借阅记录 + 读者最新额度。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BorrowResultVO {

    private BorrowRecordVO record;

    private ReaderVO reader;
}
