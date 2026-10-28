package com.university.library.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 跨域配置：来源列表从 application.yml 读取，不硬编码 *。
 */
@Data
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private List<String> allowedOrigins = new ArrayList<>();

    /**
     * 是否允许携带 Cookie/Authorization（JWT 通过 Authorization 头传递，
     * 保持 true 便于前端开发环境调试）。
     */
    private boolean allowCredentials = true;

    private long maxAgeSeconds = 3600L;
}
