package com.university.library.mapper.model;

import lombok.Data;

/**
 * 读者借阅聚合结果。
 */
@Data
public class ReaderBorrowCount {

    private Long readerId;

    private Long activeCount;

    private Long overdueCount;

    private Long totalCount;
}
