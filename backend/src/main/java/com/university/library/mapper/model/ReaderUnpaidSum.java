package com.university.library.mapper.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 读者未缴罚款汇总。
 */
@Data
public class ReaderUnpaidSum {

    private Long readerId;

    private BigDecimal unpaidAmount;
}
