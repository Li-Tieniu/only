package com.game.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应封装。
 * 格式：{ code, message, data }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    private Integer code;
    private String message;
    private T data;

    /** 成功响应（带数据） */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "success", data);
    }

    /** 成功响应（无数据） */
    public static <T> ApiResult<T> success() {
        return success(null);
    }

    /**
     * 失败响应
     * @param code    错误码
     * @param message 错误信息
     */
    public static <T> ApiResult<T> fail(Integer code, String message) {
        return new ApiResult<>(code, message, null);
    }
}
