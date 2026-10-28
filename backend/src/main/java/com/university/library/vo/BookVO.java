package com.university.library.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 图书视图：图书基础信息 + 分类冗余字段与在借统计。
 */
@Data
public class BookVO {

    private Long id;

    private String isbn;

    private String title;

    private String author;

    private String publisher;

    private Long categoryId;

    private Integer totalCopies;

    private Integer availableCopies;

    private Integer publishYear;

    private BigDecimal price;

    private String location;

    private String description;

    private BigDecimal rating;

    private LocalDateTime createdAt;

    private String categoryName;

    private String categoryCode;

    private String categoryColor;

    private Long activeLoans;

    private Boolean available;
}
