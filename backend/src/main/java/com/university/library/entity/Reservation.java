package com.university.library.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图书预约实体。
 *
 * <p>READY 状态表示已从归还副本中留出一册书，available_copies 不包含该册；
 * 读者到馆（或线上）借阅后变为 FULFILLED。</p>
 */
@Data
@TableName("reservation")
public class Reservation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long readerId;

    private Long bookId;

    private String status;

    private LocalDateTime requestedAt;

    private LocalDateTime readyAt;

    private LocalDateTime expireAt;

    private LocalDateTime closedAt;

    private Long borrowRecordId;

    private String cancelReason;

    private String readerName;

    private String readerNo;

    private String bookTitle;

    private String bookIsbn;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
