package com.game.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;
import java.util.Set;

/**
 * 请求 URL 工具类
 */
public final class RequestUrls {

    private RequestUrls() {
    }

    /**
     * 获取当前请求的完整 URL，并去掉指定查询参数，用于拼接分页链接。
     * 例如商品查询带上了 keyword / min_price，翻页链接要保留筛选条件、只替换 page 与 page_size。
     *
     * @param excludeNames 需要去掉的参数名，可以有多个
     */
    public static String currentWithout(String... excludeNames) {
        Set<String> excluded = Set.of(excludeNames);
        UriComponentsBuilder builder =
                ServletUriComponentsBuilder.fromCurrentRequest().replaceQuery(null);
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
                if (!excluded.contains(entry.getKey())) {
                    builder.queryParam(entry.getKey(), entry.getValue());
                }
            }
        }
        return builder.build().encode().toUriString();
    }
}
