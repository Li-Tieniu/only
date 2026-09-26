package com.game.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 瀑布流（游标）分页响应数据体。
 * 客户端用 next_cursor 继续请求下一屏，无需页码。
 */
@Data
public class CursorPageResult<T> {

    /** 每页条数 */
    @JsonProperty("page_size")
    private Integer pageSize;

    /** 是否还有更多数据 */
    @JsonProperty("has_more")
    private Boolean hasMore;

    /** 下一屏游标（本页最后一条 itemId），无更多时为 null */
    @JsonProperty("next_cursor")
    private Long nextCursor;

    /** 下一屏完整 URL，无更多时为 null */
    @JsonProperty("next_page")
    private String nextPage;

    /** 当前屏数据列表 */
    private List<T> items;

    /**
     * 构建瀑布流分页结果。
     *
     * @param items       当前屏数据（已按需截断）
     * @param pageSize    每页条数
     * @param hasMore     是否还有下一屏
     * @param nextCursor  下一屏游标
     * @param baseUrl     当前接口不含查询参数的基础 URL
     */
    public static <T> CursorPageResult<T> of(
            List<T> items,
            int pageSize,
            boolean hasMore,
            Long nextCursor,
            String baseUrl
    ) {
        CursorPageResult<T> result = new CursorPageResult<>();
        result.setItems(items);
        result.setPageSize(pageSize);
        result.setHasMore(hasMore);
        result.setNextCursor(nextCursor);
        result.setNextPage(hasMore && nextCursor != null
                ? baseUrl + "?cursor=" + nextCursor + "&page_size=" + pageSize
                : null);
        return result;
    }
}
