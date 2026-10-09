package com.game.console.controller;

import com.game.common.response.ApiResult;
import com.game.common.response.ListVO;
import com.game.common.response.PageResult;
import com.game.common.trading.dto.GameQueryDTO;
import com.game.common.trading.service.GameQueryService;
import com.game.common.trading.vo.DetailsVO;
import com.game.common.trading.vo.ProductVO;
import com.game.common.util.RequestUrls;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

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
     * 查询商品列表（支持关键词、价格区间、排序）
     * GET /gametrading/getinfoproduct?keyword=王者&amp;min_price=100&amp;sort=price_asc
     */
    @GetMapping("/getinfoproduct")
    public ApiResult<ListVO<ProductVO>> getInfoProduct(
            @RequestParam(required = false)
            @Size(max = 50, message = "关键词长度不能超过 50") String keyword,
            @RequestParam(name = "min_price", required = false)
            @DecimalMin(value = "0", message = "最低价不能小于 0") BigDecimal minPrice,
            @RequestParam(name = "max_price", required = false)
            @DecimalMin(value = "0", message = "最高价不能小于 0") BigDecimal maxPrice,
            @RequestParam(required = false) String sort
    ) {
        return ApiResult.success(
                gameQueryService.listProducts(GameQueryDTO.of(keyword, minPrice, maxPrice, sort))
        );
    }

    /**
     * 分页查询商品详情（支持关键词、价格区间、排序）
     * GET /gametrading/getinfodetails?page=1&amp;page_size=10&amp;keyword=王者
     */
    @GetMapping("/getinfodetails")
    public ApiResult<PageResult<DetailsVO>> getInfoDetails(
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码必须大于 0") Integer page,
            @RequestParam(name = "page_size", defaultValue = "10")
            @Min(value = 1, message = "每页条数必须大于 0")
            @Max(value = 100, message = "每页条数不能超过 100") Integer pageSize,
            @RequestParam(required = false)
            @Size(max = 50, message = "关键词长度不能超过 50") String keyword,
            @RequestParam(name = "min_price", required = false)
            @DecimalMin(value = "0", message = "最低价不能小于 0") BigDecimal minPrice,
            @RequestParam(name = "max_price", required = false)
            @DecimalMin(value = "0", message = "最高价不能小于 0") BigDecimal maxPrice,
            @RequestParam(required = false) String sort
    ) {
        return ApiResult.success(
                gameQueryService.pageDetails(
                        GameQueryDTO.of(keyword, minPrice, maxPrice, sort),
                        page,
                        pageSize,
                        RequestUrls.currentWithout("page", "page_size"))
        );
    }
}
