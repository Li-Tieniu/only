package com.game.common.validation;

import com.game.common.order.dto.OrderCreateDTO;
import com.game.common.trading.dto.GameSaveDTO;
import com.game.common.trading.dto.ItemIdDTO;
import com.game.common.user.dto.UserDTO;
import com.game.common.user.dto.UserLoginDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 参数校验单元测试：直接跑 Jakarta Validation，不需要启动 Spring。
 */
class DtoValidationTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("注册：用户名、密码不能为空白")
    void registerShouldRejectBlankUsernameAndPassword() {
        UserDTO dto = new UserDTO();
        dto.setUsername("   ");
        dto.setPassword("");

        assertThat(messages(dto)).contains("用户名不能为空", "密码不能为空");
    }

    @Test
    @DisplayName("注册：合法参数不报错")
    void registerShouldAcceptValidInput() {
        UserDTO dto = new UserDTO();
        dto.setUsername("tieniu");
        dto.setPassword("123456");
        dto.setNickname("铁牛");

        assertThat(messages(dto)).isEmpty();
    }

    @Test
    @DisplayName("注册：用户名/昵称超长会报错")
    void registerShouldRejectTooLongFields() {
        UserDTO dto = new UserDTO();
        dto.setUsername("a".repeat(51));
        dto.setPassword("123456");
        dto.setNickname("b".repeat(51));

        assertThat(messages(dto)).contains("用户名长度不能超过 50", "昵称长度不能超过 50");
    }

    @Test
    @DisplayName("登录：用户名、密码不能为空白")
    void loginShouldRejectBlankFields() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("");
        dto.setPassword(" ");

        assertThat(messages(dto)).contains("用户名不能为空", "密码不能为空");
    }

    @Test
    @DisplayName("下单：商品 ID 不能为空且必须大于 0")
    void orderCreateShouldValidateItemId() {
        assertThat(messages(new OrderCreateDTO(null))).contains("商品 ID 不能为空");
        assertThat(messages(new OrderCreateDTO(0L))).contains("商品 ID 必须大于 0");
        assertThat(messages(new OrderCreateDTO(1L))).isEmpty();
    }

    @Test
    @DisplayName("商品删除：商品 ID 不能为空")
    void itemIdShouldBeRequired() {
        assertThat(messages(new ItemIdDTO())).contains("商品 ID 不能为空");
    }

    @Test
    @DisplayName("商品新增/修改：价格格式非法、文本超长会报错")
    void gameSaveShouldValidatePriceAndLength() {
        GameSaveDTO dto = new GameSaveDTO();
        dto.setPrice("-1");
        dto.setGameName("a".repeat(51));

        assertThat(messages(dto)).contains("商品价格必须是不小于 0 的数字，最多两位小数", "游戏名称长度不能超过 50");
    }

    @Test
    @DisplayName("商品修改：价格为空字符串（表示不修改）时不报校验错误")
    void gameSaveShouldAllowBlankPrice() {
        GameSaveDTO dto = new GameSaveDTO();
        dto.setItemId(1L);
        dto.setPrice("");

        assertThat(messages(dto)).isEmpty();
    }

    @Test
    @DisplayName("新增商品（AddGroup）：所有商品字段必填，避免落到数据库报 500")
    void addGroupShouldRequireAllProductFields() {
        assertThat(messages(new GameSaveDTO(), GameSaveDTO.AddGroup.class))
                .contains("封面图不能为空", "详情图不能为空", "游戏名称不能为空",
                        "商品价格不能为空", "商品详情不能为空");
    }

    @Test
    @DisplayName("修改商品（UpdateGroup）：只有 itemId 必填，允许只传部分字段")
    void updateGroupShouldRequireOnlyItemId() {
        // 只传了 gameName，没传 itemId → 只报 itemId 缺失
        GameSaveDTO partial = new GameSaveDTO();
        partial.setGameName("王者荣耀");
        assertThat(messages(partial, GameSaveDTO.UpdateGroup.class))
                .containsExactly("商品 ID 不能为空");

        // 带 itemId + 空串价格（表示不修改）→ 无校验错误
        GameSaveDTO update = new GameSaveDTO();
        update.setItemId(1L);
        update.setPrice("");
        assertThat(messages(update, GameSaveDTO.UpdateGroup.class)).isEmpty();

        // 价格格式非法时，通用约束在 UpdateGroup 下同样生效
        update.setPrice("abc");
        assertThat(messages(update, GameSaveDTO.UpdateGroup.class))
                .contains("商品价格必须是不小于 0 的数字，最多两位小数");
    }

    private static Set<String> messages(Object target) {
        return messages(target, new Class<?>[0]);
    }

    private static Set<String> messages(Object target, Class<?>... groups) {
        return VALIDATOR.validate(target, groups).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }
}
