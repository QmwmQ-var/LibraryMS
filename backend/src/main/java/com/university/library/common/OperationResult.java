package com.university.library.common;

/**
 * Service 层操作结果：数据 + 成功提示语。
 *
 * <p>Controller 只负责把 data 与 message 放入统一响应体，避免在 Controller 中
 * 拼装业务提示信息。</p>
 */
public record OperationResult<T>(T data, String message) {

    public static <T> OperationResult<T> of(T data, String message) {
        return new OperationResult<>(data, message);
    }

    public static OperationResult<Void> ok(String message) {
        return new OperationResult<>(null, message);
    }
}
