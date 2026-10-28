package com.university.library.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 罚款实体：与借阅记录一对一。
 */
@Data
@TableName("penalty")
public class Penalty {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recordId;

    private Long readerId;

    private Long bookId;

    private Integer overdueDays;

    private BigDecimal amount;

    /**
     * UNPAID / PAID。
     */
    private String status;

    private LocalDateTime paidAt;

    private Long paidBy;

    /**
     * CASH（管理员确认）/ ONLINE（读者自助）。
     */
    private String paymentChannel;

    /**
     * 生成罚款时的每日费率快照。
     */
    private BigDecimal ruleRate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
