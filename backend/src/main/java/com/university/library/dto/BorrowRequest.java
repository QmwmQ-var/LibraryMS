package com.university.library.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 借书入参：读者自助借书时 readerId 可省略（强制取当前登录读者）。
 */
@Data
public class BorrowRequest {

    private Long readerId;

    @NotNull(message = "请选择要借阅的图书")
    private Long bookId;
}
