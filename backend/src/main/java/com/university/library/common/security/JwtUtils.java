package com.university.library.common.security;

import com.university.library.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * JWT 签发与解析工具。
 *
 * <p>Token 中携带用户 ID（subject）、角色（role）与用户名（username）；
 * 服务端每次请求仍会回查数据库校验账号状态，避免“已停用账号持旧 Token 继续访问”。</p>
 */
@Component
public class JwtUtils {

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtUtils(JwtProperties properties) {
        this.properties = properties;
        byte[] bytes = properties.getSecret() == null
                ? new byte[0]
                : properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret 至少需要 32 字节（256 位）");
        }
        this.secretKey = Keys.hmacShaKeyFor(bytes);
    }

    /**
     * 签发令牌。
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expireAt = now.plus(properties.getExpireMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("role", user.getRole())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 校验签名、签发者与有效期后返回 claims。
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
