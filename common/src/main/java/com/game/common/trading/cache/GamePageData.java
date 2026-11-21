package com.game.common.trading.cache;

import com.game.common.trading.entity.GameEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页缓存数据：只放查询结果（当前页记录 + 总数）。
 * next_page / prev_page 这类带 host 的链接留给响应层拼，
 * 这样 app 和 console 才能安全共用同一份缓存。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GamePageData {

    /** 当前页记录 */
    private List<GameEntity> records;

    /** 满足条件的总记录数 */
    private long total;
}
