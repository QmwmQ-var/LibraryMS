package com.university.library.vo;

import lombok.Data;

/**
 * 分类视图：分类信息 + 图书数量、馆藏册数、借阅次数统计。
 */
@Data
public class CategoryVO {

    private Long id;

    private String name;

    private String code;

    private String color;

    private String description;

    private Long bookCount;

    private Long copyCount;

    private Long borrowCount;
}
