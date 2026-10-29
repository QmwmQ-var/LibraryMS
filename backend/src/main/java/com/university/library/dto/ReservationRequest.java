package com.university.library.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 预约入参：读者对指定图书发起预约。
 */
@Data
public class ReservationRequest {

    @NotNull(message = "请选择要预约的图书")
    private Long bookId;
}
