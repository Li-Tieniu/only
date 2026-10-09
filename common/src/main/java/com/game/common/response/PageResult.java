package com.game.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 分页响应数据体。
 * 包含总数、当前页、页大小、上下页链接及当前页数据。
 */
@Data
public class PageResult<T> {

    /** 总记录数 */
    private Long total;

    /** 当前页码（从 1 开始） */
    private Integer page;

    /** 每页条数 */
    @JsonProperty("page_size")
    private Integer pageSize;

    /** 下一页完整 URL，无下一页时为 null */
    @JsonProperty("next_page")
    private String nextPage;

    /** 上一页完整 URL，无上一页时为 null */
    @JsonProperty("prev_page")
    private String prevPage;

    /** 当前页数据列表 */
    private List<T> items;

    /**
     * 构建分页结果，并自动生成 next_page / prev_page。
     *
     * @param items    当前页数据
     * @param total    总记录数
     * @param page     当前页码
     * @param pageSize 每页条数
     * @param baseUrl  当前接口不含查询参数的基础 URL
     */
    public static <T> PageResult<T> of(List<T> items, long total, int page, int pageSize, String baseUrl) {
        int pages = total == 0 ? 0 : (int) ((total + pageSize - 1) / pageSize);

        PageResult<T> result = new PageResult<>();
        result.setItems(items);
        result.setTotal(total);
        result.setPage(page);
        result.setPageSize(pageSize);
        result.setNextPage(page < pages ? buildPageUrl(baseUrl, page + 1, pageSize) : null);
        result.setPrevPage(page > 1 ? buildPageUrl(baseUrl, page - 1, pageSize) : null);
        return result;
    }

    /** 拼接分页查询 URL；baseUrl 上可能已经带有 keyword 等筛选条件，所以要用 & 连接 */
    private static String buildPageUrl(String baseUrl, int page, int pageSize) {
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "page=" + page + "&page_size=" + pageSize;
    }
}
