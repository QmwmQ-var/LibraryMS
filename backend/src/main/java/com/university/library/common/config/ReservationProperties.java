package com.university.library.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 预约配置。
 */
@Data
@ConfigurationProperties(prefix = "app.reservation")
public class ReservationProperties {

    /**
     * 到书后为读者保留的小时数。
     */
    private int readyHoldHours = 48;

    /**
     * 每位读者最多可同时持有的 PENDING / READY 预约数。
     */
    private int maxActivePerReader = 5;
}
