package com.game.common.trading.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import lombok.Data;

/**
 * 商品新增 / 修改请求体。
 * 新增与修改的校验规则不同，通过两个校验分组区分：
 *
 * <ul>
 *   <li>新增（{@link AddGroup}）：商品字段全部必填，对应数据库的 NOT NULL 列，
 *       缺字段时由 Controller 直接返回参数错误（400），而不是等数据库抛异常变成 500；</li>
 *   <li>修改（{@link UpdateGroup}）：只有 itemId 必填，其余字段可以只传需要修改的部分
 *       （不传 = 不修改）；</li>
 * </ul>
 *
 * 两个分组都继承 {@link Default}，所以 {@code @Size}、{@code @Pattern} 这类通用约束
 * 在两种场景下都会生效。
 */
@Data
public class GameSaveDTO {

    /** 新增场景的校验分组 */
    public interface AddGroup extends Default {
    }

    /** 修改场景的校验分组 */
    public interface UpdateGroup extends Default {
    }

    /** 商品主键 ID（修改时必填；新增由数据库自增，不用传） */
    @NotNull(message = "商品 ID 不能为空", groups = UpdateGroup.class)
    private Long itemId;

    /** 列表封面图（新增强制；修改传空串表示不修改） */
    @NotBlank(message = "封面图不能为空", groups = AddGroup.class)
    @Size(max = 2000, message = "封面图地址长度不能超过 2000")
    private String coverImage;

    /** 详情大图，多图使用 $ 分隔（新增强制；修改传空串表示不修改） */
    @NotBlank(message = "详情图不能为空", groups = AddGroup.class)
    @Size(max = 2000, message = "详情图地址长度不能超过 2000")
    private String wallImages;

    /** 游戏名称（新增强制；修改传空串表示不修改） */
    @NotBlank(message = "游戏名称不能为空", groups = AddGroup.class)
    @Size(max = 50, message = "游戏名称长度不能超过 50")
    private String gameName;

    /** 商品价格（新增强制；修改传空串表示不修改） */
    // 修改场景：空串表示"本次不修改价格"，所以正则里放行空串，只拦截非法格式；
    // 新增场景：由 AddGroup 上的 @NotBlank 保证价格必须传
    @NotBlank(message = "商品价格不能为空", groups = AddGroup.class)
    @Pattern(regexp = "^$|^\\d+(\\.\\d{1,2})?$", message = "商品价格必须是不小于 0 的数字，最多两位小数")
    private String price;

    /** 商品详情描述（新增强制；修改传空串表示不修改） */
    @NotBlank(message = "商品详情不能为空", groups = AddGroup.class)
    @Size(max = 2000, message = "商品详情长度不能超过 2000")
    private String details;
}
