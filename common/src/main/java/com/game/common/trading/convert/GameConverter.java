package com.game.common.trading.convert;

import com.game.common.trading.dto.GameSaveDTO;
import com.game.common.trading.entity.GameEntity;
import com.game.common.trading.vo.DetailsVO;
import com.game.common.trading.vo.ProductVO;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 交易商品对象转换：DTO / Entity / VO
 */
public final class GameConverter {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private GameConverter() {
    }

    /** 实体转列表 VO */
    public static ProductVO toProductVO(GameEntity entity) {
        ProductVO vo = new ProductVO();
        vo.setItemId(entity.getItemId());
        vo.setCoverImage(entity.getCoverImage());
        vo.setGameName(entity.getGameName());
        vo.setPrice(entity.getPrice());
        return vo;
    }

    /** 实体转详情 VO（含图片拆分与时间格式化） */
    public static DetailsVO toDetailsVO(GameEntity entity) {
        DetailsVO vo = new DetailsVO();
        vo.setItemId(entity.getItemId());
        vo.setWallImages(splitImages(entity.getWallImages()));
        vo.setGameName(entity.getGameName());
        vo.setPrice(entity.getPrice());
        vo.setDetails(entity.getDetails());
        vo.setCreateTime(formatTime(entity.getCreateTime()));
        vo.setUpdateTime(formatTime(entity.getUpdateTime()));
        return vo;
    }

    /** 新增请求转实体 */
    public static GameEntity toNewEntity(GameSaveDTO dto) {
        int now = currentEpochSecond();
        GameEntity entity = new GameEntity();
        fillEditableFields(entity, dto);
        entity.setIsDeleted(0);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        return entity;
    }

    /** 修改请求转实体 */
    public static GameEntity toUpdateEntity(GameSaveDTO dto) {
        GameEntity entity = new GameEntity();
        entity.setItemId(dto.getItemId());
        fillEditableFields(entity, dto);
        entity.setUpdateTime(currentEpochSecond());
        return entity;
    }

    /** 按主键构建逻辑删除实体 */
    public static GameEntity toSoftDeleteEntity(Long itemId) {
        GameEntity entity = new GameEntity();
        entity.setItemId(itemId);
        entity.setUpdateTime(currentEpochSecond());
        return entity;
    }

    /** 按主键构建物理删除实体 */
    public static GameEntity toHardDeleteEntity(Long itemId) {
        GameEntity entity = new GameEntity();
        entity.setItemId(itemId);
        return entity;
    }

    private static void fillEditableFields(GameEntity entity, GameSaveDTO dto) {
        entity.setCoverImage(dto.getCoverImage());
        entity.setWallImages(dto.getWallImages());
        entity.setGameName(dto.getGameName());
        entity.setPrice(dto.getPrice());
        entity.setDetails(dto.getDetails());
    }

    private static List<String> splitImages(String wallImages) {
        if (wallImages == null || wallImages.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(wallImages.split("\\$"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static String formatTime(Integer timestamp) {
        if (timestamp == null) {
            return null;
        }
        return Instant.ofEpochSecond(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(TIME_FORMATTER);
    }

    private static int currentEpochSecond() {
        return (int) Instant.now().getEpochSecond();
    }
}
