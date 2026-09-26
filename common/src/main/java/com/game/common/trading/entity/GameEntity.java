package com.game.common.trading.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 游戏交易商品表实体，对应表 game_trading。
 * 表名、主键、逻辑删除、字段映射统一由 MyBatis-Plus 注解声明，不再依赖 XML。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("game_trading")
public class GameEntity {

    /** 主键 ID，数据库自增，insert 后回填 */
    @TableId(value = "item_id", type = IdType.AUTO)
    private Long itemId;

    /**
     * 是否删除：0-未删除，1-已删除。
     * 逻辑删除字段：查询、更新、删除会自动追加 is_deleted = 0 条件。
     */
    @TableLogic(value = "0", delval = "1")
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 列表展示小图 */
    @TableField("cover_image")
    private String coverImage;

    /** 详情展示大图，多图使用 $ 分隔 */
    @TableField("wall_images")
    private String wallImages;

    /** 游戏名称 */
    @TableField("game_name")
    private String gameName;

    /** 商品价格 */
    @TableField("price")
    private String price;

    /** 商品详情 */
    @TableField("details")
    private String details;

    /** 创建时间（Unix 时间戳，秒） */
    @TableField("create_time")
    private Integer createTime;

    /** 修改时间（Unix 时间戳，秒） */
    @TableField("update_time")
    private Integer updateTime;
}
