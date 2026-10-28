package com.university.library.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 借阅记录视图：记录基础信息 + 读者/图书/分类冗余字段与逾期计算。
 */
@Data
public class BorrowRecordVO {

    private Long id;

    private Long readerId;

    private Long bookId;

    private LocalDate borrowDate;

    private LocalDate dueDate;

    private LocalDate returnDate;

    private String status;

    private Integer renewCount;

    private Long operatorId;

    private Long returnedBy;

    private LocalDateTime createdAt;

    private String readerName;

    private String readerNo;

    private String readerType;

    private String bookTitle;

    private String bookIsbn;

    private String bookAuthor;

    private String categoryName;

    private String categoryColor;

    private Long overdueDays;

    private Boolean canRenew;
}
