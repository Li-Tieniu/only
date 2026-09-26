package com.game.console.controller;

import com.game.common.response.ApiResult;
import com.game.common.response.ListVO;
import com.game.common.response.PageResult;
import com.game.common.trading.service.GameQueryService;
import com.game.common.trading.vo.DetailsVO;
import com.game.common.trading.vo.ProductVO;
import com.game.common.util.RequestUrls;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端交易查询接口（API 适配层）
 */
@RequestMapping("/gametrading")
@RestController
public class QueryController {

    private final GameQueryService gameQueryService;

    public QueryController(GameQueryService gameQueryService) {
        this.gameQueryService = gameQueryService;
    }

    /**
     * 查询商品列表
     * GET /gametrading/getinfoproduct
     */
    @GetMapping("/getinfoproduct")
    public ApiResult<ListVO<ProductVO>> getInfoProduct() {
        return ApiResult.success(gameQueryService.listProducts());
    }

    /**
     * 分页查询商品详情
     * GET /gametrading/getinfodetails?page=1&amp;page_size=10
     */
    @GetMapping("/getinfodetails")
    public ApiResult<PageResult<DetailsVO>> getInfoDetails(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize
    ) {
        return ApiResult.success(
                gameQueryService.pageDetails(page, pageSize, RequestUrls.currentWithoutQuery())
        );
    }
}
