package com.game.common.trading.service;

import com.game.common.response.CursorPageResult;
import com.game.common.response.ListVO;
import com.game.common.response.PageResult;
import com.game.common.trading.convert.GameConverter;
import com.game.common.trading.entity.GameEntity;
import com.game.common.trading.mapper.GameMapper;
import com.game.common.trading.vo.DetailsVO;
import com.game.common.trading.vo.ProductVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 交易商品查询服务（app / console 共用）
 */
@Service
public class GameQueryService {

    private final GameMapper gameMapper;

    public GameQueryService(GameMapper gameMapper) {
        this.gameMapper = gameMapper;
    }

    /** 查询全部未删除商品列表 */
    public ListVO<ProductVO> listProducts() {
        List<ProductVO> items = gameMapper.listActive().stream()
                .map(GameConverter::toProductVO)
                .toList();
        return ListVO.of(items);
    }

    /**
     * 分页查询商品详情
     *
     * @param page     当前页码（从 1 开始）
     * @param pageSize 每页条数
     * @param baseUrl  当前接口基础 URL（用于生成上下页链接）
     */
    public PageResult<DetailsVO> pageDetails(Integer page, Integer pageSize, String baseUrl) {
        int offset = (page - 1) * pageSize;
        List<DetailsVO> items = gameMapper.page(offset, pageSize).stream()
                .map(GameConverter::toDetailsVO)
                .toList();
        return PageResult.of(items, gameMapper.count(), page, pageSize, baseUrl);
    }

    /**
     * 瀑布流商品列表（游标分页，适合无限下滑）。
     *
     * @param cursor   上一屏返回的 next_cursor，首页不传
     * @param pageSize 每屏条数
     * @param baseUrl  当前接口基础 URL
     */
    public CursorPageResult<ProductVO> waterfallProducts(Long cursor, Integer pageSize, String baseUrl) {
        // 多查 1 条，用于判断是否还有下一屏
        List<GameEntity> entities = gameMapper.waterfall(cursor, pageSize + 1);
        boolean hasMore = entities.size() > pageSize;
        if (hasMore) {
            entities = new ArrayList<>(entities.subList(0, pageSize));
        }

        List<ProductVO> items = entities.stream()
                .map(GameConverter::toProductVO)
                .toList();

        Long nextCursor = null;
        if (hasMore && !entities.isEmpty()) {
            nextCursor = entities.get(entities.size() - 1).getItemId();
        }

        return CursorPageResult.of(items, pageSize, hasMore, nextCursor, baseUrl);
    }
}
