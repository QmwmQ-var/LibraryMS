package com.university.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 借阅证状态修改入参。
 */
@Data
public class CardStatusRequest {

    @NotBlank(message = "借阅证状态不合法")
    @Pattern(regexp = "ACTIVE|LOST|CANCELLED", message = "借阅证状态不合法")
    private String cardStatus;
}
