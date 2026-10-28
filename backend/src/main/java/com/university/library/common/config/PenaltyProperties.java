package com.university.library.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * 罚款配置：逾期罚款 = 逾期天数 × 每日费率。
 */
@Data
@ConfigurationProperties(prefix = "app.penalty")
public class PenaltyProperties {

    /**
     * 每日罚款金额（元）。
     */
    private BigDecimal ratePerDay = new BigDecimal("0.50");
}
