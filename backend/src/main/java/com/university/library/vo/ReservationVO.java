package com.university.library.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预约视图：排队信息 + 读者/图书冗余字段。
 */
@Data
public class ReservationVO {

    private Long id;

    private Long readerId;

    private String readerName;

    private String readerNo;

    private String readerType;

    private Long bookId;

    private String bookTitle;

    private String bookIsbn;

    private String bookAuthor;

    private String categoryName;

    private String categoryColor;

    private String status;

    private String statusLabel;

    /** 在本书所有 PENDING / READY 预约中的位置，1 为队首。 */
    private Integer queuePosition;

    /** 排在自己前面的人数。 */
    private Integer aheadCount;

    private Integer availableCopies;

    private Integer reservationCount;

    private LocalDateTime requestedAt;

    private LocalDateTime readyAt;

    private LocalDateTime expireAt;

    private LocalDateTime closedAt;

    private Long borrowRecordId;

    private String cancelReason;

    /** READY 状态剩余保留小时数，四舍五入到小时。 */
    private Long remainingHours;
}
