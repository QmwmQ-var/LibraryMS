package com.university.library.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 逾期天数区间分布（max 为 null 表示“30 天以上”）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OverdueBucketVO {

    private String label;

    private Integer min;

    private Integer max;

    private String color;

    private Long value;
}
