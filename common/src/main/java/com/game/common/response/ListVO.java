package com.game.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 非分页列表响应数据体。
 * 格式：{ items: [...] }
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListVO<T> {

    /** 列表数据 */
    private List<T> items = new ArrayList<>();

    /** 根据已有列表构建 ListVO */
    public static <T> ListVO<T> of(List<T> items) {
        ListVO<T> result = new ListVO<>();
        result.setItems(items);
        return result;
    }
}
