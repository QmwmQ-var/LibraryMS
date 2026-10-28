package com.university.library.common.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.university.library.common.constant.RoleConstants;
import com.university.library.common.constant.UserStatus;
import com.university.library.entity.Reader;
import com.university.library.entity.User;
import com.university.library.mapper.ReaderMapper;
import com.university.library.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：解析 Authorization: Bearer &lt;token&gt;，构建无状态登录上下文。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;
    private final ReaderMapper readerMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            String token = authorization.substring(BEARER_PREFIX.length()).trim();
            try {
                Claims claims = jwtUtils.parse(token);
                Long userId = Long.valueOf(claims.getSubject());
                User user = userMapper.selectById(userId);
                if (user != null && UserStatus.ACTIVE.equals(user.getStatus())) {
                    Reader reader = readerMapper.selectOne(
                            Wrappers.<Reader>lambdaQuery().eq(Reader::getUserId, user.getId()));
                    UserPrincipal principal = new UserPrincipal(
                            user.getId(), user.getUsername(), user.getRole(),
                            reader == null ? null : reader.getId());
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal, null,
                                    List.of(new SimpleGrantedAuthority(RoleConstants.toAuthority(user.getRole()))));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ex) {
                // Token 过期、签名错误或用户已停用：按未登录处理，由 EntryPoint 返回 401。
                SecurityContextHolder.clearContext();
                log.debug("JWT 校验失败: {}", ex.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}
