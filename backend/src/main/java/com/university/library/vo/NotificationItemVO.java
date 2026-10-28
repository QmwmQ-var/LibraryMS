package com.university.library.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知条目。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationItemVO {

    private String id;

    private String tone;

    private String title;

    private String description;

    private String path;
}
