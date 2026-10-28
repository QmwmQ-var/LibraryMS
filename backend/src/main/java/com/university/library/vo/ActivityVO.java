package com.university.library.vo;

import lombok.Data;

/**
 * 仪表盘动态视图。
 */
@Data
public class ActivityVO {

    private String id;

    private String type;

    private String title;

    private String description;

    private String time;

    private String tone;
}
