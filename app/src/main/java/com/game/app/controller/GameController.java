package com.game.app.controller;

import com.game.common.response.ApiResult;
import com.game.common.response.CursorPageResult;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RequestMapping("/gametrading")
@RestController
public class GameController {

    private final GameQueryService gameQueryService;

    public GameController(GameQueryService gameQueryService) {
        this.gameQueryService = gameQueryService;
    }

    /**
     * 查询商品列表（支持关键词、价格区间、排序）
     * GET /gametrading/product?keyword=王者&amp;min_price=100&amp;max_price=900&amp;sort=price_asc
     */
    @GetMapping("/product")
    public ApiResult<ListVO<ProductVO>> product(
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
     * 瀑布流商品列表（游标分页，无限下滑，支持关键词与价格区间）
     * 首页：GET /gametrading/product/waterfall?page_size=10&amp;keyword=王者
     * 下一屏：GET /gametrading/product/waterfall?cursor={next_cursor}&amp;page_size=10
     * 游标按 item_id 倒序，所以不支持 sort
     */
    @GetMapping("/product/waterfall")
    public ApiResult<CursorPageResult<ProductVO>> productWaterfall(
            @RequestParam(required = false) Long cursor,
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
                gameQueryService.waterfallProducts(
                        GameQueryDTO.of(keyword, minPrice, maxPrice, sort),
                        cursor,
                        pageSize,
                        RequestUrls.currentWithout("cursor", "page_size"))
        );
    }

    /** 按商品 ID 查询单个商品详情 */
    @GetMapping("/details/{itemId}")
    public ApiResult<DetailsVO> detail(@PathVariable("itemId") Long itemId) {
        return ApiResult.success(gameQueryService.getDetailById(itemId));
    }

    /**
     * 分页查询商品详情（支持关键词、价格区间、排序）
     * GET /gametrading/details?page=1&amp;page_size=10&amp;keyword=王者&amp;sort=price_desc
     */
    @GetMapping("/details")
    public ApiResult<PageResult<DetailsVO>> details(
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
