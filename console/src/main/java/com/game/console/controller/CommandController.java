package com.game.console.controller;

import com.game.common.response.ApiResult;
import com.game.common.trading.dto.GameSaveDTO;
import com.game.common.trading.dto.ItemIdDTO;
import com.game.common.trading.service.GameCommandService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端交易写操作接口（API 适配层）
 */
@RequestMapping("/gametrading")
@RestController
public class CommandController {

    private final GameCommandService gameCommandService;

    public CommandController(GameCommandService gameCommandService) {
        this.gameCommandService = gameCommandService;
    }

    /**
     * 新增商品
     * POST /gametrading/addgame
     *
     * @return 新商品主键 ID
     */
    @PostMapping("/addgame")
    public ApiResult<Long> addGame(@RequestBody GameSaveDTO dto) {
        return ApiResult.success(gameCommandService.add(dto));
    }

    /**
     * 修改商品
     * POST /gametrading/updategame
     *
     * @return 影响行数
     */
    @PostMapping("/updategame")
    public ApiResult<Integer> updateGame(@RequestBody GameSaveDTO dto) {
        return ApiResult.success(gameCommandService.update(dto));
    }

    /**
     * 逻辑删除商品
     * POST /gametrading/softdeletegame
     *
     * @return 影响行数
     */
    @PostMapping("/softdeletegame")
    public ApiResult<Integer> softDeleteGame(@RequestBody ItemIdDTO dto) {
        return ApiResult.success(gameCommandService.softDelete(dto.getItemId()));
    }

    /**
     * 物理删除商品
     * POST /gametrading/harddeletegame
     *
     * @return 影响行数
     */
    @PostMapping("/harddeletegame")
    public ApiResult<Integer> hardDeleteGame(@RequestBody ItemIdDTO dto) {
        return ApiResult.success(gameCommandService.hardDelete(dto.getItemId()));
    }
}
