package com.university.library.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 读者实体：与 user 一对一，保存学号、读者类型、借阅证状态。
 */
@Data
@TableName("reader")
public class Reader {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String studentNo;

    private String name;

    private String gender;

    /**
     * UNDERGRADUATE / GRADUATE / TEACHER。
     */
    private String type;

    /**
     * ACTIVE / LOST / CANCELLED。
     */
    private String cardStatus;

    private String department;

    private String major;

    private String email;

    private String phone;

    private String cardNo;

    private LocalDate cardIssuedAt;

    private LocalDate cardExpiresAt;

    /**
     * 逻辑删除：删除读者后保留借阅历史。
     */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
