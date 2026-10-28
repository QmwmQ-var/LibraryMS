package com.university.library.common.util;

import java.util.List;

/**
 * 分页参数归一化与内存分页工具。
 *
 * <p>本项目列表接口需要先按关键字/关联对象过滤再分页（与前端 mock 行为一致），
 * 因此统一在 Service 层完成归一化与切片，避免各处重复编写边界判断。</p>
 */
public final class PageUtils {

    public static final long DEFAULT_PAGE = 1;
    public static final long DEFAULT_SIZE = 10;
    public static final long MAX_SIZE = 100;

    private PageUtils() {
    }

    public static long normalizePage(long page) {
        return Math.max(page, 1);
    }

    public static long normalizeSize(long size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    /**
     * 按页码（从 1 开始）截取列表。
     */
    public static <T> List<T> slice(List<T> items, long page, long size) {
        long from = Math.min((page - 1) * size, items.size());
        long to = Math.min(from + size, items.size());
        return items.subList((int) from, (int) to);
    }
}
