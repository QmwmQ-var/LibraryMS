package com.university.library.common.security;

import com.university.library.common.constant.RoleConstants;

/**
 * 登录用户主体，放入 SecurityContext，Controller 通过 @AuthenticationPrincipal 获取。
 */
public record UserPrincipal(Long userId, String username, String role, Long readerId) {

    public boolean isAdmin() {
        return RoleConstants.ADMIN.equals(role);
    }

    public boolean isReader() {
        return RoleConstants.READER.equals(role);
    }
}
