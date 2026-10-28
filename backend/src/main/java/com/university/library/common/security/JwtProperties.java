package com.university.library.common.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置。
 */
@Data
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * HS256 密钥，生产环境务必通过环境变量覆盖，长度至少 32 字节。
     */
    private String secret;

    /**
     * 令牌有效期（分钟）。
     */
    private long expireMinutes = 720L;

    private String issuer = "libraryms";
}
