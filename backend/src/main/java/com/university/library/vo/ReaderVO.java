package com.university.library.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 读者视图：读者基础信息 + 借阅规则与实时统计。
 */
@Data
public class ReaderVO {

    private Long id;

    private Long userId;

    private String studentNo;

    private String name;

    private String gender;

    private String type;

    private String cardStatus;

    private String department;

    private String major;

    private String email;

    private String phone;

    private String cardNo;

    private LocalDate cardIssuedAt;

    private LocalDate cardExpiresAt;

    private LocalDateTime createdAt;

    private String typeLabel;

    private Integer maxBorrow;

    private Integer borrowDays;

    private Long activeBorrowCount;

    private Long overdueCount;

    private Long availableQuota;

    private BigDecimal unpaidAmount;

    private Long totalBorrowCount;
}
