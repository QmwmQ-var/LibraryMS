package com.university.library.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应：{records, total, page, size}，部分接口额外携带 summary。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private List<T> records;
    private long total;
    private long page;
    private long size;

    /**
     * 部分接口的汇总信息；无汇总时不输出该字段，保持与 mock 一致。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object summary;

    public static <T> PageResult<T> of(List<T> records, long total, long page, long size) {
        return new PageResult<>(records, total, page, size, null);
    }

    public static <T> PageResult<T> withSummary(List<T> records, long total, long page, long size, Object summary) {
        return new PageResult<>(records, total, page, size, summary);
    }
}
