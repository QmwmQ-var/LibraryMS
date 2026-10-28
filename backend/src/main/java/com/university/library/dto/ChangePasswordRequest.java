package com.university.library.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改密码入参。
 */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
