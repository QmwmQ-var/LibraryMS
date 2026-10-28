package com.university.library.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通知列表。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationVO {

    private List<NotificationItemVO> items;

    private Integer unreadCount;
}
