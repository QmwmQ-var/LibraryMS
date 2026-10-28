package com.university.library.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 逾期罚款月度趋势。
 */
@Data
public class MonthlyOverdueTrendVO {

    private List<String> labels = new ArrayList<>();

    private List<Long> counts = new ArrayList<>();

    private List<BigDecimal> amounts = new ArrayList<>();
}
