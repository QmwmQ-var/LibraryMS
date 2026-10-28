package com.university.library.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 罚款视图：罚款信息 + 读者/图书/借阅记录冗余字段。
 */
@Data
public class PenaltyVO {

    private Long id;

    private Long recordId;

    private Long readerId;

    private Long bookId;

    private Integer overdueDays;

    private BigDecimal amount;

    private String status;

    private LocalDateTime paidAt;

    private Long paidBy;

    private String paymentChannel;

    private BigDecimal ruleRate;

    private LocalDateTime createdAt;

    private String readerName;

    private String readerNo;

    private String bookTitle;

    private String bookIsbn;

    private LocalDate borrowDate;

    private LocalDate dueDate;

    private LocalDate returnDate;

    private String recordStatus;
}
