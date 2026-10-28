package com.university.library.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 登录失败锁定配置（数据库字段实现，无需 Redis）。
 */
@Data
@ConfigurationProperties(prefix = "app.login")
public class LoginProperties {

    /**
     * 连续失败次数上限，达到后锁定。
     */
    private int maxFailAttempts = 3;

    /**
     * 锁定时长（分钟）。
     */
    private int lockMinutes = 15;
}
