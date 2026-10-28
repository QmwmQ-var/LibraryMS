package com.university.library.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分类借阅构成。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStatVO {

    private Long id;

    private String name;

    private Long value;

    private String color;
}
