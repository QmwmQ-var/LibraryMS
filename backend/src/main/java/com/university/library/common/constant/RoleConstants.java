package com.university.library.common.constant;

/**
 * 角色常量。Spring Security 权限名统一加 ROLE_ 前缀。
 */
public final class RoleConstants {

    public static final String ADMIN = "ADMIN";
    public static final String READER = "READER";

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_READER = "ROLE_READER";

    private RoleConstants() {
    }

    public static String toAuthority(String role) {
        return "ROLE_" + role;
    }
}
