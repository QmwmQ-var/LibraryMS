package com.university.library.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 借阅记录实体。
 *
 * <p>readerName / readerNo / bookTitle / bookIsbn 为借阅时的快照，
 * 即使读者或图书之后被逻辑删除，历史记录仍可展示。</p>
 */
@Data
@TableName("borrow_record")
public class BorrowRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long readerId;

    private Long bookId;

    private LocalDate borrowDate;

    private LocalDate dueDate;

    private LocalDate returnDate;

    /**
     * BORROWED / RETURNED / OVERDUE。
     */
    private String status;

    private Integer renewCount;

    private Long operatorId;

    private Long returnedBy;

    private String readerName;

    private String readerNo;

    private String bookTitle;

    private String bookIsbn;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
