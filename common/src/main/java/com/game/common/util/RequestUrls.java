package com.game.common.util;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * 请求 URL 工具类
 */
public final class RequestUrls {

    private RequestUrls() {
    }

    /**
     * 获取当前请求不含查询参数的完整 URL，用于拼接分页链接。
     */
    public static String currentWithoutQuery() {
        return ServletUriComponentsBuilder.fromCurrentRequest()
                .replaceQuery(null)
                .build()
                .toUriString();
    }
}
