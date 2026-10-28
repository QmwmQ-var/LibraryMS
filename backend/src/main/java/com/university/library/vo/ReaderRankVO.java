package com.university.library.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 借阅排行中的读者。
 */
@Data
public class ReaderRankVO {

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

    private Long borrowCount;

    private Long activeCount;
}
