package com.university.library.mapper.model;

import lombok.Data;

/**
 * 图书借阅聚合结果。
 */
@Data
public class BookBorrowCount {

    private Long bookId;

    private Long activeCount;

    private Long totalCount;
}
