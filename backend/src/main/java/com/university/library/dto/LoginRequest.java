package com.university.library.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录入参。
 */
@Data
public class LoginRequest {

    @NotBlank(message = "账号和密码不能为空")
    private String username;

    @NotBlank(message = "账号和密码不能为空")
    private String password;
}
