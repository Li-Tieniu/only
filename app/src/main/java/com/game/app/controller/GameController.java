package com.game.app.controller;

import com.game.common.response.ApiResult;
import com.game.common.response.CursorPageResult;
import com.game.common.response.ListVO;
import com.game.common.response.PageResult;
import com.game.common.trading.service.GameQueryService;
import com.game.common.trading.vo.DetailsVO;
import com.game.common.trading.vo.ProductVO;
import com.game.common.util.RequestUrls;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/gametrading")
@RestController
public class GameController {

    private final GameQueryService gameQueryService;

    public GameController(GameQueryService gameQueryService) {
        this.gameQueryService = gameQueryService;
    }

    /**
     * 查询商品列表
     * GET /gametrading/product
     */
    @GetMapping("/product")
    public ApiResult<ListVO<ProductVO>> product() {
        return ApiResult.success(gameQueryService.listProducts());
    }

    /**
     * 瀑布流商品列表（游标分页，无限下滑）
     * 首页：GET /gametrading/product/waterfall?page_size=10
     * 下一屏：GET /gametrading/product/waterfall?cursor={next_cursor}&amp;page_size=10
     */
    @GetMapping("/product/waterfall")
    public ApiResult<CursorPageResult<ProductVO>> productWaterfall(
            @RequestParam(required = false) Long cursor,
            @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize
    ) {
        return ApiResult.success(
                gameQueryService.waterfallProducts(cursor, pageSize, RequestUrls.currentWithoutQuery())
        );
    }

    /**
     * 分页查询商品详情
     * GET /gametrading/details?page=1&amp;page_size=10
     */
    @GetMapping("/details")
    public ApiResult<PageResult<DetailsVO>> details(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize
    ) {
        return ApiResult.success(
                gameQueryService.pageDetails(page, pageSize, RequestUrls.currentWithoutQuery())
        );
    }
}
