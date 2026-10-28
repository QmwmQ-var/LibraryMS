package com.university.library.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 月度借阅趋势（labels 为 "M月"）。
 */
@Data
public class MonthlyTrendVO {

    private List<String> labels = new ArrayList<>();

    private List<Long> borrows = new ArrayList<>();

    private List<Long> returns = new ArrayList<>();
}
