package com.game.common.trading.enums;

import com.game.common.exception.BusinessException;

/**
 * 商品排序白名单。
 * 前端只能传枚举里定义的 code，具体排序列由枚举决定，
 * 不存在把前端字符串拼进 SQL 的可能。
 */
public enum GameSortType {

    /** 默认：按创建时间倒序 */
    TIME_DESC("time_desc"),

    /** 价格升序 */
    PRICE_ASC("price_asc"),

    /** 价格降序 */
    PRICE_DESC("price_desc");

    private final String code;

    GameSortType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * 按 code 解析排序方式：不传取默认值，传了白名单以外的值直接报错。
     */
    public static GameSortType of(String code) {
        if (code == null || code.isBlank()) {
            return TIME_DESC;
        }
        for (GameSortType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new BusinessException("不支持的排序方式：" + code + "，可选值：time_desc、price_asc、price_desc");
    }
}
