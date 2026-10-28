package com.university.library.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 借阅排行中的图书。
 */
@Data
public class BookRankVO {

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

    private String categoryColor;

    private Long borrowCount;
}
