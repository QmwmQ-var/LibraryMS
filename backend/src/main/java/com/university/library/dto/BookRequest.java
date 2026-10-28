package com.university.library.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 图书新增/修改入参。
 */
@Data
public class BookRequest {

    @NotBlank(message = "ISBN 不能为空")
    private String isbn;

    @NotBlank(message = "书名不能为空")
    private String title;

    @NotBlank(message = "作者不能为空")
    private String author;

    private String publisher;

    @NotNull(message = "请选择有效的图书分类")
    private Long categoryId;

    @NotNull(message = "馆藏总量至少为 1 本")
    @Min(value = 1, message = "馆藏总量至少为 1 本")
    private Integer totalCopies;

    private BigDecimal price;

    private Integer publishYear;

    private String location;

    private String description;
}
