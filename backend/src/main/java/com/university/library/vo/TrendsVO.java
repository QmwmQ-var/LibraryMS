package com.university.library.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统计趋势：月度借还 + 分类构成。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendsVO {

    private MonthlyTrendVO monthly;

    private List<CategoryStatVO> categories;
}
