package com.university.library.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告视图。
 */
@Data
public class AnnouncementVO {

    private Long id;

    private String title;

    private String content;

    private String tag;

    private LocalDateTime createdAt;
}
