package com.game.common.exception;

/**
 * 业务异常：由业务代码主动抛出，交给 GlobalExceptionHandler 统一转成 ApiResult。
 */
public class BusinessException extends RuntimeException {

    /** 默认错误码：参数校验 / 业务校验失败 */
    public static final int DEFAULT_CODE = 400;

    private final int code;

    public BusinessException(String message) {
        this(DEFAULT_CODE, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
