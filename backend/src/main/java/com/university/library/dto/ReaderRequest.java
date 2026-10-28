package com.university.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 读者新增/修改入参。
 */
@Data
public class ReaderRequest {

    @NotBlank(message = "学号不能为空")
    @Pattern(regexp = "\\d{6,12}", message = "学号应为 6–12 位数字")
    private String studentNo;

    @NotBlank(message = "姓名不能为空")
    private String name;

    private String gender;

    @NotBlank(message = "请选择有效的读者类型")
    @Pattern(regexp = "UNDERGRADUATE|GRADUATE|TEACHER", message = "请选择有效的读者类型")
    private String type;

    @NotBlank(message = "请选择有效的借阅证状态")
    @Pattern(regexp = "ACTIVE|LOST|CANCELLED", message = "请选择有效的借阅证状态")
    private String cardStatus;

    private String department;

    private String major;

    private String email;

    private String phone;
}
