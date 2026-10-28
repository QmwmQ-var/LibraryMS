package com.university.library.vo;

import lombok.Data;

/**
 * 公开用户信息（不含密码）：登录与 /auth/me 返回。
 */
@Data
public class UserVO {

    private Long id;

    private String username;

    private String name;

    private String role;

    private String title;

    private String email;

    private String phone;

    private String avatarColor;

    private Long readerId;

    /**
     * 读者账号携带读者完整视图；管理员为 null。
     */
    private ReaderVO reader;
}
